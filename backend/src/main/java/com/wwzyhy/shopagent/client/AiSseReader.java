package com.wwzyhy.shopagent.client;

import com.wwzyhy.shopagent.dto.AiStreamEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.springframework.web.client.RestClientException;

public class AiSseReader {

    public void read(
            InputStream input,
            Consumer<AiStreamEvent> onEvent) throws IOException {

        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, decoder))) {

            String event = null;
            StringBuilder data = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.length() > 16000) {
                    throw new RestClientException("Python事件行过长");
                }

                if (line.isEmpty()) {
                    if (data.length() > 0) {
                        if (!"delta".equals(event)
                                && !"done".equals(event)
                                && !"error".equals(event)) {
                            throw new RestClientException("Python事件类型不符合约定");
                        }

                        String body = data.substring(0, data.length() - 1);
                        onEvent.accept(new AiStreamEvent(event, body));

                        if ("done".equals(event) || "error".equals(event)) {
                            return;
                        }
                    }

                    event = null;
                    data.setLength(0);
                } else if (line.startsWith("event:")) {
                    event = line.substring(6).strip();
                } else if (line.startsWith("data:")) {
                    String value = line.substring(5);
                    if (value.startsWith(" ")) {
                        value = value.substring(1);
                    }
                    data.append(value).append('\n');
                    if (data.length() > 16000) {
                        throw new RestClientException("Python事件数据过长");
                    }
                }
            }
        }

        throw new RestClientException("Python流在结束事件前中断");
    }
}