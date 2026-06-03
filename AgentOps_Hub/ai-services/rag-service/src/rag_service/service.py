from __future__ import annotations

import math
import re
from collections import Counter
from pathlib import Path
from typing import Any

from rag_service.models import (
    DOCUMENT_FAILED,
    DOCUMENT_INDEXED,
    DOCUMENT_PARSING,
    DOCUMENT_UPLOADED,
    CitationRecord,
    DocumentChunkRecord,
    DocumentRecord,
    ErrorInfo,
    IndexRecord,
    KnowledgeBaseRecord,
    SearchResult,
    new_id,
    to_dict,
    utc_now,
)


TOKEN_PATTERN = re.compile(r"[a-z0-9]+")


class RagServiceError(Exception):
    def __init__(self, error: ErrorInfo):
        super().__init__(error.message)
        self.error = error


def _tokenize(value: str) -> list[str]:
    return TOKEN_PATTERN.findall(value.lower())


def _event(event_name: str, tenant_id: str, trace_id: str, producer: str, consumers: list[str], payload: dict[str, Any]) -> dict[str, Any]:
    return {
        "event_id": new_id("event"),
        "event_name": event_name,
        "event_version": "v1",
        "occurred_at": utc_now(),
        "tenant_id": tenant_id,
        "trace_id": trace_id,
        "producer": producer,
        "consumers": consumers,
        "payload": payload,
    }


class DocumentRegistry:
    def __init__(self, source_root: Path | None = None) -> None:
        self.source_root = source_root or Path.cwd()
        self.knowledge_bases: dict[str, KnowledgeBaseRecord] = {}
        self.documents: dict[str, DocumentRecord] = {}
        self.chunks: dict[str, DocumentChunkRecord] = {}
        self.events: list[dict[str, Any]] = []

    def ensure_knowledge_base(self, tenant_id: str, knowledge_base_id: str, requested_by: str) -> KnowledgeBaseRecord:
        existing = self.knowledge_bases.get(knowledge_base_id)
        if existing is not None:
            return existing
        record = KnowledgeBaseRecord(
            knowledge_base_id=knowledge_base_id,
            tenant_id=tenant_id,
            name=knowledge_base_id.replace("_", " ").title(),
            created_by=requested_by,
        )
        self.knowledge_bases[knowledge_base_id] = record
        return record

    def register_document(
        self,
        *,
        tenant_id: str,
        requested_by: str,
        trace_id: str,
        knowledge_base_id: str,
        filename: str,
        object_key: str,
        source_text: str | None = None,
        document_id: str | None = None,
    ) -> DocumentRecord:
        if not tenant_id or not requested_by or not trace_id or not knowledge_base_id or not filename or not object_key:
            raise RagServiceError(
                ErrorInfo(
                    error_code="VALIDATION_FAILED",
                    message="Document registration requires tenant, requester, trace, knowledge base, filename, and object key.",
                    trace_id=trace_id or "missing_trace",
                    retryable=False,
                )
            )
        self.ensure_knowledge_base(tenant_id, knowledge_base_id, requested_by)
        doc_id = document_id or new_id("doc")
        if doc_id in self.documents:
            raise RagServiceError(
                ErrorInfo(
                    error_code="CONFLICT",
                    message=f"Document {doc_id} is already registered.",
                    trace_id=trace_id,
                    retryable=False,
                )
            )
        document = DocumentRecord(
            document_id=doc_id,
            knowledge_base_id=knowledge_base_id,
            tenant_id=tenant_id,
            filename=filename,
            object_key=object_key,
            created_by=requested_by,
            trace_id=trace_id,
            source_text=source_text,
        )
        self.documents[doc_id] = document
        self.events.append(
            _event(
                "document.uploaded.v1",
                tenant_id,
                trace_id,
                "document-entrypoint",
                ["rag-service"],
                {
                    "document_id": document.document_id,
                    "knowledge_base_id": document.knowledge_base_id,
                    "tenant_id": document.tenant_id,
                    "object_key": document.object_key,
                    "filename": document.filename,
                    "parse_status": DOCUMENT_UPLOADED,
                },
            )
        )
        return document

    def load_source_text(self, document: DocumentRecord) -> str:
        if document.source_text is not None:
            return document.source_text
        source_path = Path(document.object_key)
        if not source_path.is_absolute():
            source_path = self.source_root / document.object_key
        if not source_path.exists():
            return ""
        return source_path.read_text(encoding="utf-8")

    def mark_parsing(self, document: DocumentRecord) -> None:
        document.parse_status = DOCUMENT_PARSING
        document.error = None
        document.updated_at = utc_now()

    def mark_failed(self, document: DocumentRecord, error: ErrorInfo) -> DocumentRecord:
        document.parse_status = DOCUMENT_FAILED
        document.error = error
        document.updated_at = utc_now()
        return document

    def mark_indexed(self, document: DocumentRecord, index: IndexRecord) -> DocumentRecord:
        document.parse_status = DOCUMENT_INDEXED
        document.chunk_count = index.chunk_count
        document.index_ref = index.index_ref
        document.error = None
        document.updated_at = utc_now()
        self.events.append(
            _event(
                "document.indexed.v1",
                document.tenant_id,
                document.trace_id,
                "rag-service",
                ["document-entrypoint", "web-console-stream"],
                {
                    "document_id": document.document_id,
                    "knowledge_base_id": document.knowledge_base_id,
                    "tenant_id": document.tenant_id,
                    "chunk_count": document.chunk_count,
                    "index_ref": document.index_ref,
                    "parse_status": DOCUMENT_INDEXED,
                },
            )
        )
        return document

    def replace_chunks(self, document: DocumentRecord, chunks: list[DocumentChunkRecord]) -> None:
        for chunk_id in [chunk_id for chunk_id, chunk in self.chunks.items() if chunk.document_id == document.document_id]:
            del self.chunks[chunk_id]
        for chunk in chunks:
            self.chunks[chunk.chunk_id] = chunk


