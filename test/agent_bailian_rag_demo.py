import os

from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain.tools import tool
from langgraph.checkpoint.memory import InMemorySaver

from langchain_core.documents import Document
from langchain_core.tools import create_retriever_tool
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_community.embeddings import DashScopeEmbeddings

# =========================
# 1. 百炼 Qwen 大模型
# =========================

BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

llm = ChatOpenAI(
    model="qwen-plus",
    api_key=os.environ["DASHSCOPE_API_KEY"],
    base_url=BASE_URL,
    temperature=0,
)

# =========================
# 2. 构造本地文档
# =========================

docs = [
    Document(
        page_content="猫是一种常见的家养动物，通常具有柔软的皮毛、敏锐的听觉、较强的夜视能力和灵活的身体。",
        metadata={"source": "cat_doc"},
    ),
    Document(
        page_content="LangChain 是一个用于构建大模型应用的框架，常见用途包括 RAG、本地知识库问答、工具调用和 Agent 智能体。",
        metadata={"source": "langchain_doc"},
    ),
    Document(
        page_content="Agent 智能体可以让大模型根据问题自动决定是否调用工具，例如搜索工具、数据库工具、计算器工具或本地知识库检索工具。",
        metadata={"source": "agent_doc"},
    ),
]

# =========================
# 3. 文本切分
# =========================

splitter = RecursiveCharacterTextSplitter(
    chunk_size=200,
    chunk_overlap=20,
)

splits = splitter.split_documents(docs)

# =========================
# 4. 使用百炼 Embedding 建向量库
# =========================

embeddings = DashScopeEmbeddings(
    model="text-embedding-v3",
    dashscope_api_key=os.environ["DASHSCOPE_API_KEY"],
)

vectorstore = FAISS.from_documents(splits, embeddings)

retriever = vectorstore.as_retriever(
    search_kwargs={"k": 2}
)

# =========================
# 5. 把 Retriever 包装成工具
# =========================

retriever_tool = create_retriever_tool(
    retriever,
    "local_knowledge_search",
    "当用户询问猫、LangChain、Agent、本地知识库相关问题时，使用这个工具检索本地资料。",
)

# =========================
# 6. 再定义一个计算工具
# =========================

@tool
def calculator(expression: str) -> str:
    """计算简单四则运算表达式。"""
    allowed_chars = set("0123456789+-*/(). ")
    if not set(expression) <= allowed_chars:
        return "只支持简单四则运算。"

    try:
        return str(eval(expression, {"__builtins__": {}}, {}))
    except Exception as e:
        return f"计算失败：{e}"


tools = [retriever_tool, calculator]

# =========================
# 7. 创建 Agent
# =========================

agent = create_agent(
    model=llm,
    tools=tools,
    system_prompt=(
        "你是一个中文助手。"
        "如果用户的问题涉及本地知识库内容，请优先调用 local_knowledge_search。"
        "如果是数学计算，请调用 calculator。"
        "回答要清楚、简洁。"
    ),
    checkpointer=InMemorySaver(),
)

# =========================
# 8. 运行测试
# =========================

def ask(question: str, thread_id: str = "rag-demo"):
    result = agent.invoke(
        {"messages": [{"role": "user", "content": question}]},
        config={"configurable": {"thread_id": thread_id}},
    )
    print("\n用户：", question)
    print("助手：", result["messages"][-1].content)


ask("猫有什么特征？")
ask("LangChain 可以用来做什么？")
ask("Agent 智能体是什么意思？")
ask("25*4+10 等于多少？")