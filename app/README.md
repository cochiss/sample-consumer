# Sample Consumer — app

Módulo **Java + Spring Boot** del sample: **PUSH** (webhook) y **PULL** con [`pull-consumer-lib`](../../pull-consumer-lib/README.md).

- [`SPEC.md`](SPEC.md)
- Endpoints y reglas del servidor: [`../../messaging-event-gateway/SPEC.md`](../../messaging-event-gateway/SPEC.md)

## Ejecutar

```bash
# desde sample-consumer/
mvn spring-boot:run
```

Puerto: **8181**.

Tokens PULL: `sample.topics.in.reintegros.subscription.token`, `sample.topics.in.reintegros-alto-monto.subscription.token` y `sample.topics.in.pagos-reintegros.subscription.token` en `src/main/resources/application.yml` (o por variables de entorno).

Estandar de configuracion en `application.yml`:
- `sample.topics.in.*` para topics/subscriptions de entrada (consumo PULL).
- `sample.topics.out.*` para topics de salida (publicacion).

Nomenclatura recomendada para nuevos nombres:
- Topics: `{tipo}-{dominio}[-{segmento}]` (ej.: `solicitudes-reintegros`, `solicitudes-reintegros-alto-monto`).
- Subscriptions: `{accion}-{dominio}[-{segmento}]` (ej.: `pago-reintegros`, `pago-reintegros-alto-monto`).
- Solo `kebab-case` en minusculas (`^[a-z]+(-[a-z]+)*$`).

## Demo PUSH

