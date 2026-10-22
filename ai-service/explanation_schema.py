from pydantic import Field

from recommendation_schema import AskRequest, CandidateProduct


class RecommendationExplainRequest(AskRequest):
    products: list[CandidateProduct] = Field(min_length=1, max_length=5)