class DocumentParser:
    def parse_document(self, document: DocumentRecord, source_text: str) -> str:
        if not source_text.strip():
            raise RagServiceError(
                ErrorInfo(
                    error_code="DOCUMENT_PARSE_FAILED",
                    message=f"Document {document.document_id} has no readable text.",
                    trace_id=document.trace_id,
                    retryable=True,
                    details={"document_id": document.document_id, "parse_status": DOCUMENT_FAILED},
                )
            )
        lines = [line.rstrip() for line in source_text.replace("\r\n", "\n").split("\n")]
        return "\n".join(lines).strip()


class DocumentChunker:
    def __init__(self, target_terms: int = 70) -> None:
        self.target_terms = target_terms

    def chunk_document(self, document: DocumentRecord, parsed_text: str) -> list[DocumentChunkRecord]:
        paragraphs = [paragraph.strip().replace("\n", " ") for paragraph in re.split(r"\n\s*\n", parsed_text) if paragraph.strip()]
        chunks: list[DocumentChunkRecord] = []
        buffer: list[str] = []
        buffer_start = 0
        cursor = 0
        for paragraph in paragraphs:
            if not buffer:
                buffer_start = cursor
            buffer.append(paragraph)
            cursor += len(paragraph) + 2
            term_count = len(_tokenize(" ".join(buffer)))
            if term_count >= self.target_terms:
                chunks.append(self._make_chunk(document, len(chunks), " ".join(buffer), buffer_start, cursor))
                buffer = []
        if buffer:
            chunks.append(self._make_chunk(document, len(chunks), " ".join(buffer), buffer_start, cursor))
        return chunks

    def _make_chunk(self, document: DocumentRecord, sequence: int, text: str, start: int, end: int) -> DocumentChunkRecord:
        return DocumentChunkRecord(
            chunk_id=f"{document.document_id}_chunk_{sequence + 1:03d}",
            document_id=document.document_id,
            knowledge_base_id=document.knowledge_base_id,
            tenant_id=document.tenant_id,
            sequence=sequence + 1,
            text=text,
            source_start=start,
            source_end=end,
        )


class LocalTermIndex:
    def __init__(self) -> None:
        self._chunk_terms: dict[str, Counter[str]] = {}

    def build_index_ref(self, document: DocumentRecord) -> str:
        return (
            "local-index://tenant/"
            f"{document.tenant_id}/knowledge-base/{document.knowledge_base_id}/document/{document.document_id}"
        )

    def add_document(self, document: DocumentRecord, chunks: list[DocumentChunkRecord]) -> IndexRecord:
        for chunk_id in [chunk_id for chunk_id in self._chunk_terms if chunk_id.startswith(f"{document.document_id}_chunk_")]:
            del self._chunk_terms[chunk_id]
        for chunk in chunks:
            self._chunk_terms[chunk.chunk_id] = Counter(_tokenize(chunk.text))
        return IndexRecord(
            index_ref=self.build_index_ref(document),
            tenant_id=document.tenant_id,
            knowledge_base_id=document.knowledge_base_id,
            document_id=document.document_id,
            chunk_count=len(chunks),
        )

    def score(self, query_terms: Counter[str], chunk: DocumentChunkRecord) -> float:
        chunk_terms = self._chunk_terms.get(chunk.chunk_id, Counter())
        if not chunk_terms:
            return 0.0
        overlap = sum(min(query_terms[term], chunk_terms[term]) for term in query_terms)
        if overlap == 0:
            return 0.0
        denominator = math.sqrt(sum(query_terms.values())) * math.sqrt(sum(chunk_terms.values()))
        return round(overlap / denominator, 4)


