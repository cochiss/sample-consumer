# Sample Consumer — SPEC

## Objetivo

PoC cliente contra la **API HTTP del Messaging Event Gateway**: crear topics/subs, publicar, recibir PUSH en un webhook y consumir por PULL con ACK/REJECT gobernados por `MegBasicPullConsumer` (validación → REJECT; handler OK → PROCESSED; excepción en handler → sin ACK, resto del batch sigue).

**Flujo Git:** todo cambio en este repo va en rama (`feat/...`, `fix/...`) desde `main` actualizado; ver **[SPEC del workspace §6](../SPEC.md#6-flujo-git-obligatorio)**.

## Alcance

| Módulo | Rol |
|--------|-----|
| **`app`** | Spring Boot: webhook PUSH y demo PULL con `@MegPullSubscription`. [`app/SPEC.md`](app/SPEC.md). |

La aplicación declara la librería cliente **`pull-consumer-lib`** por coordenada Maven `com.smg:pull-consumer-lib`. El comportamiento de esa librería está en **[`pull-consumer-lib/SPEC.md`](../pull-consumer-lib/SPEC.md)**.

## Decisiones

- PULL por **batch** por tick.
- **ACK/REJECT** por mensaje en batch vía `pull-consumer-lib` (`mapMessage` + `validateMappedMessage` por ítem); el handler de negocio no hace ACK manual en el camino estándar.

## Implementación: Reactor y programación funcional

Dentro de **`sample-consumer` (módulo `app`)**, el código de la PoC se implementa con **Project Reactor** (`Mono`, `Flux`, operadores encadenados) y **programación funcional** (composición, manejo de errores con `onErrorResume` / `flatMap`, evitar estado mutable compartido en los flujos de negocio; controladores y servicios alineados a ese estilo).

Cualquier **cambio o feature nuevo** en este módulo debe **seguir ese patrón** para no mezclar estilos. La librería **`pull-consumer-lib`** es un artefacto aparte (proyecto hermano); su SPEC y su estilo son independientes.

- **PUSH** (webhook): `Mono<ResponseEntity<…>>` y composición reactiva en el controlador.
- Extensiones futuras (más endpoints, más suscripciones): mismo criterio.

## Integración con el gateway

- Base URL configurable (`meg.pull.client.base-url`).
- `subscriptionId` según convención `{topic}-v{version}-{nameSub}` alineada al gateway.
- Token de suscripción PULL y perfil local: [`app/README.md`](app/README.md).
- **PUSH:** el tratamiento de respuestas HTTP del webhook (4xx → DLQ sin reintento, 5xx/reintento) lo define solo el **gateway**; ver [messaging-event-gateway/SPEC.md §3.4](../messaging-event-gateway/SPEC.md#34-envio-push-webhook).

## Tests y calidad

Cada **caso de uso nuevo** en el módulo **`app`** (consumidor, publisher, servicio, webhook, ruteo) debe ir acompañado de **al menos un test unitario** que lo cubra de forma explícita. Misma política que el workspace [`../SPEC.md`](../SPEC.md) §5; el gateway documenta su batería en [`../messaging-event-gateway/SPEC.md`](../messaging-event-gateway/SPEC.md) §8.

## Convención de nomenclatura (estándar)

Para mantener consistencia en el módulo `app`, se define el siguiente patrón:

- **Consumidores PULL**: `Pull<Domain><Segment>Consumer`
  - Ejemplos: `PullReintegrosStandardConsumer`, `PullReintegrosHighValueConsumer`.
- **Controladores PUSH (webhook)**: `<Domain>Controller`
  - Ejemplo: `ReintegrosController`.
- **Handlers/routers de ruteo** (si se separan de consumers): `<Domain>RoutingService` o `<Domain>RerouteService`.
- **Servicios de dominio** (efectos de negocio tras validar el mensaje): `<Domain><Accion>Service` (ej. `ReintegrosPagoService` con `pagar(ReintegroMessage)`), invocados desde el consumer con `reintegrosPagoService.pagar(reintegro)`; ver [app/SPEC.md](app/SPEC.md) sección **Formato obligatorio de consumers PULL**.

Regla práctica:
- incluir el **dominio** (`Reintegros`) y la **intención** (`Consumer`, `Controller`, `Service`).
- evitar sufijos ambiguos como `Demo` en clases que ya quedaron como referencia estable de la PoC.

Nomenclatura recomendada para recursos del gateway consumidos por el sample:

- **Topics**: `{tipo}-{dominio}[-{segmento}]` (ej.: `solicitudes-reintegros`, `solicitudes-reintegros-alto-monto`).
- **Subscriptions**: `{accion}-{dominio}[-{segmento}]` (ej.: `pago-reintegros`, `pago-reintegros-alto-monto`).
- Formato estricto sugerido: `kebab-case` en minúsculas (`^[a-z]+(-[a-z]+)*$`).

## Mantenimiento del diagrama de secuencia

El README del sample incluye:

- una imagen renderizada: `secuency-diagram.png`
- y su fuente editable en Mermaid (bloque `mermaid`).

Regla de mantenimiento:

- Si cambia el flujo funcional de la PoC (publish, ruteo, consumers, notificaciones, ACK/REJECT), se debe actualizar el bloque Mermaid del README.
- Luego, renderizarlo en [Mermaid Live Editor](https://mermaid.live/), exportar a PNG y reemplazar el archivo `sample-consumer/secuency-diagram.png`.
- Mantener ese nombre de archivo para evitar links rotos en documentación.
