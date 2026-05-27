package com.smg.sampleconsumer.mapper;

import com.smg.sampleconsumer.model.ReintegroMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReintegroMessageMapperTest {

    private final ReintegroMessageMapper mapper = new ReintegroMessageMapper();

    @Test
    void map_readsUserFromHeaderAsMegPullReturns() {
        Map<String, Object> message = Map.of(
                "header", Map.of(
                        "messageId", "msg_6e5e9f28",
                        "user", "cochis",
                        "correlationId", "corr-003",
                        "sourceApp", "finanzas-api",
                        "eventType", "reintegro.solicitado"
                ),
                "payload", Map.of(
                        "monto", 50000,
                        "cbu", "0110567620056701234560",
                        "du", "29345928"
                )
        );

        ReintegroMessage mapped = mapper.map(message);

        assertEquals("cochis", mapped.getUser());
        assertEquals("msg_6e5e9f28", mapped.getMessageId());
        assertEquals(50000d, mapped.getMonto());
    }

    @Test
    void map_fallsBackToRootUserWhenPresent() {
        Map<String, Object> message = Map.of(
                "header", Map.of("messageId", "msg_1", "correlationId", "c1"),
                "user", "legacy-root",
                "payload", Map.of("monto", 100, "cbu", "0110567620056701234560", "du", "29345928")
        );

        ReintegroMessage mapped = mapper.map(message);

        assertEquals("legacy-root", mapped.getUser());
        assertNotNull(mapped.getCbu());
    }
}
