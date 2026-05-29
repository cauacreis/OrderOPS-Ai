package com.orderops.controller;

import com.orderops.model.AgentLog;
import com.orderops.repository.AgentLogRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AgentLogController {

    private final AgentLogRepository agentLogRepository;

    public AgentLogController(AgentLogRepository agentLogRepository) {
        this.agentLogRepository = agentLogRepository;
    }

    @GetMapping("/logs")
    public List<AgentLog> getLogs() {
        return agentLogRepository.findTop50ByOrderByTimestampDesc();
    }
}
