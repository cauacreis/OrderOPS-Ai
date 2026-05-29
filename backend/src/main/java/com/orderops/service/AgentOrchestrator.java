package com.orderops.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderops.model.Driver;
import com.orderops.model.Order;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentOrchestrator {

    private final OrderService orderService;
    private final DriverService driverService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private String geminiApiKey = null;
    private double speedMultiplier = 1.0; // Dynamic simulation speed multiplier

    public AgentOrchestrator(OrderService orderService, DriverService driverService) {
        this.orderService = orderService;
        this.driverService = driverService;
        loadApiKey();
    }

    private void loadApiKey() {
        // Try reading from .env file in root directory (orderops-ai/.env)
        try {
            File envFile = new File("C:\\Users\\Cauã Felype\\.gemini\\antigravity-ide\scratch\\orderops-ai\\.env");
            if (!envFile.exists()) {
                // Fallback to current working directory
                envFile = new File(".env");
            }
            if (envFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("GEMINI_API_KEY=")) {
                            this.geminiApiKey = line.substring(line.indexOf("=") + 1).trim();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load API Key from .env file: " + e.getMessage());
        }

        // Fallback to environment variables
        if (this.geminiApiKey == null || this.geminiApiKey.isEmpty()) {
            this.geminiApiKey = System.getenv("GEMINI_API_KEY");
        }

        if (this.geminiApiKey != null && !this.geminiApiKey.isEmpty()) {
            System.out.println("GEMINI_API_KEY detected. Using real LLM triage!");
        } else {
            System.out.println("No GEMINI_API_KEY found. Falling back to high-fidelity simulated agent.");
        }
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public void processRawOrderAsync(String rawText, String platform) {
        executorService.submit(() -> {
            try {
                // Create temporary order to represent ingestion
                Order order = new Order();
                order.setRawText(rawText);
                order.setPlatform(platform);
                order = orderService.createOrder(order);

                orderService.addAgentLog("TriageAgent", "THINKING", "Ingested raw payload from " + platform + ". Starting extraction...", order.getId());
                sleep(1500);

                orderService.addAgentLog("TriageAgent", "THINKING", "Analyzing payload layout & parsing unstructured data...", order.getId());
                sleep(1000);

                Order parsedOrder = null;
                if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
                    parsedOrder = parseWithGemini(rawText, platform, order.getId());
                }

                if (parsedOrder == null) {
                    // Fallback to high-fidelity mock triage
                    parsedOrder = parseWithMockRules(rawText, platform);
                }

                // Copy parsed details to original order
                order.setCustomerName(parsedOrder.getCustomerName());
                order.setAddress(parsedOrder.getAddress());
                order.setItems(parsedOrder.getItems());
                order.setTotalPrice(parsedOrder.getTotalPrice());
                order.setPriority(parsedOrder.getPriority());
                order.setAgentNotes(parsedOrder.getAgentNotes());
                order.setPrepTimeMinutes(parsedOrder.getPrepTimeMinutes());
                order.setStatus("TRIAGED");

                orderService.createOrder(order); // Update in service
                orderService.addAgentLog("TriageAgent", "SUCCESS", "Parsed successfully. Priority: " + order.getPriority() + " | Notes: " + order.getAgentNotes(), order.getId());
                sleep(1200);

                // Transition to Cooking
                orderService.updateOrderStatus(order.getId(), "COOKING");
                orderService.addAgentLog("KitchenAgent", "INFO", "Cooking queued items: " + getItemsSummary(order), order.getId());

                // Simulated cooking duration (base: 6 seconds)
                int cookDuration = (int) (6000 / speedMultiplier);
                sleep(cookDuration);

                // Cooking complete
                orderService.updateOrderStatus(order.getId(), "READY");
                orderService.addAgentLog("KitchenAgent", "SUCCESS", "Items packaged. Order marked READY for dispatch.", order.getId());
                sleep(1200);

                // Dispatched / Route Optimizing
                orderService.addAgentLog("DispatcherAgent", "THINKING", "Scanning map coordinates for closest available driver...", order.getId());
                sleep(1000);

                // Generate random customer coordinates around restaurant
                double targetLat = DriverService.RESTAURANT_LAT + (random.nextDouble() - 0.5) * 0.015;
                double targetLng = DriverService.RESTAURANT_LNG + (random.nextDouble() - 0.5) * 0.015;

                Driver driver = driverService.findClosestAvailableDriver(targetLat, targetLng);
                if (driver == null) {
                    orderService.addAgentLog("DispatcherAgent", "WARNING", "No available drivers in area! Retrying in 3 seconds...", order.getId());
                    sleep(3000);
                    driver = driverService.findClosestAvailableDriver(targetLat, targetLng);
                }

                if (driver == null) {
                    // Force a driver availability for PoC flow
                    List<Driver> allDrivers = driverService.getAllDrivers();
                    if (!allDrivers.isEmpty()) {
                        driver = allDrivers.get(random.nextInt(allDrivers.size()));
                        driverService.updateDriverStatus(driver.getId(), "AVAILABLE", null);
                    }
                }

                if (driver != null) {
                    final Driver assignedDriver = driver;
                    orderService.addAgentLog("DispatcherAgent", "SUCCESS", "Assigned order to " + assignedDriver.getName() + " (" + assignedDriver.getVehicleType() + ")", order.getId());
                    driverService.updateDriverStatus(assignedDriver.getId(), "DELIVERING", order.getId());
                    orderService.assignDriverToOrder(order.getId(), assignedDriver.getId());
                    orderService.updateOrderStatus(order.getId(), "DELIVERING");

                    orderService.addAgentLog("RouteAgent", "THINKING", "Calculating optimal path for " + assignedDriver.getName() + " to customer address: " + order.getAddress(), order.getId());
                    sleep(1500);

                    // Simulate delivery transit with GPS tracking updates
                    simulateDeliveryTransit(assignedDriver, targetLat, targetLng, order.getId());

                    // Arrived!
                    orderService.updateOrderStatus(order.getId(), "DELIVERED");
                    driverService.updateDriverStatus(assignedDriver.getId(), "AVAILABLE", null);
                    orderService.addAgentLog("RouteAgent", "SUCCESS", "Delivered successfully! Total transit time: 10 mins (simulated)", order.getId());

                    // Sentiment Agent
                    orderService.addAgentLog("SentimentAgent", "SUCCESS", "Sent automation callback: 'Order delivered to " + order.getCustomerName() + "'", order.getId());
                } else {
                    orderService.addAgentLog("DispatcherAgent", "WARNING", "Fallback failed. Order # " + order.getId() + " stuck in queue.", order.getId());
                }

            } catch (Exception e) {
                orderService.addAgentLog("System", "WARNING", "Error processing order: " + e.getMessage(), null);
                e.printStackTrace();
            }
        });
    }

    private void simulateDeliveryTransit(Driver driver, double targetLat, double targetLng, String orderId) {
        double startLat = DriverService.RESTAURANT_LAT;
        double startLng = DriverService.RESTAURANT_LNG;

        // Move driver from restaurant to customer in N steps
        int steps = 8;
        int stepDelay = (int) (1000 / speedMultiplier);

        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / steps;
            double currentLat = startLat + (targetLat - startLat) * fraction;
            double currentLng = startLng + (targetLng - startLng) * fraction;

            driverService.updateDriverLocation(driver.getId(), currentLat, currentLng);
            if (i == 3) {
                orderService.addAgentLog("RouteAgent", "INFO", driver.getName() + " is mid-route (50% progress).", orderId);
            }
            sleep(stepDelay);
        }

        // Move driver back to restaurant (simulate return path asynchronously after a short pause)
        executorService.submit(() -> {
            sleep(2000);
            for (int i = 1; i <= steps; i++) {
                double fraction = (double) i / steps;
                double currentLat = targetLat + (DriverService.RESTAURANT_LAT - targetLat) * fraction;
                double currentLng = targetLng + (DriverService.RESTAURANT_LNG - targetLng) * fraction;
                driverService.updateDriverLocation(driver.getId(), currentLat, currentLng);
                sleep(stepDelay);
            }
        });
    }

    private Order parseWithGemini(String rawText, String platform, String tempId) {
        try {
            String prompt = "You are an automated restaurant triage system. Parse the following raw order payload from a delivery application into structured JSON.\n" +
                    "Order Text:\n\"\"\"\n" + rawText + "\n\"\"\"\n\n" +
                    "Format output strictly as a JSON object, with no markdown tags. Do NOT wrap it in ```json. Just return raw JSON. If some values are missing, guess them logically.\n" +
                    "JSON Fields:\n" +
                    "- customerName: string\n" +
                    "- address: string\n" +
                    "- items: array of objects with {name (string), quantity (number)}\n" +
                    "- totalPrice: number\n" +
                    "- priority: string (one of: LOW, MEDIUM, HIGH, CRITICAL. CRITICAL if customer complains, has severe allergies, or orders are extremely large. HIGH if they mention urgency)\n" +
                    "- agentNotes: string (short notes, summarizing allergies, custom preparation like 'no onions', or delivery instructions)\n" +
                    "- prepTimeMinutes: number (estimated kitchen time in minutes, e.g. 15)";

            String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            String requestJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String responseText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

                // Clean markdown wraps if the model returned them anyway
                responseText = responseText.replaceAll("```json", "").replaceAll("```", "").trim();

                JsonNode parsedJson = objectMapper.readTree(responseText);
                Order order = new Order();
                order.setCustomerName(parsedJson.path("customerName").asText("Cliente Desconhecido"));
                order.setAddress(parsedJson.path("address").asText("Endereço não especificado"));
                order.setTotalPrice(parsedJson.path("totalPrice").asDouble(0.0));
                order.setPriority(parsedJson.path("priority").asText("MEDIUM"));
                order.setAgentNotes(parsedJson.path("agentNotes").asText("Nenhuma instrução"));
                order.setPrepTimeMinutes(parsedJson.path("prepTimeMinutes").asInt(15));

                List<Order.OrderItem> items = new ArrayList<>();
                JsonNode itemsNode = parsedJson.path("items");
                if (itemsNode.isArray()) {
                    for (JsonNode it : itemsNode) {
                        items.add(new Order.OrderItem(it.path("name").asText(), it.path("quantity").asInt(1)));
                    }
                }
                order.setItems(items);
                return order;
            } else {
                orderService.addAgentLog("TriageAgent", "WARNING", "Gemini API returned status " + response.statusCode() + ". Falling back to regex parser.", tempId);
            }
        } catch (Exception e) {
            orderService.addAgentLog("TriageAgent", "WARNING", "Failed calling Gemini: " + e.getMessage() + ". Falling back to regex parser.", tempId);
        }
        return null;
    }

    private Order parseWithMockRules(String rawText, String platform) {
        Order order = new Order();
        order.setPlatform(platform);

        // Simple Regex Parser for Portuguese delivery messages
        String customer = "Cliente Anonimo";
        Pattern customerPat = Pattern.compile("(?i)(?:cliente|nome|mesa|para)\\s*:\\s*([^\\n]+)");
        Matcher customerMat = customerPat.matcher(rawText);
        if (customerMat.find()) {
            customer = customerMat.group(1).trim();
        } else {
            // First line could be the customer name
            String[] lines = rawText.split("\n");
            if (lines.length > 0 && !lines[0].contains(":") && lines[0].length() < 30) {
                customer = lines[0].trim();
            }
        }
        order.setCustomerName(customer);

        String address = "Retirada no Balcão";
        Pattern addressPat = Pattern.compile("(?i)(?:endereço|entrega|rua|av|avenida)\\s*:\\s*([^\\n]+)");
        Matcher addressMat = addressPat.matcher(rawText);
        if (addressMat.find()) {
            address = addressMat.group(1).trim();
        }
        order.setAddress(address);

        // Find items (e.g. "2x Hamburguer", "1 Batata", "3 Coca zero")
        List<Order.OrderItem> items = new ArrayList<>();
        Pattern itemPat = Pattern.compile("(\\d+)\\s*(?:x|\\*|-)?\\s*([a-zA-Záéíóúâêôãõç\\s]{3,25})", Pattern.CASE_INSENSITIVE);
        String[] lines = rawText.split("\n");
        double calculatedTotal = 0.0;

        for (String line : lines) {
            Matcher itemMat = itemPat.matcher(line);
            if (itemMat.find()) {
                int qty = Integer.parseInt(itemMat.group(1).trim());
                String itemName = itemMat.group(2).trim();

                // Skip keywords that might match address or customer
                if (itemName.toLowerCase().contains("rua") || itemName.toLowerCase().contains("cliente") || itemName.toLowerCase().contains("troco")) {
                    continue;
                }

                items.add(new Order.OrderItem(itemName, qty));
                calculatedTotal += qty * (15.0 + random.nextInt(30)); // Mock price
            }
        }

        if (items.isEmpty()) {
            // Fallback default item
            items.add(new Order.OrderItem("Combo Smash Burger B2B", 1));
            calculatedTotal = 39.90;
        }
        order.setItems(items);
        order.setTotalPrice(calculatedTotal);

        // Priority heuristics
        String priority = "MEDIUM";
        String notes = "Orquestrado via Heurísticas de Triage.";

        if (rawText.toLowerCase().contains("alergia") || rawText.toLowerCase().contains("sem cebola") || rawText.toLowerCase().contains("sem lactose")) {
            priority = "HIGH";
            notes = "Alerta: Restrições alimentares detectadas (Sem cebola/Alergia).";
        }
        if (rawText.toLowerCase().contains("urgente") || rawText.toLowerCase().contains("atrasado") || rawText.toLowerCase().contains("rápido")) {
            priority = "CRITICAL";
            notes = "Crítico: Cliente sinalizou urgência/atraso.";
        }
        if (calculatedTotal > 150.0) {
            priority = "HIGH";
            notes = "Pedido volumoso (" + String.format("R$ %.2f", calculatedTotal) + ") - Prioridade operacional.";
        }

        order.setPriority(priority);
        order.setAgentNotes(notes);
        order.setPrepTimeMinutes(10 + random.nextInt(15));

        return order;
    }

    private String getItemsSummary(Order order) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.getItems().size(); i++) {
            Order.OrderItem it = order.getItems().get(i);
            sb.append(it.getQuantity()).append("x ").append(it.getName());
            if (i < order.getItems().size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
