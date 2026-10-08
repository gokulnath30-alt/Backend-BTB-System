package com.busticket.controller;

import com.busticket.entity.Bus;
import com.busticket.service.BusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
public class BusController {
    private final BusService busService;

    @PostMapping
    public ResponseEntity<Bus> createBus(@RequestBody Bus bus) {
        if (bus.getBusNumber() != null) {
            bus.setBusNumber(bus.getBusNumber().trim().toUpperCase());
        }
        if (bus.getType() == null || bus.getType().trim().isEmpty()) {
            bus.setType("AC Sleeper");
        }
        if (bus.getCapacity() <= 0) {
            bus.setCapacity(40);
        }
        return ResponseEntity.ok(busService.createBus(bus));
    }

    @GetMapping
    public ResponseEntity<List<Bus>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchBuses(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String date
    ) {
        return ResponseEntity.ok(busService.searchBuses(source, destination, date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBusById(@PathVariable Long id) {
        return ResponseEntity.ok(busService.getEnrichedBusById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<Map<String, Object>>> getSeatsForBus(@PathVariable Long id) {
        return ResponseEntity.ok(busService.getSeatsForBus(id));
    }
}

