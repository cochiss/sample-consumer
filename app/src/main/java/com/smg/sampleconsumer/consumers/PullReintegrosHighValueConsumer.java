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
import com.smg.sampleconsumer.services.ReintegrosPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@MegPullSubscription(configPrefix = "sample.topics.in.reintegros-alto-monto")
public class PullReintegrosHighValueConsumer extends MegBasicPullConsumer<ReintegroMessage> {
    private static final Logger log = LoggerFactory.getLogger(PullReintegrosHighValueConsumer.class);
    private final MegSubscriptionConfig subscriptionConfig;
    private final ReintegroMessageMapper reintegroMessageMapper;
    private final ReintegrosPagoService reintegrosPagoService;
    private final PagosReintegrosPublisher pagosReintegrosPublisher;
    private final double amountMaxLargeToPay;

    public PullReintegrosHighValueConsumer(
            MegPullClient megPullClient,
            SampleTopicsProperties sampleTopicsProperties,
            SampleRoutingProperties routingProperties,
            ReintegroMessageMapper reintegroMessageMapper,
            ReintegrosPagoService reintegrosPagoService,
            PagosReintegrosPublisher pagosReintegrosPublisher
    ) {
        super(megPullClient);
        this.subscriptionConfig = MegSubscriptionConfigs.from(sampleTopicsProperties.getIn().getReintegrosAltoMonto());
        this.reintegroMessageMapper = reintegroMessageMapper;
        this.reintegrosPagoService = reintegrosPagoService;
        this.pagosReintegrosPublisher = pagosReintegrosPublisher;
        this.amountMaxLargeToPay = routingProperties.getAmountMaxLargeToPay();
    }

    @SuppressWarnings("unchecked")
    public void onPullMessage(List<Map<String, Object>> messages) {
        processMessagesForSubscription((List<Map<String, Object>>) (List<?>) messages, subscriptionConfig, message -> {
            ReintegroMessage reintegro = message;
            log.info("Processed reintegro large. monto={} cbu={}", reintegro.getMonto(), reintegro.getCbu());
            reintegrosPagoService.pagar(reintegro);
            pagosReintegrosPublisher.publish(
                    "pago-alto-monto-" + reintegro.getMessageId(),
                    reintegro.getCorrelationId(),
                    "finanzas-api",
                    "pago.reintegro.confirmado",
                    "sample-consumer",
                    reintegro.getPayload()
            );
            log.info("Published pago event to pagos-reintegros (alto monto). monto={} cbu={}", reintegro.getMonto(), reintegro.getCbu());
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
        if (reintegro.getMonto() > amountMaxLargeToPay) {
            return MegValidationResult.invalid("monto no aceptable");
        }
        return MegValidationResult.ok();
    }

}
