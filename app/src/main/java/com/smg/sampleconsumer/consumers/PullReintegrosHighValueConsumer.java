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
    private final double amountMaxLargeToPay;

    public PullReintegrosHighValueConsumer(
            MegPullClient megPullClient,
            SampleTopicsProperties sampleTopicsProperties,
            SampleRoutingProperties routingProperties,
            ReintegroMessageMapper reintegroMessageMapper,
            ReintegrosPagoService reintegrosPagoService
    ) {
        super(megPullClient);
        this.subscriptionConfig = MegSubscriptionConfigs.from(sampleTopicsProperties.getIn().getReintegrosAltoMonto());
        this.reintegroMessageMapper = reintegroMessageMapper;
        this.reintegrosPagoService = reintegrosPagoService;
        this.amountMaxLargeToPay = routingProperties.getAmountMaxLargeToPay();
    }

    @SuppressWarnings("unchecked")
    public void onPullMessage(List<Map<String, Object>> messages) {
        processMessagesForSubscription((List<Map<String, Object>>) (List<?>) messages, subscriptionConfig, message -> {
            ReintegroMessage reintegro = message;
            log.info("Processed reintegro large. monto={} cbu={}", reintegro.getMonto(), reintegro.getCbu());
            reintegrosPagoService.pagar(reintegro);
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
        if (reintegro.getMonto() > amountMaxLargeToPay) {
            return MegValidationResult.invalid("monto no aceptable");
        }
        return MegValidationResult.ok();
    }

}
