from pydantic import BaseModel, Field, StrictBool
from pydantic import ConfigDict, model_validator
from intent_schema import IntentRequest


class KnowledgeRequest(IntentRequest):
    model_config = ConfigDict(extra="forbid")
    productTitle: str | None = Field(default=None, min_length=1, max_length=200)
    productBrand: str | None = Field(default=None, min_length=1, max_length=100)

    @model_validator(mode="after")
    def validate_identity(self):
        if (self.productTitle is None) != (self.productBrand is None):
            raise ValueError("商品名称与品牌必须同时提供")
        return self


class KnowledgeDraft(BaseModel):
    answer: str = Field(min_length=1, max_length=2000, pattern=r"\S")
    answerable: StrictBool
    citedIds: list[str] = Field(max_length=3)


class KnowledgeSource(BaseModel):
    chunkId: str = Field(min_length=1, max_length=200)
    source: str = Field(min_length=1, max_length=200)
    document: str = Field(min_length=1, max_length=2000)
    section: str = Field(min_length=1, max_length=200)
    content: str = Field(min_length=1, max_length=5000)


class KnowledgeResult(BaseModel):
    answer: str = Field(min_length=1, max_length=2000, pattern=r"\S")
    answerable: StrictBool
    sources: list[KnowledgeSource] = Field(max_length=3)
