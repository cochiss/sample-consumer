package com.smg.sampleconsumer.publishers;

import com.smg.pull.lib.MegBasicPublisher;
import com.smg.pull.lib.MegPublishConfig;
import com.smg.pull.lib.MegPullClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@MegPublishConfig(configPrefix = "sample.topics.out.pagos-reintegros")
public class PagosReintegrosPublisher extends MegBasicPublisher {
    public PagosReintegrosPublisher(
            MegPullClient megPullClient,
            Environment environment
    ) {
        super(megPullClient, environment);
    }

    public Map<String, Object> publishMessage(
            String sourceMessageId,
            String correlationId,
            String user,
            Map<String, Object> payload
    ) {
        return super.publishMessageFromConfig(
                sourceMessageId,
                correlationId,
                user,
                payload,
                1
        );
    }
}
