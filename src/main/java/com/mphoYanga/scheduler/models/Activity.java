package com.mphoYanga.scheduler.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActorType actorType;

    @Column(nullable = false)
    private String action;

    private String entityType;

    private Long entityId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() { this.createdAt = LocalDateTime.now(); }

    public enum ActorType { CLIENT, ADMIN }

    public Activity() {}

    public Activity(Long actorId, String actorName, ActorType actorType,
                    String action, String entityType, Long entityId) {
        this.actorId    = actorId;
        this.actorName  = actorName;
        this.actorType  = actorType;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
    }

    public Long getId()           { return id; }
    public Long getActorId()      { return actorId; }
    public String getActorName()  { return actorName; }
    public ActorType getActorType() { return actorType; }
    public String getAction()     { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId()     { return entityId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
