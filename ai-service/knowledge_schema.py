from pydantic import BaseModel, Field, StrictBool


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