import os
import streamlit as st
import wikipedia
import time

from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.vectorstores import FAISS
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser


# =========================
# 1. 模型配置
# =========================

API_KEY = os.getenv("OPENAI_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
BASE_URL = os.getenv(
    "OPENAI_BASE_URL",
    "https://dashscope.aliyuncs.com/compatible-mode/v1"
)

CHAT_MODEL = os.getenv("CHAT_MODEL", "qwen-plus")
EMBED_MODEL = os.getenv("EMBED_MODEL", "text-embedding-v4")


# =========================
# 2. 获取维基百科内容
# =========================

def get_wiki(keyword: str):
    """
    根据用户输入的关键词，从中文维基百科获取：
    1. 词条标题
    2. 摘要
    3. 完整正文
    """

    wikipedia.set_lang("zh")

    search_results = wikipedia.search(keyword, results=5)

    if not search_results:
        return None, None, None

    title = search_results[0]

    try:
        page = wikipedia.page(title, auto_suggest=False)
    except wikipedia.DisambiguationError as e:
        # 如果关键词对应多个词条，默认取第一个候选项
        page = wikipedia.page(e.options[0], auto_suggest=False)
    except wikipedia.PageError:
        return None, None, None

    try:
        summary = wikipedia.summary(page.title, sentences=5, auto_suggest=False)
    except Exception:
        summary = page.summary

    return page.title, summary, page.content


# =========================
# 3. 初始化 FAISS 向量库
# =========================

def init_vector_db(title: str, wiki_content: str):
    """
    将维基百科正文切分成小块，然后分批转成向量，逐批写入 FAISS。
    """

    max_chars = 40000
    wiki_content = wiki_content[:max_chars]

    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=400,
        chunk_overlap=60,
        separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""]
    )

    texts = text_splitter.split_text(wiki_content)

    texts = [t.strip() for t in texts if isinstance(t, str) and len(t.strip()) >= 30]

    if len(texts) == 0:
        raise ValueError("维基百科内容为空，无法建立向量库。")

    embeddings = OpenAIEmbeddings(
        model=EMBED_MODEL,
        api_key=API_KEY,
        base_url=BASE_URL,
        chunk_size=4,
        timeout=60,
        max_retries=6,
        check_embedding_ctx_length=False,
        encoding_format="float"
    )

    batch_size = 4
    vector_db = None

    for start in range(0, len(texts), batch_size):
        batch_texts = texts[start:start + batch_size]

        batch_metadatas = [
            {"source": title, "chunk": start + i}
            for i in range(len(batch_texts))
        ]

        last_error = None

        for attempt in range(3):
            try:
                if vector_db is None:
                    vector_db = FAISS.from_texts(
                        texts=batch_texts,
                        embedding=embeddings,
                        metadatas=batch_metadatas
                    )
                else:
                    vector_db.add_texts(
                        texts=batch_texts,
                        metadatas=batch_metadatas
                    )

                break

            except Exception as e:
                last_error = e
                time.sleep(2 + attempt * 2)

        else:
            raise RuntimeError(
                f"第 {start // batch_size + 1} 批文本向量化失败：{last_error}"
            )

    return vector_db
# =========================
# 4. 基于检索结果生成回答
# =========================

def get_bot_response(vector_db, title: str, user_question: str):
    """
    根据用户问题，从 FAISS 中检索相关文本，再交给大模型回答。
    """

    docs = vector_db.similarity_search(user_question, k=5)

    context = "\n\n".join(
        [f"片段{i + 1}：\n{doc.page_content}" for i, doc in enumerate(docs)]
    )

    llm = ChatOpenAI(
        model=CHAT_MODEL,
        api_key=API_KEY,
        base_url=BASE_URL,
        temperature=0
    )

    prompt = ChatPromptTemplate.from_messages([
        (
            "system",
            "你是一个基于维基百科资料回答问题的助手。"
            "请只根据给定资料回答。"
            "如果资料中没有足够信息，请明确说明：维基百科资料中没有足够信息。"
        ),
        (
            "human",
            "当前维基百科词条：{title}\n\n"
            "可参考资料：\n{context}\n\n"
            "用户问题：{question}\n\n"
            "请用中文回答。"
        )
    ])

    chain = prompt | llm | StrOutputParser()

    answer = chain.invoke({
        "title": title,
        "context": context,
        "question": user_question
    })

    return answer, docs


# =========================
# 5. Streamlit 网页界面
# =========================

st.set_page_config(
    page_title="基于维基百科的网页问答",
    page_icon="🔎",
    layout="centered"
)

st.markdown(
    "<h1 style='text-align: center;'>基于 Web URL 的问答</h1>",
    unsafe_allow_html=True
)

st.write("请输入一个维基百科关键词，系统会获取该词条内容，并基于该内容回答你的问题。")

if not API_KEY:
    st.error("未检测到 API Key。请先设置 OPENAI_API_KEY 或 DASHSCOPE_API_KEY。")
    st.stop()


keyword = st.text_input("请输入要检索的关键词", value="黄河")

if st.button("从维基百科获取摘要并建立索引"):
    if not keyword.strip():
        st.warning("请输入关键词。")
    else:
        with st.spinner("正在获取维基百科内容并建立 FAISS 向量库..."):
            title, summary, content = get_wiki(keyword.strip())

            if not content:
                st.error("没有获取到维基百科内容，请换一个关键词。")
            else:
                vector_db = init_vector_db(title, content)

                st.session_state["wiki_title"] = title
                st.session_state["wiki_summary"] = summary
                st.session_state["vector_db"] = vector_db

                st.success(f"已成功建立词条《{title}》的向量索引。")


if "wiki_summary" in st.session_state:
    st.subheader("从维基百科获取的摘要")
    st.write(st.session_state["wiki_summary"])


if "vector_db" in st.session_state:
    st.subheader("向该维基百科词条提问")

    user_question = st.text_input("请输入你的问题", value="")

    if st.button("Send"):
        if not user_question.strip():
            st.warning("请输入问题。")
        else:
            with st.spinner("正在检索相关内容并生成回答..."):
                answer, docs = get_bot_response(
                    st.session_state["vector_db"],
                    st.session_state["wiki_title"],
                    user_question.strip()
                )

            st.subheader("回答")
            st.write(answer)

            with st.expander("查看本次检索到的维基百科片段"):
                for i, doc in enumerate(docs, start=1):
                    st.markdown(f"**片段 {i}**")
                    st.write(doc.page_content)
                    st.divider()