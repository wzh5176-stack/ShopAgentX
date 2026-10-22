from pydantic import BaseModel, Field


class IntentRequest(BaseModel):
    question: str = Field(
        min_length=1,
        max_length=1000,
        pattern=r"\S"
    )


class SearchIntent(BaseModel):
    keyword: str = Field(max_length=50)
    maxPrice: str | None = None