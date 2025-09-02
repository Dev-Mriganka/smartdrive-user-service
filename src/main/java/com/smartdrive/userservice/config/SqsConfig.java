package com.smartdrive.userservice.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;

/**
 * SQS Configuration for User Service
 * Handles event-driven communication as per sequence diagram
 */
@Configuration
public class SqsConfig {

    /**
     * Configure SQS Message Listener Container Factory
     * Handles async processing of user events
     */
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory() {
        return SqsMessageListenerContainerFactory
                .builder()
                .configure(options -> options
                        .acknowledgementMode(AcknowledgementMode.ON_SUCCESS)
                        .acknowledgementOrdering(AcknowledgementOrdering.ORDERED)
                        .acknowledgementInterval(Duration.ofSeconds(3))
                        .acknowledgementThreshold(5))
                .sqsAsyncClient(SqsAsyncClient.create())
                .build();
    }
}