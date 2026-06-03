"""
LangGraph 多代理协作 Demo：使用阿里云百炼 / DashScope 千问模型

安装：
pip install -U langgraph langchain langchain-openai pydantic

环境变量：
export DASHSCOPE_API_KEY="你的百炼 API Key"

如果你在 Windows PowerShell：
$env:DASHSCOPE_API_KEY="你的百炼 API Key"
"""

import os
import math
from typing import Annotated, Literal, TypedDict

from pydantic import BaseModel, Field

from langchain_openai import ChatOpenAI
from langchain_core.messages import AnyMessage, HumanMessage, SystemMessage
from langchain_core.tools import tool

from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode


# ============================================================
# 1. 百炼 / DashScope 模型配置
# ============================================================

DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY")

if not DASHSCOPE_API_KEY:
    raise RuntimeError("请先设置环境变量 DASHSCOPE_API_KEY")

# 北京地域：
DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

# 如果你使用国际站 / 新加坡地域，可以改成：
# DASHSCOPE_BASE_URL = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1"

llm = ChatOpenAI(
    model="qwen-plus",
    api_key=DASHSCOPE_API_KEY,
    base_url=DASHSCOPE_BASE_URL,
    temperature=0,
)

# 你也可以换成：
# model="qwen-turbo"
# model="qwen-max"
# model="qwen-coder-plus"


# ============================================================
# 2. 定义工具 Tools
# ============================================================

@tool
def search_docs(query: str) -> str:
    """
    模拟文档搜索工具。
    真实项目里可以替换成 RAG、向量数据库、企业知识库、搜索 API 等。
    """
    mock_knowledge = {
        "langgraph": (
            "LangGraph 使用图结构组织 Agent 工作流。核心概念包括："
            "State、Node、Edge、Graph、ToolNode。"
        ),
        "multi-agent": (
            "多代理协作适合复杂任务拆分。"
            "例如 Research Agent 负责查资料，Code Agent 负责写代码，"
            "Supervisor 负责调度。"
        ),
        "toolnode": (
            "ToolNode 是 LangGraph 预构建节点，用于执行大模型消息中的工具调用。"
            "常见流程是：Agent 产生 tool_calls，ToolNode 执行工具，再返回 Agent。"
        ),
    }

    q = query.lower()
    for key, value in mock_knowledge.items():
        if key in q:
            return value

    return "未找到精确资料。建议围绕 State、Node、Edge、ToolNode、Supervisor 设计。"


@tool
def calculate(expression: str) -> str:
    """
    简单计算工具。
    """
    allowed_names = {
        "sqrt": math.sqrt,
        "sin": math.sin,
        "cos": math.cos,
        "tan": math.tan,
        "log": math.log,
        "pi": math.pi,
        "e": math.e,
    }

    try:
        result = eval(expression, {"__builtins__": {}}, allowed_names)
        return str(result)
    except Exception as e:
        return f"计算失败：{e}"


@tool
def make_code_snippet(requirement: str) -> str:
    """
    模拟代码生成辅助工具。
    """
    return f"""
def demo():
    \"\"\"需求：{requirement}\"\"\"
    print("这是 Code Agent 生成的示例代码")

demo()
"""


# ============================================================
# 3. 定义状态 State
# ============================================================

class MultiAgentState(TypedDict):
    """
    多代理系统共享状态。

    messages:
        保存用户、代理、工具之间的消息历史。

    next:
        Supervisor 决定下一个节点。
    """
    messages: Annotated[list[AnyMessage], add_messages]
    next: str


# ============================================================
# 4. 给不同代理绑定不同工具
# ============================================================

research_tools = [search_docs, calculate]
code_tools = [make_code_snippet, calculate]

research_llm = llm.bind_tools(research_tools)
code_llm = llm.bind_tools(code_tools)


# ============================================================
# 5. 定义 Supervisor 结构化路由输出
# ============================================================

class RouteDecision(BaseModel):
    next: Literal["research_agent", "code_agent", "FINISH"] = Field(
        description="下一步执行哪个代理；任务完成则选择 FINISH。"
    )


router_llm = llm.with_structured_output(RouteDecision)


# ============================================================
# 6. 定义代理节点 Agent Nodes
# ============================================================

def supervisor_node(state: MultiAgentState) -> dict:
    """
    Supervisor 节点：
    负责多代理协作调度。
    """
    system_prompt = SystemMessage(content="""
你是一个 LangGraph 多代理系统的 Supervisor。

你有两个下属代理：

1. research_agent
   - 负责查资料、解释概念、总结知识点
   - 可以使用 search_docs 和 calculate 工具

2. code_agent
   - 负责生成 Python 代码 demo
   - 可以使用 make_code_snippet 和 calculate 工具

调度规则：
- 如果用户需求还需要理解、梳理或补充知识点，选择 research_agent。
- 如果已经可以开始写代码，选择 code_agent。
- 如果代码已经生成且任务完整，选择 FINISH。
- 不要无限循环。
""")

    decision = router_llm.invoke([system_prompt] + state["messages"])
    return {"next": decision.next}


