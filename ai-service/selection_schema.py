from pydantic import BaseModel, Field, StrictInt


class ProductSelection(BaseModel):
    recommendedIds: list[StrictInt] = Field(max_length=5)