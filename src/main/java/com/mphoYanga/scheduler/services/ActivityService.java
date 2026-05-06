package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Activity;
import com.mphoYanga.scheduler.models.Activity.ActorType;
import com.mphoYanga.scheduler.repos.ActivityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityService {

    private final ActivityRepository       activityRepository;
    private final SimpMessagingTemplate    messagingTemplate;

    public ActivityService(ActivityRepository activityRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.activityRepository = activityRepository;
        this.messagingTemplate  = messagingTemplate;
    }

    /**
     * Persist an activity and immediately push it to all subscribed admin dashboards.
     */
    public Activity log(Long actorId, String actorName, ActorType actorType,
                        String action, String entityType, Long entityId) {

        Activity activity = new Activity(actorId, actorName, actorType, action, entityType, entityId);
        activity = activityRepository.save(activity);

        // Push to WebSocket topic so every open admin dashboard updates instantly
        messagingTemplate.convertAndSend("/topic/activities", (Object) toMap(activity));

        return activity;
    }

    /** Returns the 20 most recent activities (newest first). */
    public List<Map<String, Object>> getRecent() {
        return activityRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20))
                .stream()
                .map(this::toMap)
                .toList();
    }

    private Map<String, Object> toMap(Activity a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         a.getId());
        m.put("actorId",    a.getActorId());
        m.put("actorName",  a.getActorName());
        m.put("actorType",  a.getActorType().name());
        m.put("action",     a.getAction());
        m.put("entityType", a.getEntityType());
        m.put("entityId",   a.getEntityId());
        m.put("createdAt",  a.getCreatedAt().toString());
        return m;
    }
}
