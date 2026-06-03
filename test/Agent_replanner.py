import os
import operator
import asyncio
from typing import Annotated, List, Tuple, TypedDict, Union, Literal

from pydantic import BaseModel, Field

from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser
from langchain_community.tools.tavily_search import TavilySearchResults

from langgraph.graph import StateGraph, START, END
from langgraph.prebuilt import create_react_agent


# ============================================================
# 0. 禁用 LangSmith / Smith 追踪
# ============================================================

os.environ["LANGCHAIN_TRACING_V2"] = "false"
os.environ.pop("LANGCHAIN_API_KEY", None)
os.environ.pop("LANGCHAIN_PROJECT", None)


# ============================================================
# 1. 百炼模型配置
# ============================================================

DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY")

if not DASHSCOPE_API_KEY:
    raise RuntimeError(
        "请先设置环境变量 DASHSCOPE_API_KEY，例如：export DASHSCOPE_API_KEY='你的百炼API Key'"
    )

BAILIAN_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"

# 你也可以替换成 qwen-plus、qwen-max、qwen-turbo 等
PLANNER_MODEL = "qwen-plus"
EXECUTOR_MODEL = "qwen-plus"
REPLANNER_MODEL = "qwen-plus"


def make_llm(model: str, temperature: float = 0):
    return ChatOpenAI(
        model=model,
        api_key=DASHSCOPE_API_KEY,
        base_url=BAILIAN_BASE_URL,
        temperature=temperature,
    )


planner_llm = make_llm(PLANNER_MODEL, temperature=0)
executor_llm = make_llm(EXECUTOR_MODEL, temperature=0)
replanner_llm = make_llm(REPLANNER_MODEL, temperature=0)


# ============================================================
# 2. 定义工具
# ============================================================

if not os.getenv("TAVILY_API_KEY"):
    raise RuntimeError(
        "请先设置环境变量 TAVILY_API_KEY。这个示例用 Tavily 作为 ReAct Agent 的搜索工具。"
    )

tools = [
    TavilySearchResults(
        max_results=3,
        description="用于联网搜索最新信息、事实资料、人物、地点、赛事、公司、技术文档等。"
    )
]


# ============================================================
# 3. 定义 ReAct 执行代理
# ============================================================

executor_prompt = """
你是一个执行型 Agent。

你只负责执行用户计划中的当前步骤，不需要重新规划整个任务。

要求：
1. 如果需要事实信息，请优先使用工具搜索。
2. 只完成当前步骤，不要擅自跳到后续步骤。
3. 输出要简洁、明确，并说明你本步骤得到的关键信息。
"""

agent_executor = create_react_agent(
    model=executor_llm,
    tools=tools,
    prompt=executor_prompt,
)


# ============================================================
# 4. 定义 LangGraph 状态
# ============================================================

class PlanExecuteState(TypedDict):
    input: str
    plan: List[str]
    past_steps: Annotated[List[Tuple[str, str]], operator.add]
    response: str


# ============================================================
# 5. 定义规划器 Planner
# ============================================================

class Plan(BaseModel):
    """后续要执行的计划。"""

    steps: List[str] = Field(
        description="需要执行的步骤列表，必须按顺序排列。每一步都应该是清晰、独立、可执行的任务。"
    )


plan_parser = PydanticOutputParser(pydantic_object=Plan)

planner_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """
你是一个任务规划器。

请根据用户目标，制定一个简洁、可执行的多步骤计划。

要求：
1. 每一步必须是独立可执行的任务。
2. 不要添加无意义步骤。
3. 最后一步应该能帮助得到最终答案。
4. 只输出 JSON，不要输出 Markdown。
5. JSON 必须符合以下格式说明：

{format_instructions}
"""
        ),
        ("human", "{input}"),
    ]
).partial(format_instructions=plan_parser.get_format_instructions())


async def plan_step(state: PlanExecuteState):
    """根据用户输入生成初始计划。"""

    chain = planner_prompt | planner_llm | plan_parser
    plan = await chain.ainvoke({"input": state["input"]})

    return {
        "plan": plan.steps,
        "past_steps": [],
        "response": "",
    }


# ============================================================
# 6. 定义执行节点
# ============================================================

