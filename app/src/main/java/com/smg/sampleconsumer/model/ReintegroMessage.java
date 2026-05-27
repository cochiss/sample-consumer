package com.smg.sampleconsumer.model;

import com.smg.pull.lib.model.MegMessage;

import java.util.Map;

public class ReintegroMessage extends MegMessage {
    private final String cbu;
    private final String du;
    private final double monto;

    public ReintegroMessage(
            String messageId,
            String correlationId,
            String user,
            Map<String, Object> payload,
            String cbu,
            String du,
            double monto
    ) {
        super(messageId, correlationId, user, payload);
        this.cbu = cbu;
        this.du = du;
        this.monto = monto;
    }

    public String getCbu() {
        return cbu;
    }

    public String getDu() {
        return du;
    }

    public double getMonto() {
        return monto;
    }
}
