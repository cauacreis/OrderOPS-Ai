package com.orderops.service;

import com.orderops.model.Driver;
import com.orderops.repository.DriverRepository;
import com.orderops.websocket.OrderWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final OrderWebSocketHandler webSocketHandler;

    // Restaurant coords near Av Paulista, São Paulo
    public static final double RESTAURANT_LAT = -23.5615;
    public static final double RESTAURANT_LNG = -46.6620;

    public DriverService(DriverRepository driverRepository, OrderWebSocketHandler webSocketHandler) {
        this.driverRepository = driverRepository;
        this.webSocketHandler = webSocketHandler;
        initializeDrivers();
    }

    @Transactional
    public void initializeDrivers() {
        driverRepository.deleteAll();
        addDriver(new Driver("DRV-01", "Carlos Motoboy", "AVAILABLE", -23.5630, -46.6640, "MOTORCYCLE"));
        addDriver(new Driver("DRV-02", "Ana Bike", "AVAILABLE", -23.5600, -46.6600, "BIKE"));
        addDriver(new Driver("DRV-03", "Marcos Flash", "AVAILABLE", -23.5650, -46.6610, "MOTORCYCLE"));
    }

    @Transactional
    public void addDriver(Driver driver) {
        Driver saved = driverRepository.save(driver);
        webSocketHandler.broadcast("DRIVER_UPDATE", saved);
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getDriver(String id) {
        return driverRepository.findById(id).orElse(null);
    }

    public synchronized Driver findClosestAvailableDriver(double targetLat, double targetLng) {
        Driver closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Driver d : driverRepository.findAll()) {
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

    @Transactional
    public void updateDriverLocation(String id, double lat, double lng) {
        Driver d = driverRepository.findById(id).orElse(null);
        if (d != null) {
            d.setLatitude(lat);
            d.setLongitude(lng);
            Driver saved = driverRepository.save(d);
            webSocketHandler.broadcast("DRIVER_UPDATE", saved);
        }
    }

    @Transactional
    public void updateDriverStatus(String id, String status, String orderId) {
        Driver d = driverRepository.findById(id).orElse(null);
        if (d != null) {
            d.setStatus(status);
            d.setCurrentOrderId(orderId);
            Driver saved = driverRepository.save(d);
            webSocketHandler.broadcast("DRIVER_UPDATE", saved);
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Simple Euclidean distance for local coordinates is sufficient for simulation
        double dLat = lat1 - lat2;
        double dLng = lng1 - lng2;
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
}
