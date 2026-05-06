package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.services.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint — admin dashboard calls GET /api/activities on page load
 * to populate the activity feed with the latest 20 persisted events.
 *
 * Real-time updates arrive via WebSocket at /topic/activities (pushed by
 * ActivityService.log() whenever an event is recorded).
 */
@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "*")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getRecent() {
        return ResponseEntity.ok(activityService.getRecent());
    }
}
