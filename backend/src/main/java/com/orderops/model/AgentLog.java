package com.orderops.model;

import jakarta.persistence.*;

@Entity
@Table(name = "agent_logs")
public class AgentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long timestamp;
    private String agentName;
    private String level; // INFO, THINKING, SUCCESS, WARNING
    private String message;
    private String orderId;

    public AgentLog() {
        this.timestamp = System.currentTimeMillis();
    }

    public AgentLog(String agentName, String level, String message, String orderId) {
        this.timestamp = System.currentTimeMillis();
        this.agentName = agentName;
        this.level = level;
        this.message = message;
        this.orderId = orderId;
    }

    // Getters and Setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
