import os
import requests
from requests.auth import HTTPBasicAuth
from fastapi import APIRouter, Depends, Request
from private_gpt.server.utils.auth import authenticated
from llama_index.llms.openai import OpenAI
from llama_index.core.agent import ReActAgent
from llama_index.core.tools import FunctionTool
from dashjoin.aikb.util import executeNotebook
from pydantic import BaseModel

# Expose the App's REST service
app_router = APIRouter(prefix="/app/v1", dependencies=[Depends(authenticated)])

# Simple REST API that returns an info object
@app_router.get("/info", description="App Version Info", tags=["Custom App API"])
def info() -> object:
    return {
        "name": "Custom App",
        "version": "1.0"
    }

os.environ["OPENAI_API_KEY"] = "sk-..."

llm = OpenAI(model="gpt-3.5-turbo",temperature=0)

def get_employee_by_last_name(lastname: str) -> object:
    """Looks up an employee by last name and returns their information as a JSON object"""
    return requests.post(
        "http://host.docker.internal:8080/rest/function/tool",
        json={ "lastname": lastname },
        auth=HTTPBasicAuth("admin", "djdjdj")
    )

tool = FunctionTool.from_defaults(
    get_employee_by_last_name
)

agent = ReActAgent.from_tools(
    [tool],
    llm=llm,
    verbose=True,
)

@app_router.get("/tool", description="Find employees using LLM and tool")
def complete(lastname: str) -> str:
    return str(agent.chat("what is the first name of our employee '"+lastname+"'? Only use information from local tools to generate your answer."))

class NotebookArgs(BaseModel):
    query: str
    option: bool | None = False
    timeout: int | None

@app_router.post("/notebook", name="Jupyter Notebook as a Service", description="Jupyter Notebook as Service", tags=["Custom App API"])
def notebook(args: NotebookArgs) -> object:
    os.environ["DJ_INPUT_ARGS"] = args.model_dump_json()
    return executeNotebook("app/aikb/dashjoin/aikb/notebook.ipynb")
