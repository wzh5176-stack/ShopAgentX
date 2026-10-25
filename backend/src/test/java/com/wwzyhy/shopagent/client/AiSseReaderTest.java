package com.wwzyhy.shopagent.client;

import com.wwzyhy.shopagent.dto.AiStreamEvent;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import static org.junit.jupiter.api.Assertions.*;

class AiSseReaderTest {
    private final AiSseReader reader = new AiSseReader();
    @Test void chineseDeltasAndDoneAreDeliveredInOrder() throws Exception {
        var events = new ArrayList<AiStreamEvent>();
        reader.read(input("event: delta\ndata: {\"content\":\"耳机\"}\n\nevent: done\ndata: {}\n\n"), events::add);
        assertEquals(2, events.size());
        assertEquals("delta", events.get(0).event());
        assertTrue(events.get(0).data().contains("耳机"));
        assertEquals("done", events.get(1).event());
    }
    @Test void truncatedStreamDoesNotSucceed() {
        assertThrows(RestClientException.class, () -> reader.read(input("event: delta\ndata: {}\n\n"), e -> {}));
    }
    @Test void unknownEventIsRejected() {
        assertThrows(RestClientException.class, () -> reader.read(input("event: execute\ndata: {}\n\n"), e -> {}));
    }
    @Test void oversizedEventIsRejected() {
        assertThrows(RestClientException.class, () -> reader.read(input("event: delta\ndata: " + "x".repeat(16001) + "\n\n"), e -> {}));
    }
    @Test void malformedUtf8IsRejected() {
        assertThrows(java.io.IOException.class, () -> reader.read(new ByteArrayInputStream(new byte[]{(byte) 0xc3, 0x28}), e -> {}));
    }
    private ByteArrayInputStream input(String text) { return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)); }
}
