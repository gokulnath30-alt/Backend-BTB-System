package com.busticket.controller;

import com.busticket.dto.ScheduleRequest;
import com.busticket.entity.Schedule;
import com.busticket.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<Schedule> createSchedule(@RequestBody ScheduleRequest request) {
        Schedule schedule = Schedule.builder()
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .fare(request.getFare())
                .build();
        return ResponseEntity.ok(scheduleService.createSchedule(schedule, request.getBusId(), request.getRouteId()));
    }

    @GetMapping
    public ResponseEntity<List<Schedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }
    
    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<Schedule>> getSchedulesByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByRoute(routeId));
    }
}
