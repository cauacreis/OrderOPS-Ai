package com.orderops.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    private String id;
    private String name;
    private String status; // AVAILABLE, DELIVERING, OFFLINE
    private double latitude;
    private double longitude;
    private String currentOrderId;
    private String vehicleType; // BIKE, MOTORCYCLE, CAR

    public Driver() {}

    public Driver(String id, String name, String status, double latitude, double longitude, String vehicleType) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.vehicleType = vehicleType;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getCurrentOrderId() { return currentOrderId; }
    public void setCurrentOrderId(String currentOrderId) { this.currentOrderId = currentOrderId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
}
