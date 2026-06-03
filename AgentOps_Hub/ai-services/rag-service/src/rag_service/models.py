from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import UTC, datetime
from typing import Any
from uuid import uuid4


DOCUMENT_UPLOADED = "uploaded"
DOCUMENT_PARSING = "parsing"
DOCUMENT_INDEXED = "indexed"
DOCUMENT_FAILED = "failed"


def utc_now() -> str:
    return datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid4().hex[:12]}"


@dataclass
class ErrorInfo:
    error_code: str
    message: str
    trace_id: str
    retryable: bool
    details: dict[str, Any] = field(default_factory=dict)


@dataclass
class KnowledgeBaseRecord:
    knowledge_base_id: str
    tenant_id: str
    name: str
    created_by: str
    created_at: str = field(default_factory=utc_now)


@dataclass
class DocumentRecord:
    document_id: str
    knowledge_base_id: str
    tenant_id: str
    filename: str
    object_key: str
    created_by: str
    trace_id: str
    parse_status: str = DOCUMENT_UPLOADED
    chunk_count: int = 0
    index_ref: str | None = None
    error: ErrorInfo | None = None
    source_text: str | None = None
    created_at: str = field(default_factory=utc_now)
    updated_at: str = field(default_factory=utc_now)


@dataclass
class DocumentChunkRecord:
    chunk_id: str
    document_id: str
    knowledge_base_id: str
    tenant_id: str
    sequence: int
    text: str
    source_start: int
    source_end: int


@dataclass
class IndexRecord:
    index_ref: str
    tenant_id: str
    knowledge_base_id: str
    document_id: str
    chunk_count: int
    created_at: str = field(default_factory=utc_now)


@dataclass
class CitationRecord:
    document_id: str
    chunk_id: str
    source_title: str
    source_uri: str
    mock_source: bool = False


@dataclass
class SearchResult:
    document_id: str
    chunk_id: str
    snippet: str
    score: float
    citation: CitationRecord
    mock_source: bool = False


@dataclass
class RagEvalCase:
    case_id: str
    tenant_id: str
    query: str
    expected_document_id: str
    expected_terms: list[str]


@dataclass
class RagEvalResult:
    case_id: str
    passed: bool
    hit_rate: float
    citation_accuracy: float
    checks: dict[str, bool]


def to_dict(value: Any) -> Any:
    if hasattr(value, "__dataclass_fields__"):
        return {key: to_dict(item) for key, item in asdict(value).items()}
    if isinstance(value, list):
        return [to_dict(item) for item in value]
    if isinstance(value, dict):
        return {key: to_dict(item) for key, item in value.items()}
    return value