El webhook valida el mismo **shape** que el PULL (`header`/`payload`, `monto`, `cbu`, `du`, etc.): si no cumple, responde **400** (en el gateway, **4xx → DLQ sin reintentos**). Un fallo **aleatorio** simulado devuelve **500** (el gateway **reintenta** y solo entonces puede ir a DLQ). Ver [SPEC del gateway §3.4](../../messaging-event-gateway/SPEC.md#34-envio-push-webhook).

1. Crear suscripción `PUSH` al webhook del sample (`/ws.reintegros/pagos` en **8181**).

- Si el **gateway corre en Docker** y el sample **en la Mac**, `urlRest` debe usar **`host.docker.internal`** (desde el contenedor, `localhost` no es tu máquina):

```bash
curl --location 'http://localhost:8080/subscriptions' \
--header 'Content-Type: application/json' \
--data '{
    "topicId": "solicitudes-reintegros",
    "topicVersion": 1,
    "nameSub": "pago-reintegros-push",
    "description": "procesador de reintegros por PUSH",
    "type": "PUSH",
    "urlRest": "http://host.docker.internal:8181/ws.reintegros/pagos",
    "maxRetries": 10
  }'
```

- Si el **gateway y el sample** corren **todos en el host** (sin contenedor del gateway), podés usar `http://localhost:8181/ws.reintegros/pagos`.

2. Publicar un mensaje:

```bash
curl --location 'http://localhost:8080/topics/solicitudes-reintegros/v1/messages' \
--header 'Idempotency-Key: evt-001' \
--header 'X-Topic-Token: TOPIC_TOKEN_DEVUELTO_EN_CREATE_TOPIC' \
--header 'X-Correlation-Id: corr-001' \
--header 'X-Source-App: finanzas-api' \
--header 'Content-Type: application/json' \
--data '{
    "user": "jdoe",
    "eventType": "reintegro.solicitado",
    "payload": {
      "monto": 100,
      "cbu": "0110567620056701234560",
      "du": "29345928"
    }
  }'
```

3. Ver llegada en `POST http://localhost:8181/ws.reintegros/pagos` · breakpoint sugerido: `ReintegrosController#receivePush`.

## Demo PULL (cron) y ruteo a reintegros-alto-monto

La demo resuelve topics/subscriptions por propiedades (sin hardcode):

```yaml
sample:
  routing:
    amount-max-standard-to-pay: 10000
    amount-max-large-to-pay: 100000
  topics:
    in:
      reintegros:
        id: solicitudes-reintegros
        version: 1
        subscription:
          name-sub: pago-reintegros
          token: TOKEN_DE_LA_SUB_PARA_PULL
      reintegros-alto-monto:
        id: solicitudes-reintegros-alto-monto
        version: 1
        subscription:
          name-sub: pago-reintegros-alto-monto
          token: TOKEN_DE_LA_SUB_PARA_PULL_ALTO_MONTO
      pagos-reintegros:
        id: pagos-reintegros
        version: 1
        subscription:
          name-sub: notificacion-pago-reintegros
          token: TOKEN_DE_LA_SUB_PARA_PULL_PAGOS
    out:
      pagos-reintegros:
        id: pagos-reintegros
        version: 1
        token: TOPIC_TOKEN_PAGOS_REINTEGROS
        event-type: pago.reintegro.confirmado
      reintegros-alto-monto:
        id: solicitudes-reintegros-alto-monto
        version: 1
        token: TOPIC_TOKEN_ALTO_MONTO
        event-type: reintegro.alto-monto.detectado
```

Opcional por env:

```yaml
sample:
  topics:
    in:
      reintegros:
        subscription:
          token: ${SAMPLE_SUB_TOKEN:REPLACE_WITH_SUB_TOKEN}
      reintegros-alto-monto:
        subscription:
          token: ${SAMPLE_SUB_TOKEN_ALTO_MONTO:REPLACE_WITH_SUB_TOKEN_ALTO_MONTO}
      pagos-reintegros:
        subscription:
          token: ${SAMPLE_SUB_TOKEN_PAGOS:REPLACE_WITH_SUB_TOKEN_PAGOS}
```

Forma recomendada de uso de la librería (PULL):

```java
@Component
@MegPullSubscription(configPrefix = "sample.topics.in.reintegros")
public class PullReintegrosStandardConsumer extends MegBasicPullConsumer<ReintegroMessage> {
    private final MegSubscriptionConfig subscriptionConfig;

    public PullReintegrosStandardConsumer(..., SampleTopicsProperties props, ...) {
        this.subscriptionConfig = MegSubscriptionConfigs.from(props.getIn().getReintegros());
    }
}
```

Forma recomendada de uso de la librería (Publisher):

```java
@Component
@MegPublishConfig(configPrefix = "sample.topics.out.pagos-reintegros")
public class PagosReintegrosPublisher extends MegBasicPublisher {
    public Map<String, Object> publish(
            String idempotencyKey,
            String correlationId,
            String user,
            String eventType,
            String sourceApp,
            Map<String, Object> payload
    ) {
        return super.publishUsingTopicConfig(idempotencyKey, correlationId, user, eventType, sourceApp, payload, 1);
    }
}

// En el consumer (ejemplo):
pagosReintegrosPublisher.publish(
    "pago-confirmado-" + reintegro.getMessageId(),
    reintegro.getCorrelationId(),
    "finanzas-api",
    "pago.reintegro.confirmado",
    "sample-consumer",
    reintegro.getPayload()
);
```

Comportamiento:

- `MegBasicPullConsumer` por cada mensaje del batch: falla de `mapMessage` o `validateMappedMessage` inválido → **REJECT**; handler sin excepción → **PROCESSED**; excepción en el handler → **sin ACK** (pendiente) y **sigue** con el resto del batch.
- Entrega por **batch** (`List<Map<String,Object>>`).
- Breakpoint: `com.smg.sampleconsumer.consumers.PullReintegrosStandardConsumer#onPullMessage`.
- Si `monto > sample.routing.amount-max-standard-to-pay` (default `10000`), el sample publica el mismo payload en `sample.topics.out.reintegros-alto-monto` usando la lib; el **PROCESSED** del original lo envia el basic al terminar el handler bien.
- Si `monto <= sample.routing.amount-max-standard-to-pay`, el sample ejecuta `pagar(...)` y publica evento de pago en `sample.topics.out.pagos-reintegros`.
- En el consumer de `alto-monto`, si `monto > sample.routing.amount-max-large-to-pay` (default `100000`), el mensaje se REJECT por validación de negocio (`monto no aceptable`).
- El sample tambien consume por PULL `sample.topics.in.reintegros-alto-monto` en `com.smg.sampleconsumer.consumers.PullReintegrosHighValueConsumer`: ejecuta `pagar(...)` y publica el evento de pago en `sample.topics.out.pagos-reintegros` (igual que el flujo estándar), para que el consumidor de notificaciones reciba altos montos ya pagados.
- El sample tambien consume por PULL `sample.topics.in.pagos-reintegros` en `com.smg.sampleconsumer.consumers.PullPagosReintegrosNotificationConsumer`, registrando log de simulacion de envio de mail al cliente.

Reglas del demo de negocio (resumen): payload inválido en mapping o validación de negocio (`validateMappedMessage`) → `REJECT`; `monto > 0` y handler OK → `PROCESSED` automático; si el publish u otra acción en el handler lanza excepción, ese mensaje queda pendiente.
