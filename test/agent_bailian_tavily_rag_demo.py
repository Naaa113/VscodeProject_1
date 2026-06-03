import os

from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain_core.tools import tool
from langgraph.checkpoint.memory import InMemorySaver

from langchain_core.documents import Document

try:
    from langchain_core.tools import create_retriever_tool
except ImportError:
    from langchain.tools.retriever import create_retriever_tool

from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_community.embeddings import DashScopeEmbeddings

from langchain_tavily import TavilySearch


# ============================================================
# 1. 检查环境变量
# ============================================================

def require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise ValueError(f"没有找到环境变量 {name}，请先配置。")
    return value


DASHSCOPE_API_KEY = require_env("DASHSCOPE_API_KEY")
TAVILY_API_KEY = require_env("TAVILY_API_KEY")


# ============================================================
# 2. 配置百炼 Qwen 大模型
# ============================================================

BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

llm = ChatOpenAI(
    model="qwen-plus",
    api_key=DASHSCOPE_API_KEY,
    base_url=BASE_URL,
    temperature=0,
)


# ============================================================
# 3. 构造本地知识库文档
# ============================================================

docs = [
    Document(
        page_content=(
            "猫是一种常见的家养动物，通常具有柔软的皮毛、敏锐的听觉、"
            "较强的夜视能力和灵活的身体。猫常通过叫声、尾巴动作和身体姿态表达情绪。"
        ),
        metadata={"source": "cat_doc"},
    ),
    Document(
        page_content=(
            "LangChain 是一个用于构建大模型应用的框架，常见用途包括 RAG、本地知识库问答、"
            "工具调用、Agent 智能体和多步骤工作流。"
        ),
        metadata={"source": "langchain_doc"},
    ),
    Document(
        page_content=(
            "Agent 智能体可以让大模型根据用户问题自动决定是否调用工具。"
            "例如，可以调用搜索工具、数据库工具、计算器工具或本地知识库检索工具。"
        ),
        metadata={"source": "agent_doc"},
    ),
]


# ============================================================
# 4. 文本切分
# ============================================================

splitter = RecursiveCharacterTextSplitter(
    chunk_size=200,
    chunk_overlap=20,
)

splits = splitter.split_documents(docs)


# ============================================================
# 5. 使用百炼 Embedding 创建 FAISS 向量库
# ============================================================

embeddings = DashScopeEmbeddings(
    model="text-embedding-v3",
    dashscope_api_key=DASHSCOPE_API_KEY,
)

vectorstore = FAISS.from_documents(splits, embeddings)

retriever = vectorstore.as_retriever(
    search_kwargs={"k": 2}
)


# ============================================================
# 6. 把 Retriever 包装成 Agent 工具
# ============================================================

retriever_tool = create_retriever_tool(
    retriever,
    "local_knowledge_search",
    "当用户询问猫、LangChain、Agent、本地知识库相关问题时，使用这个工具检索本地资料。",
)


# ============================================================
# 7. 创建 Tavily 搜索工具
# ============================================================

tavily_tool = TavilySearch(
    max_results=3,
    topic="general",
)


# ============================================================
# 8. 创建计算器工具
# ============================================================

@tool
def calculator(expression: str) -> str:
    """计算简单四则运算表达式，例如 12*8+3。"""
    allowed_chars = set("0123456789+-*/(). ")

    if not set(expression) <= allowed_chars:
        return "只支持简单四则运算。"

    try:
        result = eval(expression, {"__builtins__": {}}, {})
        return str(result)
    except Exception as e:
        return f"计算失败：{e}"


# ============================================================
# 9. 工具列表
# ============================================================

tools = [
    tavily_tool,
    retriever_tool,
    calculator,
]


# ============================================================
# 10. 创建记忆存储器
# ============================================================
# 注意：
# InMemorySaver 是“内存型短期记忆”。
# 只要当前 Python 程序没有关闭，它就能记住同一个 thread_id 下的上下文。
# 程序关闭后，记忆会丢失。

memory = InMemorySaver()


# ============================================================
# 11. 创建带记忆的 Agent
# ============================================================

agent = create_agent(
    model=llm,
    tools=tools,
    system_prompt=(
        "你是一个中文智能助手。"
        "你可以根据问题决定是否调用工具。"
        "如果问题涉及实时信息、新闻、天气、最新资料、外部网页信息，优先使用 Tavily 搜索工具。"
        "如果问题涉及本地知识库中的猫、LangChain、Agent 等内容，优先使用 local_knowledge_search。"
        "如果问题涉及数学计算，使用 calculator。"
        "如果不需要工具，就直接回答。"
        "你需要结合当前会话历史回答用户问题。"
    ),
    checkpointer=memory,
)


# ============================================================
# 12. 封装带 thread_id 的提问函数
# ============================================================

def ask(question: str, thread_id: str = "default-session") -> str:
    """
    thread_id 表示一段会话。
    同一个 thread_id 会共享历史记忆。
    不同 thread_id 之间互不影响。
    """

    result = agent.invoke(
        {
            "messages": [
                {
                    "role": "user",
                    "content": question,
                }
            ]
        },
        config={
            "configurable": {
                "thread_id": thread_id
            }
        },
    )

    answer = result["messages"][-1].content

    print("\n" + "=" * 80)
    print(f"会话ID：{thread_id}")
    print(f"用户：{question}")
    print(f"助手：{answer}")

    return answer


# ============================================================
# 13. 记忆功能测试
# ============================================================

def run_memory_demo():
    print("\n\n==================== 记忆功能测试 ====================")

    # 同一个会话：应该能记住名字
    ask("你好，我叫 Cyper。", thread_id="session-A")
    ask("我叫什么名字？", thread_id="session-A")

    # 换一个会话：不应该知道刚才的名字
    ask("我叫什么名字？", thread_id="session-B")

    print("\n说明：")
    print("session-A 中，Agent 应该能记住你叫 Cyper。")
    print("session-B 是新会话，所以它通常不知道你叫什么。")


# ============================================================
# 14. 工具调用测试
# ============================================================

def run_tool_demo():
    print("\n\n==================== 工具调用测试 ====================")

    ask("猫有什么特征？", thread_id="tool-test")
    ask("LangChain 可以用来做什么？", thread_id="tool-test")
    ask("25*4+10 等于多少？", thread_id="tool-test")
    ask("今天上海天气怎么样？", thread_id="tool-test")
    ask("最近人工智能领域有什么新闻？", thread_id="tool-test")


# ============================================================
# 15. 交互式聊天
# ============================================================

def chat_loop():
    print("\n\n==================== 进入交互式聊天 ====================")
    print("输入 exit / quit / q 可以退出。")

    thread_id = input("请输入会话ID，直接回车默认使用 default-session：").strip()

    if not thread_id:
        thread_id = "default-session"

    print(f"\n当前会话ID：{thread_id}")
    print("同一个会话ID 下，Agent 会记住前面的对话。")

    while True:
        question = input("\n你：").strip()

        if question.lower() in {"exit", "quit", "q"}:
            print("已退出。")
            break

        if not question:
            continue

        try:
            answer = ask(question, thread_id=thread_id)
            print(f"\n助手：{answer}")
        except Exception as e:
            print(f"\n运行出错：{e}")


# ============================================================
# 16. 主程序入口
# ============================================================

if __name__ == "__main__":
    # 先自动测试记忆功能
    run_memory_demo()

    # 再自动测试工具调用
    run_tool_demo()

    # 最后进入可连续对话的交互模式
    chat_loop()