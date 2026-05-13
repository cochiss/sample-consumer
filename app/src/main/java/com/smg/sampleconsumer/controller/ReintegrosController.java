package com.smg.sampleconsumer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Webhook PUSH: misma idea de validación que {@link com.smg.sampleconsumer.consumers.PullReintegrosStandardConsumer}
 * (header/payload/monto/cbu). HTTP 400 = el consumidor rechaza el mensaje como inválido (en el gateway,
 * 4xx → DLQ sin reintentos). HTTP 5xx = fallo transitorio (el gateway reintenta hasta el máximo).
 */
@RestController
@RequestMapping("/ws.reintegros")
public class ReintegrosController {
    private static final Logger log = LoggerFactory.getLogger(ReintegrosController.class);

    @PostMapping("/pagos")
    public Mono<ResponseEntity<Map<String, Object>>> receivePush(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
                validateMessageShape(body);
                return body;
            })
            .flatMap(
                b -> Mono.fromCallable(
                    () -> {
                        maybeThrowTransientFailure();
                        return b;
                    }
                )
            )
            .map(
                b -> {
                    log.info("PUSH webhook accepted: {}", b);
                    return ResponseEntity.accepted().body(Map.of("status", "RECEIVED", "body", b));
                }
            )
            .onErrorResume(
                IllegalArgumentException.class,
                ex -> {
                    log.warn("PUSH rejected (bad request): {}", ex.getMessage());
                    String msg = ex.getMessage() == null ? "bad request" : ex.getMessage();
                    return Mono.just(ResponseEntity.badRequest().body(Map.of("error", msg)));
                }
            );
    }

    @SuppressWarnings("unchecked")
    private void validateMessageShape(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("body required");
        }
        Object headerObj = body.get("header");
        Object payloadObj = body.get("payload");
        if (!(headerObj instanceof Map<?, ?>) || !(payloadObj instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Invalid body: missing header/payload/messageId");
        }
        Map<String, Object> header = (Map<String, Object>) headerObj;
        Map<String, Object> payload = (Map<String, Object>) payloadObj;
        if (header == null || payload == null || header.get("messageId") == null) {
            throw new IllegalArgumentException("Invalid body: missing header/payload/messageId");
        }
        Object cbuValue = payload.get("cbu");
        Object montoObj = payload.get("monto");
        if (cbuValue == null || montoObj == null) {
            throw new IllegalArgumentException("Invalid body: missing monto/cbu");
        }
        if (!(montoObj instanceof Number)) {
            throw new IllegalArgumentException("Invalid body: monto must be numeric");
        }
        Number montoValue = (Number) montoObj;
        String cbu = String.valueOf(cbuValue);
        if (cbu.isBlank()) {
            throw new IllegalArgumentException("Invalid body: blank cbu");
        }
        double monto = montoValue.doubleValue();
        if (monto <= 0 || monto == -1d) {
            throw new IllegalArgumentException("Invalid monto");
        }
    }

    /** Simula fallo transitorio (misma idea que en PULL): el gateway debería reintentar y luego DLQ si no mejora. */
    private void maybeThrowTransientFailure() {
        if (ThreadLocalRandom.current().nextInt(100) < 30) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Simulated transient processing error"
            );
        }
    }
}
