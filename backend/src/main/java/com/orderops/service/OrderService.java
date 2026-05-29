package com.orderops.service;

import com.orderops.model.AgentLog;
import com.orderops.model.Order;
import com.orderops.websocket.OrderWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {

    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private final List<AgentLog> agentLogs = new CopyOnWriteArrayList<>();
    private final AtomicInteger orderIdCounter = new AtomicInteger(100);
    private final OrderWebSocketHandler webSocketHandler;
    private final DriverService driverService;

    public OrderService(OrderWebSocketHandler webSocketHandler, DriverService driverService) {
        this.webSocketHandler = webSocketHandler;
        this.driverService = driverService;
    }

    public Order createOrder(Order order) {
        if (order.getId() == null) {
            order.setId("ORD-" + orderIdCounter.incrementAndGet());
        }
        orders.put(order.getId(), order);
        webSocketHandler.broadcast("ORDER_UPDATE", order);
        return order;
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public Order getOrder(String id) {
        return orders.get(id);
    }

    public void updateOrderStatus(String orderId, String status) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setStatus(status);
            orders.put(orderId, order);
            webSocketHandler.broadcast("ORDER_UPDATE", order);
        }
    }

    public void assignDriverToOrder(String orderId, String driverId) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setDriverId(driverId);
            webSocketHandler.broadcast("ORDER_UPDATE", order);
        }
    }

    public void addAgentLog(String agentName, String level, String message, String orderId) {
        AgentLog log = new AgentLog(agentName, level, message, orderId);
        agentLogs.add(log);
        webSocketHandler.broadcast("AGENT_LOG", log);
    }

    public List<AgentLog> getAllLogs() {
        return new ArrayList<>(agentLogs);
    }

    public void reset() {
        orders.clear();
        agentLogs.clear();
        orderIdCounter.set(100);
        driverService.initializeDrivers();
        addAgentLog("System", "INFO", "PoC environment reset successfully.", null);
    }
}
