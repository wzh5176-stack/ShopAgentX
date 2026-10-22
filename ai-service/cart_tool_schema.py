from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, StrictInt

from intent_schema import IntentRequest


class CartToolProduct(BaseModel):
    model_config = ConfigDict(extra="forbid")

    title: str = Field(min_length=1, max_length=200, pattern=r"\S")
    brand: str = Field(min_length=1, max_length=100, pattern=r"\S")
    price: str = Field(pattern=r"^(0|[1-9][0-9]{0,7})\.[0-9]{2}$")


class CartToolIntentRequest(IntentRequest):
    model_config = ConfigDict(extra="forbid")

    products: list[CartToolProduct] = Field(min_length=1, max_length=5)


class CartToolIntent(BaseModel):
    model_config = ConfigDict(extra="forbid")

    action: Literal["set_quantity", "clarify", "none"]
    productIndex: StrictInt | None = Field(ge=1, le=5)
    quantity: StrictInt | None = Field(ge=1, le=99)