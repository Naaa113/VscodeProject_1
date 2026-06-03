import asyncio
import os
from typing import Literal

from dotenv import load_dotenv
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent


load_dotenv()


@tool
def get_weather(city: Literal["北京", "上海", "深圳", "旧金山"]) -> str:
    """
    查询指定城市的天气。

    参数:
        city: 城市名称，只能是 北京、上海、深圳、旧金山。

    返回:
        天气描述字符串。
    """
    weather_map = {
        "北京": "北京今天晴，气温 3 到 12 摄氏度。",
        "上海": "上海今天多云，气温 8 到 15 摄氏度。",
        "深圳": "深圳今天小雨，气温 18 到 23 摄氏度。",
        "旧金山": "旧金山今天晴朗，气温 10 到 18 摄氏度。",
    }

    return weather_map[city]


def build_graph():
    """
    创建 LangGraph React Agent。

    这个 Agent 内部大致有两个节点：
    1. agent 节点：调用大模型，判断是否需要工具
    2. tools 节点：真正执行 Python 工具函数
    """

    model = ChatOpenAI(
        model=os.getenv("DASHSCOPE_MODEL", "qwen-plus"),
        api_key=os.getenv("DASHSCOPE_API_KEY"),
        base_url=os.getenv(
            "DASHSCOPE_BASE_URL",
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
        ),
        temperature=0,
        streaming=True,
    )

    tools = [get_weather]

    graph = create_react_agent(
        model=model,
        tools=tools,
    )

    return graph


async def run_values_demo(graph):
    """
    values 模式：
    每次返回完整状态。
    适合调试完整 messages。
    """

    print("\n\n==============================")
    print("stream_mode='values'")
    print("==============================")

    inputs = {
        "messages": [
            ("human", "帮我查一下深圳今天的天气")
        ]
    }

    step = 1

    async for chunk in graph.astream(inputs, stream_mode="values"):
        print(f"\n------ 第 {step} 次流式返回 ------")

        # values 模式返回的是完整状态
        # chunk 的典型结构：
        # {
        #     "messages": [
        #         HumanMessage(...),
        #         AIMessage(...),
        #         ToolMessage(...),
        #         AIMessage(...)
        #     ]
        # }

        messages = chunk["messages"]

        print(f"当前 messages 总数: {len(messages)}")
        print("当前最后一条消息:")

        messages[-1].pretty_print()

        step += 1


async def run_updates_demo(graph):
    """
    updates 模式：
    每次只返回某个节点产生的增量更新。
    适合展示 Agent 执行过程。
    """

    print("\n\n==============================")
    print("stream_mode='updates'")
    print("==============================")

    inputs = {
        "messages": [
            ("human", "帮我查一下深圳今天的天气")
        ]
    }

    step = 1

    async for chunk in graph.astream(inputs, stream_mode="updates"):
        print(f"\n------ 第 {step} 次流式返回 ------")

        # updates 模式返回的是节点增量
        # chunk 的典型结构：
        # {
        #     "agent": {"messages": [AIMessage(...)]}
        # }
        #
        # 或者：
        # {
        #     "tools": {"messages": [ToolMessage(...)]}
        # }

        for node_name, node_update in chunk.items():
            print(f"发生更新的节点: {node_name}")

            messages = node_update.get("messages", [])

            for message in messages:
                message.pretty_print()

        step += 1


async def run_messages_demo(graph):
    """
    messages 模式：
    流式输出大模型 token。
    适合做聊天界面的逐字输出。
    """

    print("\n\n==============================")
    print("stream_mode='messages'")
    print("==============================")

    inputs = {
        "messages": [
            ("human", "帮我查一下深圳今天的天气")
        ]
    }

    async for token, metadata in graph.astream(inputs, stream_mode="messages"):
        # token 是模型流式返回的消息片段
        # metadata 里通常包含当前节点信息
        if token.content:
            print(token.content, end="", flush=True)

    print()


async def main():
    graph = build_graph()

    await run_values_demo(graph)
    await run_updates_demo(graph)
    await run_messages_demo(graph)


if __name__ == "__main__":
    asyncio.run(main())