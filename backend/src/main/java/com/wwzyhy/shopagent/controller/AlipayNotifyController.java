package com.wwzyhy.shopagent.controller;

import com.wwzyhy.shopagent.service.AlipayNotifyService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "shopagent.alipay.sandbox.enabled", havingValue = "true")
public class AlipayNotifyController {
    private static final Logger log = LoggerFactory.getLogger(AlipayNotifyController.class);
    private final AlipayNotifyService service;
    public AlipayNotifyController(AlipayNotifyService service) { this.service = service; }

    @PostMapping(value = "/api/payments/alipay/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> notify(HttpServletRequest request) {
        try {
            if (request.getContentType() == null
                    || !MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(MediaType.parseMediaType(request.getContentType()))) {
                return reply(415, "failure");
            }
            if (request.getQueryString() != null) { return reply(400, "failure"); }
            byte[] bytes = request.getInputStream().readNBytes(65537);
            if (bytes.length > 65536) { return reply(413, "failure"); }
            Map<String, String> values = new LinkedHashMap<>();
            for (String pair : new String(bytes, StandardCharsets.UTF_8).split("&", -1)) {
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) { throw new IllegalArgumentException(); }
                String name = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                if (!name.matches("[a-zA-Z0-9_]{1,64}") || value.length() > 8192
                        || values.putIfAbsent(name, value) != null || values.size() > 64) {
                    throw new IllegalArgumentException();
                }
            }
            service.handle(values);
            // Only returned after the settlement transaction commits; never JSON or a redirect.
            return reply(200, "success");
        } catch (IllegalArgumentException exception) {
            return reply(400, "failure");
        } catch (IOException | RuntimeException exception) {
            log.warn("支付通知尚未确认，reason={}", exception.getClass().getSimpleName());
            return reply(503, "failure");
        }
    }

    private ResponseEntity<String> reply(int code, String body) {
        return ResponseEntity.status(code).contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header("Cache-Control", "no-store").body(body);
    }
}
