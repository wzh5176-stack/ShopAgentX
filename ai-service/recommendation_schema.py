from pydantic import BaseModel, Field, StrictInt

from intent_schema import IntentRequest, SearchIntent


class CandidateProduct(BaseModel):
    id: StrictInt = Field(gt=0)
    title: str = Field(min_length=1, max_length=200, pattern=r"\S")
    brand: str = Field(min_length=1, max_length=100, pattern=r"\S")
    price: str = Field(pattern=r"^(0|[1-9][0-9]{0,7})\.[0-9]{2}$")
    description: str = Field(max_length=10000)


class AskRequest(IntentRequest):
    products: list[CandidateProduct] = Field(
        min_length=1,
        max_length=20
    )
    searchIntent: SearchIntent | None = None


class RecommendationResult(BaseModel):
    answer: str = Field(
        min_length=1,
        max_length=2000,
        pattern=r"\S"
    )
    recommendedIds: list[StrictInt] = Field(max_length=5)