async def execute_step(state: PlanExecuteState):
    """执行当前计划中的第一步。"""

    plan = state["plan"]

    if not plan:
        return {
            "response": "没有可执行的计划步骤，无法继续。"
        }

    current_task = plan[0]

    plan_text = "\n".join(
        f"{index + 1}. {step}"
        for index, step in enumerate(plan)
    )

    task_prompt = f"""
用户的原始目标是：

{state["input"]}

当前完整计划是：

{plan_text}

你现在只需要执行第 1 步：

{current_task}

请完成这个步骤，并给出本步骤的结果。
"""

    agent_result = await agent_executor.ainvoke(
        {
            "messages": [
                HumanMessage(content=task_prompt)
            ]
        }
    )

    last_message = agent_result["messages"][-1].content

    return {
        "past_steps": [
            (current_task, last_message)
        ]
    }


# ============================================================
# 7. 定义重规划器 Replanner
# ============================================================

class Response(BaseModel):
    """最终给用户的回答。"""

    response: str = Field(description="可以直接返回给用户的最终答案。")


class Act(BaseModel):
    """下一步动作。"""

    action_type: Literal["response", "plan"] = Field(
        description="如果任务已经完成，填 response；如果还需要继续执行，填 plan。"
    )

    response: str = Field(
        default="",
        description="当 action_type=response 时，填写最终答案；否则留空。"
    )

    steps: List[str] = Field(
        default_factory=list,
        description="当 action_type=plan 时，填写后续仍需执行的步骤；否则为空列表。"
    )


act_parser = PydanticOutputParser(pydantic_object=Act)

replanner_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """
你是一个重规划器。

你会拿到：
1. 用户的原始目标
2. 原计划
3. 已经完成的步骤及其结果

你需要判断：
- 如果已经可以回答用户，输出 action_type=response，并填写 response。
- 如果还需要继续执行，输出 action_type=plan，并只填写“剩余需要执行的步骤”。

要求：
1. 不要重复已经完成的步骤。
2. 如果已有信息足够，必须直接 response，不要继续 plan。
3. 只输出 JSON，不要输出 Markdown。
4. JSON 必须符合以下格式说明：

{format_instructions}
"""
        ),
        (
            "human",
            """
用户原始目标：

{input}

当前计划：

{plan}

已经完成的步骤和结果：

{past_steps}

请判断下一步应该继续执行，还是直接回答用户。
"""
        ),
    ]
).partial(format_instructions=act_parser.get_format_instructions())


async def replan_step(state: PlanExecuteState):
    """根据已完成步骤决定继续执行还是结束。"""

    chain = replanner_prompt | replanner_llm | act_parser

    result = await chain.ainvoke(
        {
            "input": state["input"],
            "plan": state["plan"],
            "past_steps": state["past_steps"],
        }
    )

    if result.action_type == "response":
        return {
            "response": result.response
        }

    return {
        "plan": result.steps
    }


# ============================================================
# 8. 定义结束条件
# ============================================================

def should_end(state: PlanExecuteState):
    """判断是否结束工作流。"""

    if state.get("response"):
        return END

    if not state.get("plan"):
        return END

    return "agent"


# ============================================================
# 9. 创建 LangGraph 工作流
# ============================================================

workflow = StateGraph(PlanExecuteState)

workflow.add_node("planner", plan_step)
workflow.add_node("agent", execute_step)
workflow.add_node("replan", replan_step)

workflow.add_edge(START, "planner")
workflow.add_edge("planner", "agent")
workflow.add_edge("agent", "replan")

workflow.add_conditional_edges(
    "replan",
    should_end,
    {
        "agent": "agent",
        END: END,
    },
)

app = workflow.compile()


# ============================================================
# 10. 运行入口
# ============================================================

async def main():
    question = input("请输入你的任务：").strip()

    if not question:
        question = "请查找 2024 年澳大利亚网球公开赛男子单打冠军是谁，并告诉我他的家乡在哪里。"

    result = await app.ainvoke(
        {
            "input": question,
            "plan": [],
            "past_steps": [],
            "response": "",
        },
        config={
            "recursion_limit": 20
        }
    )

    print("\n========== 初始 / 最新计划 ==========")
    for index, step in enumerate(result.get("plan", []), start=1):
        print(f"{index}. {step}")

    print("\n========== 已完成步骤 ==========")
    for index, item in enumerate(result.get("past_steps", []), start=1):
        task, output = item
        print(f"\n[{index}] 任务：{task}")
        print(f"结果：{output}")

    print("\n========== 最终答案 ==========")
    print(result.get("response", "没有生成最终答案。"))


if __name__ == "__main__":
    asyncio.run(main())