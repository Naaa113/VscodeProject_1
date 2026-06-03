import os
import uuid
from typing import List, Literal

from pydantic import BaseModel, Field

from langchain_openai import ChatOpenAI
from langchain_core.messages import (
    SystemMessage,
    HumanMessage,
    AIMessage,
    ToolMessage,
    BaseMessage,
)

from langgraph.graph import START, END, StateGraph, MessagesState
from langgraph.checkpoint.memory import MemorySaver


# ============================================================
# 0. 关闭 Smith / LangSmith 追踪
# ============================================================

os.environ["LANGSMITH_TRACING"] = "false"
os.environ["LANGCHAIN_TRACING_V2"] = "false"


# ============================================================
# 1. 配置百炼平台大模型
# ============================================================

DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY")

if not DASHSCOPE_API_KEY:
    raise ValueError(
        "请先设置环境变量 DASHSCOPE_API_KEY，例如：\n"
        "Windows PowerShell: setx DASHSCOPE_API_KEY \"你的百炼API_KEY\"\n"
        "macOS/Linux: export DASHSCOPE_API_KEY=\"你的百炼API_KEY\""
    )

# 中国站百炼 OpenAI 兼容地址通常使用：
# https://dashscope.aliyuncs.com/compatible-mode/v1
#
# 如果你使用国际站 / 新加坡区域，可以改成：
# https://dashscope-intl.aliyuncs.com/compatible-mode/v1
BASE_URL = os.getenv(
    "DASHSCOPE_BASE_URL",
    "https://dashscope.aliyuncs.com/compatible-mode/v1",
)

MODEL_NAME = os.getenv("DASHSCOPE_MODEL", "qwen-plus")

llm = ChatOpenAI(
    model=MODEL_NAME,
    api_key=DASHSCOPE_API_KEY,
    base_url=BASE_URL,
    temperature=0,
)


# ============================================================
# 2. 定义工具 Schema
# ============================================================
# 这个工具不是真正去查外部信息。
# 它的作用是：当 LLM 认为需求信息已经收集完整时，
# 通过调用该工具，把结构化需求传给后续 prompt 节点。


class PromptInstructions(BaseModel):
    """用于保存用户想创建的 Prompt 模板需求。"""

    objective: str = Field(
        description="Prompt 的目标，例如：收集客户满意度反馈、生成招聘 JD、生成学习计划等"
    )
    variables: List[str] = Field(
        description="Prompt 模板中需要填入的变量，例如：客户名称、评分、岗位名称、技能要求等"
    )
    constraints: List[str] = Field(
        description="输出不能做什么，例如：不能包含个人身份信息、不能编造事实等"
    )
    requirements: List[str] = Field(
        description="输出必须满足什么要求，例如：必须结构化、必须包含指定字段等"
    )


# 给 LLM 绑定工具。
# info 节点中的 LLM 可以决定：
# 1. 继续追问用户；
# 2. 调用 PromptInstructions 工具，表示需求已经收集完成。
llm_with_tools = llm.bind_tools([PromptInstructions])


# ============================================================
# 3. 定义“收集信息”节点使用的系统提示词
# ============================================================

info_template = """
Your job is to get information from a user about what type of prompt template they want to create.

You should get the following information from them:

- What the objective of the prompt is
- What variables will be passed into the prompt template
- Any constraints for what the output should NOT do
- Any requirements that the output MUST adhere to

If you are not able to discern this info, ask them to clarify.
Do not attempt to wildly guess.

After you are able to discern all the information, call the relevant tool.

请用中文和用户交流。
"""


def get_messages_info(messages: List[BaseMessage]) -> List[BaseMessage]:
    """
    给用户消息前面加上系统提示词。

    原始 messages:
        [HumanMessage("我想收集客户满意度反馈")]

    处理后:
        [
            SystemMessage(info_template),
            HumanMessage("我想收集客户满意度反馈")
        ]
    """
    return [SystemMessage(content=info_template)] + messages


def info_node(state: MessagesState):
    """
    info 节点：负责收集用户需求。

    如果信息不完整，LLM 会继续追问。
    如果信息完整，LLM 会调用 PromptInstructions 工具。
    """
    messages = get_messages_info(state["messages"])
    response = llm_with_tools.invoke(messages)
    return {"messages": [response]}


# ============================================================
# 4. 定义“生成 Prompt”节点
# ============================================================

prompt_system = """
Based on the following requirements, write a good prompt template:

{reqs}

请生成一份清晰、可复用、结构化的中文 Prompt 模板。
模板中需要保留变量占位符，例如：{{客户名称}}、{{评分}}。
"""


def extract_latest_tool_args(messages: List[BaseMessage]) -> dict:
    """
    从消息历史中找到最近一次工具调用参数。

    当 info 节点中的 LLM 判断信息完整时，会调用 PromptInstructions。
    这个函数负责把工具调用里的 args 取出来，供 prompt 节点使用。
    """
    latest_args = None

    for msg in messages:
        if isinstance(msg, AIMessage) and msg.tool_calls:
            latest_args = msg.tool_calls[0].get("args", None)

    if latest_args is None:
        raise ValueError("没有找到工具调用参数，无法生成 Prompt。")

    return latest_args


