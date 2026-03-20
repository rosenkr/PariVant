package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ingest_attempt")
public class IngestAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="source", nullable = false, length = 32)
    private String source;

    @Column(name="game_type", nullable = false, length = 32)
    private String gameType;

    @Column(name="endpoint", nullable = false, length = 128)
    private String endpoint;

    @Column(name="status", nullable = false, length = 16)
    private String status;

    @Column(name="reason")
    private String reason;

    @Column(name="details")
    private String details;

    @Column(name="created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    protected IngestAttemptEntity() {}

    public IngestAttemptEntity(String source, String gameType, String endpoint, String status, String reason, String details, OffsetDateTime createdAt) {
        this.source = source;
        this.gameType = gameType;
        this.endpoint = endpoint;
        this.status = status;
        this.reason = reason;
        this.details = details;
        this.createdAt = createdAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public String getGameType() { return gameType; }
    public String getEndpoint() { return endpoint; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setSource(String source) { this.source = source; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setStatus(String status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
    public void setDetails(String details) { this.details = details; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}