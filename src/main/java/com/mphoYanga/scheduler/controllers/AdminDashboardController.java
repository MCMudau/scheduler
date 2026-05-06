package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.QuotationStatus;
import com.mphoYanga.scheduler.repos.CalendarSlotRepository;
import com.mphoYanga.scheduler.repos.ClientRepository;
import com.mphoYanga.scheduler.repos.PreviousProjectRepository;
import com.mphoYanga.scheduler.repos.ProjectRepository;
import com.mphoYanga.scheduler.repos.QuotationRepository;
import com.mphoYanga.scheduler.models.Quotation;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final ProjectRepository        projectRepository;
    private final QuotationRepository      quotationRepository;
    private final ClientRepository         clientRepository;
    private final CalendarSlotRepository   calendarSlotRepository;
    private final PreviousProjectRepository previousProjectRepository;

    public AdminDashboardController(
            ProjectRepository projectRepository,
            QuotationRepository quotationRepository,
            ClientRepository clientRepository,
            CalendarSlotRepository calendarSlotRepository,
            PreviousProjectRepository previousProjectRepository) {
        this.projectRepository         = projectRepository;
        this.quotationRepository       = quotationRepository;
        this.clientRepository          = clientRepository;
        this.calendarSlotRepository    = calendarSlotRepository;
        this.previousProjectRepository = previousProjectRepository;
    }

    /**
     * GET /api/dashboard/stats
     * Returns all counts needed by the admin dashboard stat cards.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("activeProjects",    safeCount(() -> projectRepository.countActiveProjects()));
        stats.put("totalQuotations",   quotationRepository.count());
        stats.put("pendingQuotations", quotationRepository.countByStatus(QuotationStatus.SENT));
        stats.put("acceptedQuotations",quotationRepository.countByStatus(QuotationStatus.ACCEPTED));
        stats.put("rejectedQuotations",quotationRepository.countByStatus(QuotationStatus.REJECTED));
        stats.put("totalClients",      clientRepository.count());
        stats.put("openSlots",         calendarSlotRepository.countOpenSlots(LocalDateTime.now()));
        stats.put("completedJobs",     previousProjectRepository.count());

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/dashboard/recent-quotations
     * Returns the 5 most recent quotations for the dashboard panel.
     */
    @GetMapping("/recent-quotations")
    public ResponseEntity<?> getRecentQuotations() {
        List<Quotation> all = quotationRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = all.stream()
                .limit(5)
                .map(q -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",              q.getQuotationId());
                    m.put("quotationNumber", q.getQuotationNumber());
                    m.put("title",           q.getTitle());
                    m.put("status",          q.getStatus().name());
                    m.put("totalAmount",     q.getTotalAmount());
                    m.put("createdAt",       q.getCreatedAt());
                    if (q.getClient() != null) {
                        m.put("clientName", q.getClient().getName() + " " + q.getClient().getSurname());
                    }
                    return m;
                }).toList();
        return ResponseEntity.ok(result);
    }

    private long safeCount(java.util.function.Supplier<Number> supplier) {
        try {
            Number n = supplier.get();
            return n == null ? 0L : n.longValue();
        } catch (Exception e) {
            return 0L;
        }
    }
}
