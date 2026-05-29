package com.orderops.service;

import com.orderops.model.Driver;
import com.orderops.websocket.OrderWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DriverService {

    private final ConcurrentHashMap<String, Driver> drivers = new ConcurrentHashMap<>();
    private final OrderWebSocketHandler webSocketHandler;

    // Restaurant coords near Av Paulista, São Paulo
    public static final double RESTAURANT_LAT = -23.5615;
    public static final double RESTAURANT_LNG = -46.6620;

    public DriverService(OrderWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
        initializeDrivers();
    }

    public void initializeDrivers() {
        drivers.clear();
        addDriver(new Driver("DRV-01", "Carlos Motoboy", "AVAILABLE", -23.5630, -46.6640, "MOTORCYCLE"));
        addDriver(new Driver("DRV-02", "Ana Bike", "AVAILABLE", -23.5600, -46.6600, "BIKE"));
        addDriver(new Driver("DRV-03", "Marcos Flash", "AVAILABLE", -23.5650, -46.6610, "MOTORCYCLE"));
    }

    public void addDriver(Driver driver) {
        drivers.put(driver.getId(), driver);
        webSocketHandler.broadcast("DRIVER_UPDATE", driver);
    }

    public List<Driver> getAllDrivers() {
        return new ArrayList<>(drivers.values());
    }

    public Driver getDriver(String id) {
        return drivers.get(id);
    }

    public synchronized Driver findClosestAvailableDriver(double targetLat, double targetLng) {
        Driver closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Driver d : drivers.values()) {
            if ("AVAILABLE".equals(d.getStatus())) {
                double dist = calculateDistance(d.getLatitude(), d.getLongitude(), targetLat, targetLng);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = d;
                }
            }
        }
        return closest;
    }

    public void updateDriverLocation(String id, double lat, double lng) {
        Driver d = drivers.get(id);
        if (d != null) {
            d.setLatitude(lat);
            d.setLongitude(lng);
            webSocketHandler.broadcast("DRIVER_UPDATE", d);
        }
    }

    public void updateDriverStatus(String id, String status, String orderId) {
        Driver d = drivers.get(id);
        if (d != null) {
            d.setStatus(status);
            d.setCurrentOrderId(orderId);
            webSocketHandler.broadcast("DRIVER_UPDATE", d);
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Simple Euclidean distance for local coordinates is sufficient for simulation
        double dLat = lat1 - lat2;
        double dLng = lng1 - lng2;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
}
