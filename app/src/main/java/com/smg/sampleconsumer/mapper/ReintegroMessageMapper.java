package com.smg.sampleconsumer.mapper;

import com.smg.pull.lib.mapper.MegMessageMapper;
import com.smg.sampleconsumer.model.ReintegroMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReintegroMessageMapper implements MegMessageMapper<ReintegroMessage> {
    @Override
    @SuppressWarnings("unchecked")
    public ReintegroMessage map(Map<String, Object> message) {
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        String messageId = headerValue(message, "messageId");
        String correlationId = headerValue(message, "correlationId");
        if (correlationId == null || correlationId.isBlank()) correlationId = "corr-" + messageId;
        String user = asText(message.get("user"));
        if (user == null || user.isBlank()) user = "sample-consumer";
        String cbu = asText(payload.get("cbu"));
        double monto = asNumber(payload.get("monto")).doubleValue();
        return new ReintegroMessage(messageId, correlationId, user, payload, cbu, monto);
    }

    @SuppressWarnings("unchecked")
    private String headerValue(Map<String, Object> message, String key) {
        Object headerObj = message.get("header");
        if (!(headerObj instanceof Map<?, ?> header)) return null;
        Object value = header.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Number asNumber(Object value) {
        return value instanceof Number ? (Number) value : null;
    }
}
