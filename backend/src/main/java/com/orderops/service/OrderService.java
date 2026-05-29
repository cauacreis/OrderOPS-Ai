package com.orderops.service;

import com.orderops.model.AgentLog;
import com.orderops.model.Order;
import com.orderops.repository.AgentLogRepository;
import com.orderops.repository.OrderRepository;
import com.orderops.websocket.OrderWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AgentLogRepository agentLogRepository;
    private final OrderWebSocketHandler webSocketHandler;
    private final DriverService driverService;
    private final AtomicInteger orderIdCounter = new AtomicInteger(100);

    public OrderService(OrderRepository orderRepository, AgentLogRepository agentLogRepository,
                        OrderWebSocketHandler webSocketHandler, DriverService driverService) {
        this.orderRepository = orderRepository;
        this.agentLogRepository = agentLogRepository;
        this.webSocketHandler = webSocketHandler;
        this.driverService = driverService;
        initializeOrderIdCounter();
    }

    private void initializeOrderIdCounter() {
        int maxId = 100;
        try {
            List<Order> existingOrders = orderRepository.findAll();
            for (Order o : existingOrders) {
                if (o.getId() != null && o.getId().startsWith("ORD-")) {
                    try {
                        int suffix = Integer.parseInt(o.getId().substring(4));
                        if (suffix > maxId) {
                            maxId = suffix;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load existing order count for ID counter initialization: " + e.getMessage());
        }
        orderIdCounter.set(maxId);
    }

    @Transactional
    public Order createOrder(Order order) {
        if (order.getId() == null) {
            order.setId("ORD-" + orderIdCounter.incrementAndGet());
        }
        Order saved = orderRepository.save(order);
        webSocketHandler.broadcast("ORDER_UPDATE", saved);
        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrder(String id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public void updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(status);
            Order saved = orderRepository.save(order);
            webSocketHandler.broadcast("ORDER_UPDATE", saved);
        }
    }

    @Transactional
    public void assignDriverToOrder(String orderId, String driverId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setDriverId(driverId);
            Order saved = orderRepository.save(order);
            webSocketHandler.broadcast("ORDER_UPDATE", saved);
        }
    }

    @Transactional
    public void addAgentLog(String agentName, String level, String message, String orderId) {
        AgentLog log = new AgentLog(agentName, level, message, orderId);
        AgentLog saved = agentLogRepository.save(log);
        webSocketHandler.broadcast("AGENT_LOG", saved);
    }

    public List<AgentLog> getAllLogs() {
        return agentLogRepository.findAll();
    }

    @Transactional
    public void reset() {
        orderRepository.deleteAll();
        agentLogRepository.deleteAll();
        orderIdCounter.set(100);
        driverService.initializeDrivers();
        addAgentLog("System", "INFO", "PoC environment reset successfully.", null);
    }
}
