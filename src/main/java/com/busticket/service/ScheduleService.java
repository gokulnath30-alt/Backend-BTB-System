package com.busticket.service;

import com.busticket.entity.Bus;
import com.busticket.entity.Route;
import com.busticket.entity.Schedule;
import com.busticket.entity.Seat;
import com.busticket.exception.ResourceNotFoundException;
import com.busticket.repository.BusRepository;
import com.busticket.repository.RouteRepository;
import com.busticket.repository.ScheduleRepository;
import com.busticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public Schedule createSchedule(Schedule schedule, Long busId, Long routeId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        
        if (schedule.getDepartureTime() != null && schedule.getDepartureTime().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Schedule departure time cannot be in the past (" + schedule.getDepartureTime() + "). Schedules can only be created for current and future dates.");
        }

        schedule.setBus(bus);
        schedule.setRoute(route);
        Schedule savedSchedule = scheduleRepository.save(schedule);

        // Auto-generate seats for the schedule
        int capacity = bus.getCapacity() > 0 ? bus.getCapacity() : 30;
        for (int i = 1; i <= capacity; i++) {
            Seat seat = Seat.builder()
                    .schedule(savedSchedule)
                    .seatNumber("S" + i)
                    .isBooked(false)
                    .build();
            seatRepository.save(seat);
        }
        return savedSchedule;
    }

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
    }
    
    public List<Schedule> getSchedulesByRoute(Long routeId) {
        return scheduleRepository.findByRouteId(routeId);
    }
}