class CitationBuilder:
    def build(self, document: DocumentRecord, chunk: DocumentChunkRecord) -> CitationRecord:
        source_uri = f"{document.index_ref}#chunk/{chunk.chunk_id}" if document.index_ref else f"local-index://document/{document.document_id}#chunk/{chunk.chunk_id}"
        return CitationRecord(
            document_id=document.document_id,
            chunk_id=chunk.chunk_id,
            source_title=document.filename,
            source_uri=source_uri,
            mock_source=False,
        )


class KnowledgeSearchEngine:
    def __init__(
        self,
        registry: DocumentRegistry | None = None,
        parser: DocumentParser | None = None,
        chunker: DocumentChunker | None = None,
        index: LocalTermIndex | None = None,
        citation_builder: CitationBuilder | None = None,
    ) -> None:
        self.registry = registry or DocumentRegistry(source_root=service_root())
        self.parser = parser or DocumentParser()
        self.chunker = chunker or DocumentChunker()
        self.index = index or LocalTermIndex()
        self.citation_builder = citation_builder or CitationBuilder()

    def register_document(self, **kwargs: Any) -> DocumentRecord:
        return self.registry.register_document(**kwargs)

    def parse_document(
        self,
        *,
        document_id: str,
        tenant_id: str,
        requested_by: str,
        trace_id: str,
    ) -> DocumentRecord:
        if not tenant_id or not requested_by or not trace_id:
            raise RagServiceError(
                ErrorInfo(
                    error_code="VALIDATION_FAILED",
                    message="Document parsing requires tenant, requester, and trace context.",
                    trace_id=trace_id or "missing_trace",
                    retryable=False,
                )
            )
        document = self._document(document_id, tenant_id=tenant_id, trace_id=trace_id)
        document.trace_id = trace_id
        self.registry.mark_parsing(document)
        try:
            parsed_text = self.parser.parse_document(document, self.registry.load_source_text(document))
            chunks = self.chunker.chunk_document(document, parsed_text)
            if not chunks:
                raise RagServiceError(
                    ErrorInfo(
                        error_code="DOCUMENT_PARSE_FAILED",
                        message=f"Document {document.document_id} produced no chunks.",
                        trace_id=document.trace_id,
                        retryable=True,
                    )
                )
            self.registry.replace_chunks(document, chunks)
            index = self.index.add_document(document, chunks)
            return self.registry.mark_indexed(document, index)
        except RagServiceError as exc:
            return self.registry.mark_failed(document, exc.error)

    def ingest_document(self, **kwargs: Any) -> DocumentRecord:
        document = self.register_document(**kwargs)
        return self.parse_document(
            document_id=document.document_id,
            tenant_id=document.tenant_id,
            requested_by=document.created_by,
            trace_id=document.trace_id,
        )

    def retry_parse(
        self,
        *,
        document_id: str,
        tenant_id: str,
        requested_by: str,
        trace_id: str,
        source_text: str | None = None,
    ) -> DocumentRecord:
        if not tenant_id or not requested_by or not trace_id:
            raise RagServiceError(
                ErrorInfo(
                    error_code="VALIDATION_FAILED",
                    message="Document parse retry requires tenant, requester, and trace context.",
                    trace_id=trace_id or "missing_trace",
                    retryable=False,
                )
            )
        document = self._document(document_id, tenant_id=tenant_id, trace_id=trace_id)
        if source_text is not None:
            document.source_text = source_text
        return self.parse_document(
            document_id=document_id,
            tenant_id=tenant_id,
            requested_by=requested_by,
            trace_id=trace_id,
        )

    def list_documents(self, tenant_id: str) -> list[dict[str, Any]]:
        return [self.document_summary(document) for document in self.registry.documents.values() if document.tenant_id == tenant_id]

    def search(
        self,
        *,
        tenant_id: str,
        requested_by: str,
        trace_id: str,
        query: str,
        top_k: int = 3,
        filters: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        results = self.search_results(
            tenant_id=tenant_id,
            trace_id=trace_id,
            query=query,
            top_k=top_k,
            filters=filters,
        )
        return {
            "tool_name": "knowledge.search.v1",
            "tool_version": "v1",
            "tenant_id": tenant_id,
            "trace_id": trace_id,
            "status": "success",
            "output": {
                "items": [to_dict(result) for result in results],
                "mock_source": False,
            },
            "error": None,
        }

    def search_results(
        self,
        *,
        tenant_id: str,
        trace_id: str,
        query: str,
        top_k: int,
        filters: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        if top_k < 1 or top_k > 20:
            raise RagServiceError(
                ErrorInfo(
                    error_code="VALIDATION_FAILED",
                    message="knowledge.search.v1 top_k must be between 1 and 20.",
                    trace_id=trace_id,
                    retryable=False,
                )
            )
        query_terms = Counter(_tokenize(query))
        if not query_terms:
            raise RagServiceError(
                ErrorInfo(
                    error_code="VALIDATION_FAILED",
                    message="knowledge.search.v1 query must contain searchable terms.",
                    trace_id=trace_id,
                    retryable=False,
                )
            )
        active_filters = filters or {}
        scored: list[tuple[float, DocumentRecord, DocumentChunkRecord]] = []
        for chunk in self.registry.chunks.values():
            document = self.registry.documents[chunk.document_id]
            if not self._matches(document, tenant_id, active_filters):
                continue
            score = self.index.score(query_terms, chunk)
            if score > 0:
                scored.append((score, document, chunk))
        scored.sort(key=lambda item: (-item[0], item[2].document_id, item[2].sequence))
        return [self._to_result(score, document, chunk) for score, document, chunk in scored[:top_k]]

    def document_summary(self, document: DocumentRecord) -> dict[str, Any]:
        return {
            "document_id": document.document_id,
            "knowledge_base_id": document.knowledge_base_id,
            "tenant_id": document.tenant_id,
            "filename": document.filename,
            "parse_status": document.parse_status,
            "chunk_count": document.chunk_count,
            "index_ref": document.index_ref,
            "error": to_dict(document.error) if document.error else None,
        }

    def _document(self, document_id: str, *, tenant_id: str, trace_id: str) -> DocumentRecord:
        document = self.registry.documents.get(document_id)
        if document is None or document.tenant_id != tenant_id:
            raise RagServiceError(
                ErrorInfo(
                    error_code="RESOURCE_NOT_FOUND",
                    message=f"Document {document_id} was not found.",
                    trace_id=trace_id or "missing_trace",
                    retryable=False,
                )
            )
        return document

    def _matches(self, document: DocumentRecord, tenant_id: str, filters: dict[str, Any]) -> bool:
        if document.tenant_id != tenant_id or document.parse_status != DOCUMENT_INDEXED:
            return False
        if filters.get("knowledge_base_id") and document.knowledge_base_id != filters["knowledge_base_id"]:
            return False
        if filters.get("document_id") and document.document_id != filters["document_id"]:
            return False
        if filters.get("parse_status") and filters["parse_status"] != DOCUMENT_INDEXED:
            return False
        return True

    def _to_result(self, score: float, document: DocumentRecord, chunk: DocumentChunkRecord) -> SearchResult:
        snippet = chunk.text[:240]
        return SearchResult(
            document_id=document.document_id,
            chunk_id=chunk.chunk_id,
            snippet=snippet,
            score=score,
            citation=self.citation_builder.build(document, chunk),
            mock_source=False,
        )


def service_root() -> Path:
    return Path(__file__).resolve().parents[2]


def create_demo_engine() -> KnowledgeSearchEngine:
    root = service_root()
    fixtures = root / "fixtures"
    engine = KnowledgeSearchEngine(registry=DocumentRegistry(source_root=root))
    demo_documents = [
        {
            "document_id": "doc_billing_complaint_sop",
            "knowledge_base_id": "kb_support_policy",
            "filename": "billing_complaint_sop.txt",
            "object_key": "fixtures/billing_complaint_sop.txt",
            "source_text": (fixtures / "billing_complaint_sop.txt").read_text(encoding="utf-8"),
        },
        {
            "document_id": "doc_refund_policy_faq",
            "knowledge_base_id": "kb_support_policy",
            "filename": "refund_policy_faq.txt",
            "object_key": "fixtures/refund_policy_faq.txt",
            "source_text": (fixtures / "refund_policy_faq.txt").read_text(encoding="utf-8"),
        },
    ]
    for item in demo_documents:
        engine.ingest_document(
            tenant_id="tenant_demo",
            requested_by="user_ops_demo",
            trace_id=f"trace_{item['document_id']}",
            **item,
        )
    return engine
