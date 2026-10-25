package com.wwzyhy.shopagent.service;

import com.wwzyhy.shopagent.dto.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiContractTest {
    private MockRestServiceServer server;
    private AiClient client;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("http://ai.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiClient(builder.build());
    }
    private AiAskRequest request() {
        return new AiAskRequest("推荐耳机", List.of(new AiCandidateProduct(9L, "测试耳机", "Demo", "19.90", "教学数据")));
    }
    @Test void recommendationOutsideRealCandidatesIsRejected() {
        server.expect(requestTo("http://ai.invalid/api/ask")).andRespond(withSuccess(
                "{\"data\":{\"answer\":\"推荐\",\"recommendedIds\":[999]}}", MediaType.APPLICATION_JSON));
        assertThrows(RestClientException.class, () -> client.recommend(request()));
        server.verify();
    }
    @Test void duplicateRecommendedIdsAreCollapsed() {
        server.expect(requestTo("http://ai.invalid/api/ask")).andRespond(withSuccess(
                "{\"data\":{\"answer\":\"推荐\",\"recommendedIds\":[9,9]}}", MediaType.APPLICATION_JSON));
        assertEquals(List.of(9L), client.recommend(request()).recommendedIds());
        server.verify();
    }
    @Test void modelServiceFailureIsNotSuccessfulReply() {
        server.expect(anything()).andRespond(withServiceUnavailable());
        assertThrows(RestClientException.class, () -> client.recommend(request()));
        server.verify();
    }
    @Test void doneMustMatchAlreadyStreamedText() {
        var codec = new AiStreamJsonCodec(JsonMapper.builder().build());
        assertThrows(RestClientException.class, () -> codec.readDone(
                "{\"answer\":\"另一份答案\",\"service\":\"shopagent-ai\"}", "已显示文字"));
    }
    @Test void trailingJsonCannotBeAcceptedAsOneDelta() {
        var codec = new AiStreamJsonCodec(JsonMapper.builder().build());
        assertThrows(RestClientException.class, () -> codec.readDelta("{\"content\":\"a\"} {}"));
    }
}
