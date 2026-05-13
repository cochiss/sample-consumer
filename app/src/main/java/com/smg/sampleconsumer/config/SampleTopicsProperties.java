package com.smg.sampleconsumer.config;

import com.smg.pull.lib.MegInboundSubscriptionBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sample.topics")
public class SampleTopicsProperties {
    private Inbound in = new Inbound();
    private Outbound out = new Outbound();

    public Inbound getIn() {
        return in;
    }

    public void setIn(Inbound in) {
        this.in = in;
    }

    public Outbound getOut() {
        return out;
    }

    public void setOut(Outbound out) {
        this.out = out;
    }

    public static class Inbound {
        private InboundTopicConfig reintegros = new InboundTopicConfig();
        private InboundTopicConfig reintegrosAltoMonto = new InboundTopicConfig();
        private InboundTopicConfig pagosReintegros = new InboundTopicConfig();

        public InboundTopicConfig getReintegros() {
            return reintegros;
        }

        public void setReintegros(InboundTopicConfig reintegros) {
            this.reintegros = reintegros;
        }

        public InboundTopicConfig getReintegrosAltoMonto() {
            return reintegrosAltoMonto;
        }

        public void setReintegrosAltoMonto(InboundTopicConfig reintegrosAltoMonto) {
            this.reintegrosAltoMonto = reintegrosAltoMonto;
        }

        public InboundTopicConfig getPagosReintegros() {
            return pagosReintegros;
        }

        public void setPagosReintegros(InboundTopicConfig pagosReintegros) {
            this.pagosReintegros = pagosReintegros;
        }
    }

    public static class Outbound {
        private OutTopicConfig reintegrosAltoMonto = new OutTopicConfig();

        public OutTopicConfig getReintegrosAltoMonto() {
            return reintegrosAltoMonto;
        }

        public void setReintegrosAltoMonto(OutTopicConfig reintegrosAltoMonto) {
            this.reintegrosAltoMonto = reintegrosAltoMonto;
        }
    }

    public static class InboundTopicConfig implements MegInboundSubscriptionBinding {
        private String id;
        private int version;
        private SubscriptionConfig subscription = new SubscriptionConfig();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        @Override
        public String getTopicId() {
            return id;
        }

        @Override
        public int getTopicVersion() {
            return version;
        }

        @Override
        public String getSubscriptionNameSub() {
            return subscription.getNameSub();
        }

        @Override
        public String getSubscriptionToken() {
            return subscription.getToken();
        }

        public SubscriptionConfig getSubscription() {
            return subscription;
        }

        public void setSubscription(SubscriptionConfig subscription) {
            this.subscription = subscription;
        }
    }

    public static class SubscriptionConfig {
        private String nameSub;
        private String token;
        private int rows;
        private String cron;

        public String getNameSub() {
            return nameSub;
        }

        public void setNameSub(String nameSub) {
            this.nameSub = nameSub;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public int getRows() {
            return rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class OutTopicConfig {
        private String id;
        private int version;
        private String token;
        private String eventType;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }
    }
}
