package com.smg.sampleconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sample.routing")
public class SampleRoutingProperties {
    private double amountMaxStandardToPay;
    private double amountMaxLargeToPay;

    public double getAmountMaxStandardToPay() {
        return amountMaxStandardToPay;
    }

    public void setAmountMaxStandardToPay(double amountMaxStandardToPay) {
        this.amountMaxStandardToPay = amountMaxStandardToPay;
    }

    public double getAmountMaxLargeToPay() {
        return amountMaxLargeToPay;
    }

    public void setAmountMaxLargeToPay(double amountMaxLargeToPay) {
        this.amountMaxLargeToPay = amountMaxLargeToPay;
    }
}
