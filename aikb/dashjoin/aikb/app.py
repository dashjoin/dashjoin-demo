import logging
from fastapi import FastAPI
from private_gpt.settings.settings import Settings
from private_gpt.ui.ui import PrivateGptUi
from dashjoin.aikb.app_router import app_router

logger = logging.getLogger(__name__)

def initializeApp(app: FastAPI, settings: Settings, ui: PrivateGptUi):
    logger.info("Dashjoin App - initializing")
    app.include_router(app_router)