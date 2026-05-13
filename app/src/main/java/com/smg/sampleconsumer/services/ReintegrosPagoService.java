package com.smg.sampleconsumer.services;

import com.smg.sampleconsumer.config.SampleRoutingProperties;
import com.smg.sampleconsumer.model.ReintegroMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class ReintegrosPagoService {
    private static final Logger log = LoggerFactory.getLogger(ReintegrosPagoService.class);
    private final double amountMaxLargeToPay;

    public ReintegrosPagoService(SampleRoutingProperties routingProperties) {
        this.amountMaxLargeToPay = routingProperties.getAmountMaxLargeToPay();
    }

    public void pagar(ReintegroMessage reintegro) {
        if (reintegro.getMonto() > amountMaxLargeToPay && ThreadLocalRandom.current().nextInt(100) < 35) {
            throw new IllegalStateException("random failure in pagar for retry demo");
        }
        log.info("Pago ejecutado. messageId={} monto={}", reintegro.getMessageId(), reintegro.getMonto());
    }
}
