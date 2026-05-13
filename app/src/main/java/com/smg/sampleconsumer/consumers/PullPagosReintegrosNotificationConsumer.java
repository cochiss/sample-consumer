package com.smg.sampleconsumer.consumers;

import com.smg.pull.lib.MegBasicPullConsumer;
import com.smg.pull.lib.MegPullClient;
import com.smg.pull.lib.MegPullSubscription;
import com.smg.pull.lib.MegSubscriptionConfig;
import com.smg.pull.lib.MegSubscriptionConfigs;
import com.smg.pull.lib.MegValidationResult;
import com.smg.sampleconsumer.config.SampleTopicsProperties;
import com.smg.sampleconsumer.mapper.ReintegroMessageMapper;
import com.smg.sampleconsumer.model.ReintegroMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@MegPullSubscription(configPrefix = "sample.topics.in.pagos-reintegros")
public class PullPagosReintegrosNotificationConsumer extends MegBasicPullConsumer<ReintegroMessage> {
    private static final Logger log = LoggerFactory.getLogger(PullPagosReintegrosNotificationConsumer.class);
    private final MegSubscriptionConfig subscriptionConfig;
    private final ReintegroMessageMapper reintegroMessageMapper;

    public PullPagosReintegrosNotificationConsumer(
            MegPullClient megPullClient,
            SampleTopicsProperties sampleTopicsProperties,
            ReintegroMessageMapper reintegroMessageMapper
    ) {
        super(megPullClient);
        this.subscriptionConfig = MegSubscriptionConfigs.from(sampleTopicsProperties.getIn().getPagosReintegros());
        this.reintegroMessageMapper = reintegroMessageMapper;
    }

    @SuppressWarnings("unchecked")
    public void onPullMessage(List<Map<String, Object>> messages) {
        processMessagesForSubscription((List<Map<String, Object>>) (List<?>) messages, subscriptionConfig, message -> {
            ReintegroMessage pago = message;
            String du = String.valueOf(pago.getPayload().getOrDefault("du", "sin-du"));
            log.info(
                    "Simulacion envio mail cliente por reintegro pagado. messageId={} monto={} cbu={} du={} correlationId={}",
                    pago.getMessageId(),
                    pago.getMonto(),
                    pago.getCbu(),
                    du,
                    pago.getCorrelationId()
            );
        });
    }

    @Override
    protected ReintegroMessage mapMessage(Map<String, Object> message) {
        return reintegroMessageMapper.map(message);
    }

    @Override
    protected MegValidationResult validateMappedMessage(ReintegroMessage pago) {
        if (pago.getMessageId() == null || pago.getMessageId().isBlank()) {
            return MegValidationResult.invalid("missing messageId");
        }
        if (pago.getCbu() == null || pago.getCbu().isBlank()) {
            return MegValidationResult.invalid("blank cbu");
        }
        if (pago.getMonto() <= 0) {
            return MegValidationResult.invalid("invalid monto");
        }
        return MegValidationResult.ok();
    }
}
