import os

from langchain_openai import ChatOpenAI
from langchain.tools import tool
from langchain.agents import create_agent
from langgraph.checkpoint.memory import InMemorySaver

# =========================
# 1. 配置百炼大模型
# =========================

BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

llm = ChatOpenAI(
    model="qwen-plus",
    api_key=os.environ["DASHSCOPE_API_KEY"],
    base_url=BASE_URL,
    temperature=0,
)

# =========================
# 2. 定义工具
# =========================

@tool
def get_weather(city: str) -> str:
    """查询指定城市的天气。这里只是演示工具，返回模拟数据。"""
    fake_data = {
        "上海": "上海今天多云，气温约25℃。",
        "北京": "北京今天晴，气温约20℃。",
        "成都": "成都今天小雨，气温约22℃。",
    }
    return fake_data.get(city, f"暂时没有 {city} 的天气数据。")


@tool
def calculator(expression: str) -> str:
    """计算简单的四则运算表达式，例如 12*8+3。"""
    allowed_chars = set("0123456789+-*/(). ")
    if not set(expression) <= allowed_chars:
        return "只支持简单四则运算。"

    try:
        result = eval(expression, {"__builtins__": {}}, {})
        return str(result)
    except Exception as e:
        return f"计算失败：{e}"


tools = [get_weather, calculator]

# =========================
# 3. 创建 Agent
# =========================

agent = create_agent(
    model=llm,
    tools=tools,
    system_prompt="你是一个中文助手。能直接回答就直接回答；需要工具时再调用工具。",
    checkpointer=InMemorySaver(),  # 本地短期记忆，不用 LangSmith
)

# =========================
# 4. 运行 Agent
# =========================

def ask(question: str, thread_id: str = "demo"):
    result = agent.invoke(
        {"messages": [{"role": "user", "content": question}]},
        config={"configurable": {"thread_id": thread_id}},
    )
    print("\n用户：", question)
    print("助手：", result["messages"][-1].content)


ask("你好，我叫 Cyper。")
ask("我叫什么名字？")
ask("12*8+3 等于多少？")
ask("上海天气怎么样？")