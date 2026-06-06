package com.github.swim_developer.dnotam.provider.infrastructure.out.persistence.entity;

import com.github.swim_developer.framework.domain.model.EventStatus;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "dnotam_events", indexes = {
        @Index(name = "idx_event_scenario", columnList = "eventScenario"),
        @Index(name = "idx_airport_heliport", columnList = "airportHeliport"),
        @Index(name = "idx_airspace", columnList = "airspace"),
        @Index(name = "idx_valid_from", columnList = "validFrom"),
        @Index(name = "idx_valid_to", columnList = "validTo"),
        @Index(name = "idx_provider", columnList = "provider"),
        @Index(name = "idx_received_at", columnList = "receivedAt"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnotamEventJpaEntity {

    @Id
    @Column(length = 100)
    private String eventId;

    @Column(length = 50)
    private String eventScenario;

    @Column(length = 10)
    private String airportHeliport;

    @Column(length = 10)
    private String airspace;

    @Column(length = 5)
    private String eventSeries;

    @Column(length = 100)
    private String publisher;

    @Column(length = 100)
    private String provider;

    private Instant validFrom;

    private Instant validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private EventStatus status = EventStatus.RECEIVED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();

    private Instant processedAt;

    @Builder.Default
    private int deliveredCount = 0;

    @Builder.Default
    private int retryCount = 0;

    @Column(columnDefinition = "XML", nullable = false)
    @JdbcTypeCode(SqlTypes.SQLXML)
    private String aixmMessage;
}
