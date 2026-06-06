package com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.entity;

import com.github.swim_developer.framework.domain.model.QualityOfService;
import com.github.swim_developer.framework.domain.model.SubscriptionStatus;
import com.github.swim_developer.framework.provider.infrastructure.out.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscription_topic", columnList = "topic"),
        @Index(name = "idx_subscription_status", columnList = "status"),
        @Index(name = "idx_subscription_user", columnList = "userId"),
        @Index(name = "idx_subscription_topic_status", columnList = "topic, status"),
        @Index(name = "idx_subscription_hash", columnList = "subscriptionHash", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionJpaEntity {

    @Id
    private UUID subscriptionId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> eventScenario = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> airportHeliport = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> airspace = new ArrayList<>();

    @Column(length = 5)
    private String eventSeries;

    @Column(length = 100)
    private String publisher;

    @Column(length = 100)
    private String provider;

    @Column(nullable = false, unique = true, length = 255)
    private String queue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PAUSED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QualityOfService qos = QualityOfService.AT_LEAST_ONCE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean durable = true;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, unique = true, length = 64)
    private String subscriptionHash;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "subscription_end", nullable = false)
    private Instant subscriptionEnd;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String comment;
}