def get_prompt_messages(messages: List[BaseMessage]) -> List[BaseMessage]:
    """
    为 prompt 节点构造新的消息列表。

    这里有一个关键点：
    生成 Prompt 时，不应该直接把包含 tool_calls 的 AIMessage 原样传给模型，
    否则有些模型接口会要求后面必须跟 ToolMessage。

    所以这里会：
    1. 提取工具调用参数；
    2. 跳过 ToolMessage；
    3. 跳过带 tool_calls 的 AIMessage；
    4. 保留普通 HumanMessage / AIMessage 作为上下文。
    """
    tool_args = extract_latest_tool_args(messages)

    cleaned_history: List[BaseMessage] = []

    for msg in messages:
        if isinstance(msg, ToolMessage):
            continue

        if isinstance(msg, AIMessage) and msg.tool_calls:
            continue

        cleaned_history.append(msg)

    system_message = SystemMessage(
        content=prompt_system.format(reqs=tool_args)
    )

    return [system_message] + cleaned_history


def prompt_node(state: MessagesState):
    """
    prompt 节点：根据已经收集好的结构化需求生成最终 Prompt。
    """
    messages = get_prompt_messages(state["messages"])
    response = llm.invoke(messages)
    return {"messages": [response]}


# ============================================================
# 5. 定义状态判断逻辑
# ============================================================

def get_state(state: MessagesState) -> Literal["add_tool_message", "info", "__end__"]:
    """
    根据最后一条消息判断下一步走向。

    1. 如果最后一条是 AIMessage，并且包含 tool_calls：
       说明 LLM 已经收集完需求，准备生成 Prompt。
       下一步进入 add_tool_message。

    2. 如果最后一条不是 HumanMessage：
       说明当前轮 AI 已经回答完了，需要等待下一次用户输入。
       所以本轮图执行结束。

    3. 如果最后一条是 HumanMessage：
       说明用户刚输入内容，进入 info 节点继续处理。
    """
    messages = state["messages"]
    last_message = messages[-1]

    if isinstance(last_message, AIMessage) and last_message.tool_calls:
        return "add_tool_message"

    elif not isinstance(last_message, HumanMessage):
        return "__end__"

    return "info"


# ============================================================
# 6. 定义工具消息节点
# ============================================================

def add_tool_message_node(state: MessagesState):
    """
    当 AIMessage 里出现 tool_calls 后，需要补一条对应的 ToolMessage。

    这里的 ToolMessage 相当于告诉 LangChain / LangGraph：
    工具已经执行完成，可以继续往下走。

    注意：
    这个工具本身不做复杂计算。
    它只是一个“状态切换信号”。
    """
    last_message = state["messages"][-1]

    if not isinstance(last_message, AIMessage) or not last_message.tool_calls:
        raise ValueError("最后一条消息没有工具调用，无法添加 ToolMessage。")

    tool_call_id = last_message.tool_calls[0]["id"]

    return {
        "messages": [
            ToolMessage(
                content="Prompt generated!",
                tool_call_id=tool_call_id,
            )
        ]
    }


# ============================================================
# 7. 创建 LangGraph 图
# ============================================================

memory = MemorySaver()

workflow = StateGraph(MessagesState)

# 添加节点
workflow.add_node("info", info_node)
workflow.add_node("add_tool_message", add_tool_message_node)
workflow.add_node("prompt", prompt_node)

# 定义入口
workflow.add_edge(START, "info")

# info 节点执行完后，根据 get_state 判断下一步
workflow.add_conditional_edges(
    "info",
    get_state,
    {
        "add_tool_message": "add_tool_message",
        "info": "info",
        "__end__": END,
    },
)

# 工具消息节点执行完后，进入 prompt 节点
workflow.add_edge("add_tool_message", "prompt")

# prompt 节点执行完后，结束
workflow.add_edge("prompt", END)

# 编译图，使用 MemorySaver 保存多轮对话状态
graph = workflow.compile(checkpointer=memory)


# ============================================================
# 8. 可选：保存图结构
# ============================================================

def save_graph_visualization():
    """
    尝试保存 LangGraph 图结构。

    有些环境 draw_mermaid_png 可能需要额外依赖或网络能力。
    如果失败，就保存 mermaid 文本。
    """
    try:
        graph_png = graph.get_graph().draw_mermaid_png()
        with open("user_prompt_chatbot.png", "wb") as f:
            f.write(graph_png)
        print("图结构已保存为 user_prompt_chatbot.png")
    except Exception as e:
        mermaid_text = graph.get_graph().draw_mermaid()
        with open("user_prompt_chatbot.mmd", "w", encoding="utf-8") as f:
            f.write(mermaid_text)
        print("PNG 图生成失败，已保存 Mermaid 文本为 user_prompt_chatbot.mmd")
        print("失败原因：", repr(e))


# ============================================================
# 9. 使用图：命令行多轮聊天
# ============================================================

def run_chatbot():
    """
    启动命令行聊天机器人。

    thread_id 的作用：
    - 同一个 thread_id 会保留上下文；
    - 不同 thread_id 表示不同会话。
    """
    config = {
        "configurable": {
            "thread_id": str(uuid.uuid4())
        }
    }

    print("需求智能收集助手已启动。")
    print("输入 q 或 Q 退出。")
    print("-" * 60)

    while True:
        user_input = input("\nUser: ")

        if user_input.strip() in {"q", "Q"}:
            print("AI: ByeBye")
            break

        last_event = None

        for event in graph.stream(
            {"messages": [HumanMessage(content=user_input)]},
            config=config,
            stream_mode="updates",
        ):
            last_event = event

            for node_name, update in event.items():
                print(f"\n========== 节点输出：{node_name} ==========")

                messages = update.get("messages", [])

                if not messages:
                    print(update)
                    continue

                last_message = messages[-1]
                last_message.pretty_print()

        if last_event and "prompt" in last_event:
            print("\nDone! 已生成最终 Prompt。")


if __name__ == "__main__":
    # 需要图结构文件时打开这一行
    # save_graph_visualization()

    run_chatbot()