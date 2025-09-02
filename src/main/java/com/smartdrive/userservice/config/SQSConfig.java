package com.smartdrive.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS SQS Configuration for User Service
 * Handles event-driven communication with Auth Service
 */
@Configuration
public class SQSConfig {

    @Value("${AWS_ACCESS_KEY_ID}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY}")
    private String secretAccessKey;

    @Value("${AWS_REGION:us-east-1}")
    private String region;

    @Value("${AWS_SQS_ENDPOINT:https://sqs.us-east-1.amazonaws.com}")
    private String sqsEndpoint;

    /**
     * SQS Client for AWS operations
     */
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .endpointOverride(java.net.URI.create(sqsEndpoint))
                .build();
    }

    /**
     * Queue names for different events
     */
    public static class QueueNames {
        public static final String USER_REGISTERED_QUEUE = "user-registered-queue";
        public static final String EMAIL_VERIFIED_QUEUE = "email-verified-queue";
        public static final String EMAIL_CHANGED_QUEUE = "email-changed-queue";
        
        // Dead letter queues
        public static final String USER_REGISTERED_DLQ = "user-registered-dlq";
        public static final String EMAIL_VERIFIED_DLQ = "email-verified-dlq";
        public static final String EMAIL_CHANGED_DLQ = "email-changed-dlq";
    }
}
