from fastapi import FastAPI
from langserve import add_routes
from langchain_core.runnables import RunnableLambda
from pydantic import BaseModel


class QuestionInput(BaseModel):
    question: str


class AnswerOutput(BaseModel):
    answer: str


def reply(data):
    question = data["question"] if isinstance(data, dict) else data.question
    return {
        "answer": f"LangServe 已收到你的问题：{question}"
    }


chain = RunnableLambda(reply).with_types(
    input_type=QuestionInput,
    output_type=AnswerOutput,
)

app = FastAPI(
    title="LangServe Demo",
    version="1.0",
)

add_routes(app, chain, path="/chat")