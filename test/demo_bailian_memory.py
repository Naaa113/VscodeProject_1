from dotenv import load_dotenv
import os

from langchain_openai import ChatOpenAI
from langchain_core.chat_history import BaseChatMessageHistory, InMemoryChatMessageHistory
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.runnables.history import RunnableWithMessageHistory

load_dotenv()

# 1. Prompt 模板
# history 是历史消息插入的位置
# input 是用户当前输入
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个擅长{ability}的中文助手，请根据上下文回答用户问题。"),
    MessagesPlaceholder(variable_name="history"),
    ("human", "{input}"),
])

# 2. 使用百炼平台 API
# 中国大陆北京地域一般用：https://dashscope.aliyuncs.com/compatible-mode/v1
# 新加坡地域一般用：https://dashscope-intl.aliyuncs.com/compatible-mode/v1
model = ChatOpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    model="qwen-plus",
    temperature=0,
)

# 3. 普通 chain
runnable = prompt | model

# 4. 用内存字典保存聊天历史
store: dict[str, BaseChatMessageHistory] = {}


def get_session_history(session_id: str) -> BaseChatMessageHistory:
    """
    根据 session_id 获取对应的聊天历史。
    如果这个 session_id 第一次出现，就创建新的历史记录。
    """
    if session_id not in store:
        store[session_id] = InMemoryChatMessageHistory()

    return store[session_id]


# 5. 给 runnable 增加聊天历史管理能力
with_message_history = RunnableWithMessageHistory(
    runnable,
    get_session_history,
    input_messages_key="input",
    history_messages_key="history",
)


def ask(session_id: str, question: str):
    response = with_message_history.invoke(
        {
            "ability": "数学",
            "input": question,
        },
        config={
            "configurable": {
                "session_id": session_id
            }
        },
    )

    print("\n==============================")
    print(f"session_id: {session_id}")
    print(f"用户: {question}")
    print(f"AI: {response.content}")
    print(f"当前历史消息数量: {len(store[session_id].messages)}")


# 第一次：abc123 会话
ask("abc123", "余弦是什么意思？")

# 第二次：还是 abc123，模型应该能记住前面说过余弦
ask("abc123", "那它和正弦有什么区别？")

# 第三次：换成 def234，是新会话，模型不会知道前面聊过余弦
ask("def234", "那它和正弦有什么区别？")


print("\nabc123 的历史消息：")
for msg in store["abc123"].messages:
    print(type(msg).__name__, ":", msg.content)