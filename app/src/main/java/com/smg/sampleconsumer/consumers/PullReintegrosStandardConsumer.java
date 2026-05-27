package com.smg.sampleconsumer.consumers;

import com.smg.pull.lib.MegBasicPullConsumer;
import com.smg.pull.lib.MegPullClient;
import com.smg.pull.lib.MegPullSubscription;
import com.smg.pull.lib.MegSubscriptionConfig;
import com.smg.pull.lib.MegSubscriptionConfigs;
import com.smg.pull.lib.MegValidationResult;
import com.smg.sampleconsumer.config.SampleRoutingProperties;
import com.smg.sampleconsumer.config.SampleTopicsProperties;
import com.smg.sampleconsumer.mapper.ReintegroMessageMapper;
import com.smg.sampleconsumer.model.ReintegroMessage;
import com.smg.sampleconsumer.publishers.PagosReintegrosPublisher;
import com.smg.sampleconsumer.publishers.ReintegrosLargePublisher;
import com.smg.sampleconsumer.services.ReintegrosPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@MegPullSubscription(configPrefix = "sample.topics.in.reintegros")
public class PullReintegrosStandardConsumer extends MegBasicPullConsumer<ReintegroMessage> {
    private static final Logger log = LoggerFactory.getLogger(PullReintegrosStandardConsumer.class);
    private final MegSubscriptionConfig subscriptionConfig;
    private final ReintegrosLargePublisher reintegrosLargePublisher;
    private final PagosReintegrosPublisher pagosReintegrosPublisher;
    private final ReintegroMessageMapper reintegroMessageMapper;
    private final ReintegrosPagoService reintegrosPagoService;
    private final double amountMaxStandardToPay;

    public PullReintegrosStandardConsumer(
            MegPullClient megPullClient,
            SampleTopicsProperties sampleTopicsProperties,
            SampleRoutingProperties routingProperties,
            ReintegrosLargePublisher reintegrosLargePublisher,
            PagosReintegrosPublisher pagosReintegrosPublisher,
            ReintegroMessageMapper reintegroMessageMapper,
            ReintegrosPagoService reintegrosPagoService
    ) {
        super(megPullClient);
        this.subscriptionConfig = MegSubscriptionConfigs.from(sampleTopicsProperties.getIn().getReintegros());
        this.amountMaxStandardToPay = routingProperties.getAmountMaxStandardToPay();
        this.reintegrosLargePublisher = reintegrosLargePublisher;
        this.pagosReintegrosPublisher = pagosReintegrosPublisher;
        this.reintegroMessageMapper = reintegroMessageMapper;
        this.reintegrosPagoService = reintegrosPagoService;
    }

    @SuppressWarnings("unchecked")
    public void onPullMessage(List<Map<String, Object>> messages) {
        processMessagesForSubscription((List<Map<String, Object>>) (List<?>) messages, subscriptionConfig, message -> {
            ReintegroMessage reintegro = message;
            log.info("Processed reintegro. monto={} cbu={}", reintegro.getMonto(), reintegro.getCbu());
            if (reintegro.getMonto() > amountMaxStandardToPay) {
                log.info("Redirect to reintegro large. monto={} cbu={}", reintegro.getMonto(), reintegro.getCbu());
                reintegrosLargePublisher.publish(
                        "alto-monto-route-" + reintegro.getMessageId(),
                        reintegro.getCorrelationId(),
                        "finanzas-api",
                        "solicitud.reintegro.alto-monto.detectada",
                        "sample-consumer",
                        reintegro.getPayload()
                );
            } else {
                reintegrosPagoService.pagar(reintegro);
                pagosReintegrosPublisher.publish(
                        "pago-confirmado-" + reintegro.getMessageId(),
                        reintegro.getCorrelationId(),
                        "finanzas-api",
                        "pago.reintegro.confirmado",
                        "sample-consumer",
                        reintegro.getPayload()
                );
                log.info("Published pago event to pagos-reintegros. monto={} cbu={}", reintegro.getMonto(), reintegro.getCbu());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ReintegroMessage mapMessage(Map<String, Object> message) {
        return reintegroMessageMapper.map(message);
    }

    @Override
    protected MegValidationResult validateMappedMessage(ReintegroMessage reintegro) {
        if (reintegro.getMessageId() == null || reintegro.getMessageId().isBlank()) {
            return MegValidationResult.invalid("missing messageId");
        }
        if (reintegro.getCbu() == null || reintegro.getCbu().isBlank()) {
            return MegValidationResult.invalid("blank cbu");
        }
        if (reintegro.getDu() == null || reintegro.getDu().isBlank()) {
            return MegValidationResult.invalid("blank du");
        }
        if (reintegro.getUser() == null || reintegro.getUser().isBlank()) {
            return MegValidationResult.invalid("blank user");
        }
        if (reintegro.getMonto() <= 0 || reintegro.getMonto() == -1d) {
            return MegValidationResult.invalid("invalid monto");
        }
        return MegValidationResult.ok();
    }
}
