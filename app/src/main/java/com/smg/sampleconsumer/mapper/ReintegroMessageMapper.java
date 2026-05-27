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
        String user = headerValue(message, "user");
        if (user == null || user.isBlank()) {
            user = asText(message.get("user"));
        }
        String cbu = asText(payload.get("cbu"));
        String du = asText(payload.get("du"));
        Number montoNumber = asNumber(payload.get("monto"));
        double monto = montoNumber == null ? 0d : montoNumber.doubleValue();
        return new ReintegroMessage(messageId, correlationId, user, payload, cbu, du, monto);
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
