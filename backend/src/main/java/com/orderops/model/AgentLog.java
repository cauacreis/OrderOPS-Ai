package com.orderops.model;

public class AgentLog {
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
}
