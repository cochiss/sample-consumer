# Sample Consumer — app · SPEC

## Objetivo

Aplicación de prueba para validar flujos **PUSH** y **PULL** contra el gateway ([contratos del servidor](../../messaging-event-gateway/SPEC.md)).

## Patrón obligatorio: Reactor + FP

Norma del proyecto **`sample-consumer`**: ver [**Sample Consumer — SPEC**](../SPEC.md) (sección **Implementación: Reactor y programación funcional**). Resumen: todo código nuevo en **`app`** usa **Reactor** (`Mono`/`Flux`) y estilo **funcional** (cadenas de operadores, errores explícitos en el flujo, sin mezclar bloques imperativos extensos en la lógica de negocio de demo).

## PUSH
- Expone `POST /ws.reintegros/pagos` con validación alineada al PULL; **400** si el body no cumple; **500** aleatorio de demo (transitorio). El gateway interpreta **4xx** vs **5xx** según [SPEC del gateway §3.4](../../messaging-event-gateway/SPEC.md#34-envio-push-webhook).
- Caso de uso: confirmar que cada mensaje publicado llega al callback y observar DLQ en el gateway según la respuesta HTTP.

## Convención de nombres en código

- PULL: `Pull<Domain><Segment>Consumer` (ej. `PullReintegrosStandardConsumer`, `PullReintegrosHighValueConsumer`).
- PUSH webhook: `<Domain>Controller` para endpoints webhook de dominio (ej. `ReintegrosController`).
- Evitar `*Demo` en nombres de clases cuando la PoC ya se usa como base funcional.

## Formato obligatorio de consumers PULL

- Clase simple: extiende `MegBasicPullConsumer`, inyecta config tipada en constructor y evita `@Value`.
- `@MegPullSubscription` en la **clase** del consumer con `configPrefix` al bloque YAML inbound (la lib resuelve id, versión, token, cron, rows); handler por defecto `onPullMessage`.
- Handler con nombre estándar `onPullMessage(List<Map<String, Object>> messages)`.
- `MegSubscriptionConfig`: obtener con `MegSubscriptionConfigs.from(...)`; `InboundTopicConfig` implementa `MegInboundSubscriptionBinding` para no repetir adaptadores anónimos.
- Base genérica: `MegBasicPullConsumer<T : MegMessage>`.
- Transformación de mensaje: adapter explícito con `mapMessage(...)` (normalmente delega en `MegMessageMapper<T>`) hacia modelo de dominio (`ReintegroMessage`).
- Validación de rechazo de negocio: `validateMappedMessage(T)` devolviendo `MegValidationResult` (no se valida en el mapper).
- Tras validación OK, el **basic** envía **PROCESSED** si el cuerpo del handler termina sin excepción; **REJECT** ante falla de mapping o `validateMappedMessage` inválido. Excepción en el handler → sin ACK, mensaje pendiente; el resto del batch sigue.
- Organización de packages del módulo: `com.smg.sampleconsumer.consumers` para PULL y `com.smg.sampleconsumer.publishers` para publishers.

## Formato obligatorio de publishers

- Clase simple: extiende `MegBasicPublisher` y declara `@MegPublishConfig(configPrefix = "...")`.
- Constructor mínimo: `MegPullClient` + `Environment`.
- Método público estándar para publicación: `publishMessage(String sourceMessageId, String correlationId, String user, Map<String, Object> payload)`.
- No usar `@MegPullSubscription` en publishers (esa anotación es exclusiva de consumers PULL).

## PULL
- Usa `pull-consumer-lib` via anotacion `@MegPullSubscription`.
- Construye `subscriptionId` deterministico: `{topicId}-v{version}-{nameSub}`.
- Ejecuta pull por cron y entrega un batch completo (`List<Map<String,Object>>`) al handler.
- ACK **PROCESSED** / validación→**REJECT** los centraliza `MegBasicPullConsumer` al procesar cada ítem del batch (ver SPEC de `pull-consumer-lib`).
- Si `monto > 10000` (configurable), publica el mismo payload al topic de salida `solicitudes-reintegros-alto-monto` usando la librería; el PROCESSED del original lo hace `MegBasicPullConsumer` al finalizar sin excepción.
- Si `monto <= 10000` (configurable), ejecuta `pagar(...)` y publica evento de pago en topic de salida `pagos-reintegros` usando la librería.
- El mismo sample incluye un segundo consumidor PULL para `solicitudes-reintegros-alto-monto`.
- El mismo sample incluye un consumidor PULL para `pagos-reintegros` que simula envio de mail de notificacion por reintegro pagado (solo log).
- Reglas de negocio del demo:
  - `mapMessage` falla o `validateMappedMessage` inválido => REJECT automático por ítem
  - handler sin excepción => PROCESSED automático por ítem
  - excepción en handler => sin ACK para ese mensaje (pending) y el batch continúa

## Tests

Cada **caso de uso nuevo** en este módulo debe incluir **al menos un test unitario** que lo ejercite de forma explícita. Ver [`../SPEC.md`](../SPEC.md) (sección **Tests y calidad**) y el criterio global en [`../../SPEC.md`](../../SPEC.md) §5.

## Configuracion

- Estandar de topicos:
  - `sample.topics.in.*`: topicos/subscriptions de entrada (consumo PULL).
  - `sample.topics.out.*`: topicos de salida (publicacion).
  - incluye `sample.topics.out.pagos-reintegros` para evento de pago confirmado.
  - en `sample.topics.out.*.token` se configura el token de publicacion del topic de salida (`X-Topic-Token`).
- `SAMPLE_SUB_TOKEN`: token PULL para `sample.topics.in.reintegros.subscription.token`.
- `SAMPLE_SUB_TOKEN_ALTO_MONTO`: token PULL para `sample.topics.in.reintegros-alto-monto.subscription.token`.
- `SAMPLE_SUB_TOKEN_PAGOS`: token PULL para `sample.topics.in.pagos-reintegros.subscription.token`.
- `sample.routing.amount-max-standard-to-pay`: umbral de monto para rutear a `solicitudes-reintegros-alto-monto`.
- `sample.routing.amount-max-large-to-pay`: límite máximo aceptado en el consumer de `solicitudes-reintegros-alto-monto` (si se supera, REJECT por negocio).
- `meg.pull.client.base-url`: URL del gateway (default `http://localhost:8080`).
- Puerto local de esta app: `8181`.

## Estandar de nomenclatura (topics y subscriptions)

Contrato recomendado para evitar nombres libres:

- Formato obligatorio: `kebab-case` en minusculas, solo letras y guion medio.
- Regex recomendada: `^[a-z]+(-[a-z]+)*$`.
- Evitar: mayusculas, underscores, espacios, sufijos de version (`-v1`) y nombres de app dentro del topic.

Topics (evento/dominio):

- Patron: `{tipo}-{dominio}[-{segmento}]`.
- Ejemplo base: `solicitudes-reintegros`.
- Ejemplo segmentado: `solicitudes-reintegros-alto-monto`.

Subscriptions (proposito de consumo):

- Patron: `{accion}-{dominio}[-{segmento}]`.
- Ejemplo base: `pago-reintegros`.
- Ejemplo segmentado: `pago-reintegros-alto-monto`.

## Niveles de log (estándar del módulo)

- `ERROR`: errores no recuperables del proceso (arranque/configuración crítica).
- `WARN`: mensajes rechazados por validación o excepciones en handler (mensaje pending, sin ACK).
- `INFO`: eventos funcionales importantes y resúmenes operativos (p. ej. enrutado a topic high-value).
- `DEBUG`: detalle por mensaje en flujo exitoso (camino feliz).
- `TRACE`: trazas de diagnóstico de muy bajo nivel; no usar por defecto.