def research_agent_node(state: MultiAgentState) -> dict:
    """
    Research Agent：
    负责整理 LangGraph 多代理协作的关键知识点。
    """
    system_prompt = SystemMessage(content="""
你是 Research Agent。

职责：
- 解释 LangGraph 多代理协作中的概念
- 必要时调用 search_docs
- 必要时调用 calculate
- 不输出最终完整代码，只为 Code Agent 准备实现思路

必须覆盖：
- 协作
- 创建代理
- 定义工具
- 创建图
- 定义状态
- 定义代理节点
- 定义工具节点
- 定义边逻辑
- 定义图
- 调用
""")

    response = research_llm.invoke([system_prompt] + state["messages"])
    return {"messages": [response]}


def code_agent_node(state: MultiAgentState) -> dict:
    """
    Code Agent：
    负责生成最终代码。
    """
    system_prompt = SystemMessage(content="""
你是 Code Agent。

请生成一个使用阿里云百炼 DashScope 千问模型的 LangGraph 多代理协作 Python demo。

代码必须体现：
1. 百炼模型配置
2. 定义工具
3. 定义 State
4. 创建 Research Agent 和 Code Agent
5. 创建 ToolNode
6. 定义条件边
7. 编译 graph
8. invoke 调用

最终输出应该是清晰、可运行、可教学的代码。
""")

    response = code_llm.invoke([system_prompt] + state["messages"])
    return {"messages": [response]}


# ============================================================
# 7. 定义工具节点 Tool Nodes
# ============================================================

research_tool_node = ToolNode(research_tools)
code_tool_node = ToolNode(code_tools)


# ============================================================
# 8. 定义边逻辑 Edge Logic
# ============================================================

def route_from_supervisor(state: MultiAgentState) -> str:
    """
    Supervisor 条件边：
    根据 state["next"] 决定下一个节点。
    """
    if state["next"] == "research_agent":
        return "research_agent"

    if state["next"] == "code_agent":
        return "code_agent"

    return "FINISH"


def route_research_agent(state: MultiAgentState) -> str:
    """
    Research Agent 条件边：
    - 如果产生工具调用，进入 research_tools
    - 否则回到 supervisor
    """
    last_message = state["messages"][-1]

    if getattr(last_message, "tool_calls", None):
        return "research_tools"

    return "supervisor"


def route_code_agent(state: MultiAgentState) -> str:
    """
    Code Agent 条件边：
    - 如果产生工具调用，进入 code_tools
    - 否则回到 supervisor
    """
    last_message = state["messages"][-1]

    if getattr(last_message, "tool_calls", None):
        return "code_tools"

    return "supervisor"


# ============================================================
# 9. 创建图 Graph
# ============================================================

workflow = StateGraph(MultiAgentState)

workflow.add_node("supervisor", supervisor_node)
workflow.add_node("research_agent", research_agent_node)
workflow.add_node("research_tools", research_tool_node)
workflow.add_node("code_agent", code_agent_node)
workflow.add_node("code_tools", code_tool_node)

workflow.add_edge(START, "supervisor")

workflow.add_conditional_edges(
    "supervisor",
    route_from_supervisor,
    {
        "research_agent": "research_agent",
        "code_agent": "code_agent",
        "FINISH": END,
    },
)

workflow.add_conditional_edges(
    "research_agent",
    route_research_agent,
    {
        "research_tools": "research_tools",
        "supervisor": "supervisor",
    },
)

workflow.add_edge("research_tools", "research_agent")

workflow.add_conditional_edges(
    "code_agent",
    route_code_agent,
    {
        "code_tools": "code_tools",
        "supervisor": "supervisor",
    },
)

workflow.add_edge("code_tools", "code_agent")


# ============================================================
# 10. 定义图并编译
# ============================================================

graph = workflow.compile()


# ============================================================
# 11. 调用
# ============================================================

if __name__ == "__main__":
    result = graph.invoke(
        {
            "messages": [
                HumanMessage(content="""
请根据 LangGraph 多代理协作知识点，生成一个完整代码 demo。
要求覆盖：
协作、创建代理、定义工具、创建图、定义状态、
定义代理节点、定义工具节点、定义边逻辑、定义图、调用。
""")
            ],
            "next": "",
        },
        config={
            "recursion_limit": 20
        },
    )

    print("\n========== 最终输出 ==========\n")
    print(result["messages"][-1].content)