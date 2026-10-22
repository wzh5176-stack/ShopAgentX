from typing import Literal

from pydantic import BaseModel, Field

from intent_schema import IntentRequest, SearchIntent


class HistoryMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(
        min_length=1,
        max_length=2000,
        pattern=r"\S"
    )


class ConversationIntentRequest(IntentRequest):
    history: list[HistoryMessage] = Field(
        default_factory=list,
        max_length=10
    )


class ConversationIntent(SearchIntent):
    clarification: str | None = Field(
        default=None,
        min_length=1,
        max_length=200,
        pattern=r"\S"
    )