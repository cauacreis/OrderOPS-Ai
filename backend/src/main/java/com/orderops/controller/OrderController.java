package com.orderops.controller;

import com.orderops.model.AgentLog;
import com.orderops.model.Driver;
import com.orderops.model.Order;
import com.orderops.service.AgentOrchestrator;
import com.orderops.service.DriverService;
import com.orderops.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final DriverService driverService;
    private final AgentOrchestrator agentOrchestrator;
    private final Random random = new Random();

    // Set of typical Brazilian delivery app chats for the automated demo feed
    private static final String[] MOCK_INPUTS = {
        "Cliente: João da Silva\n1 Pizza Calabresa com borda recheada\n1 Coca Cola 2L\nEntregar na Rua Augusta, 1420 ap 104\nObs: Sem cebola, por favor, tenho alergia leve. Campainha com defeito, ligar ao chegar.",
        "Mesa 3 (iFood):\n2 Hamburguer Duplo Cheddar\n1 Batata Frita Grande com Bacon\nEntregar na Av Paulista, 1000 - Recepção\nUrgente! O cliente ligou reclamando de demora no pedido anterior.",
        "Cliente: Maria Julia\n1 Salada Caesar com grelhado\n1 Suco Natural de Laranja 500ml\nEntrega na Rua da Consolação, 2300 bloco B ap 12\nInstrução: Entregar na portaria. Deixar sob cuidados do porteiro Francisco.",
        "Pedido Rappi #994\n3 Temaki Completo de Salmão\n1 Refrigerante Guaraná Lata\nEndereço: Alameda Lorena, 450 apto 92\nNota: Mandar talheres de plástico e sachês de shoyu extra.",
        "Cliente: Fernando Santos\n1 Lasanha Bolonhesa Grande (2 pessoas)\n2 Tortas de Limão fatia\nEntregar na Rua Bela Cintra, 890\nObs: Troco para R$ 100,00."
    };

    private static final String[] PLATFORMS = {"iFood", "Rappi", "Uber Eats"};

    public OrderController(OrderService orderService, DriverService driverService, AgentOrchestrator agentOrchestrator) {
        this.orderService = orderService;
        this.driverService = driverService;
        this.agentOrchestrator = agentOrchestrator;
    }

    @GetMapping("/orders")
    public List<Order> getOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/drivers")
    public List<Driver> getDrivers() {
        return driverService.getAllDrivers();
    }

    @PostMapping("/orders/ingest")
    public ResponseEntity<Map<String, String>> ingestOrder(@RequestBody Map<String, String> payload) {
        String rawText = payload.get("rawText");
        String platform = payload.getOrDefault("platform", "iFood");

        if (rawText == null || rawText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "rawText cannot be empty"));
        }

        agentOrchestrator.processRawOrderAsync(rawText, platform);
        return ResponseEntity.ok(Map.of("message", "Order received and queued for AI triage"));
    }

    @PostMapping("/orders/mock-feed")
    public ResponseEntity<Map<String, String>> triggerMockFeed() {
        String randomText = MOCK_INPUTS[random.nextInt(MOCK_INPUTS.length)];
        String randomPlatform = PLATFORMS[random.nextInt(PLATFORMS.length)];

        agentOrchestrator.processRawOrderAsync(randomText, randomPlatform);
        return ResponseEntity.ok(Map.of(
            "message", "Triggered mock feed order",
            "platform", randomPlatform,
            "snippet", randomText.substring(0, Math.min(randomText.length(), 60)) + "..."
        ));
    }

    @PostMapping("/speed")
    public ResponseEntity<Map<String, Object>> changeSpeed(@RequestParam double value) {
        if (value < 0.1 || value > 10.0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Speed multiplier must be between 0.1 and 10.0"));
        }
        agentOrchestrator.setSpeedMultiplier(value);
        return ResponseEntity.ok(Map.of("message", "Speed multiplier updated", "value", value));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetSimulation() {
        orderService.reset();
        return ResponseEntity.ok(Map.of("message", "Simulation state reset"));
    }
}
