package com.busticket.controller;

import com.busticket.entity.Bus;
import com.busticket.entity.Schedule;
import com.busticket.repository.BusRepository;
import com.busticket.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/operator")
@RequiredArgsConstructor
public class OperatorController {
    private final BusRepository busRepository;
    private final ScheduleRepository scheduleRepository;

    @GetMapping("/buses")
    public ResponseEntity<List<Map<String, Object>>> getOperatorBuses() {
        List<Bus> buses = busRepository.findAll();
        List<Schedule> schedules = scheduleRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Bus b : buses) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("name", b.getBusNumber());
            map.put("busNumber", b.getBusNumber());
            map.put("busType", b.getType());
            map.put("capacity", b.getCapacity());

            Optional<Schedule> schedOpt = schedules.stream()
                    .filter(s -> s.getBus() != null && s.getBus().getId().equals(b.getId()))
                    .findFirst();

            if (schedOpt.isPresent()) {
                Schedule s = schedOpt.get();
                map.put("source", s.getRoute() != null ? s.getRoute().getSource() : "Standard Route");
                map.put("destination", s.getRoute() != null ? s.getRoute().getDestination() : "Standard Route");
            } else {
                map.put("source", "City Center");
                map.put("destination", "Station Terminal");
            }
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }
}
