package com.busticket.service;

import com.busticket.entity.*;
import com.busticket.exception.ResourceNotFoundException;
import com.busticket.repository.BusRepository;
import com.busticket.repository.ScheduleRepository;
import com.busticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class BusService {
    private final BusRepository busRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;

    public Bus createBus(Bus bus) {
        return busRepository.save(bus);
    }

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public Bus getBusById(Long id) {
        return busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));
    }

    public List<Map<String, Object>> searchBuses(String source, String destination, String date) {
        // If search date is in the past, return empty list immediately - bookings are only for today and future
        if (date != null && !date.isBlank()) {
            try {
                LocalDate searchDate = LocalDate.parse(date.trim());
                if (searchDate.isBefore(LocalDate.now())) {
                    return Collections.emptyList();
                }
            } catch (Exception ignored) {
            }
        }

        List<Schedule> schedules = scheduleRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Schedule s : schedules) {
            Route r = s.getRoute();

            boolean matchSource = (source == null || source.isBlank()) ||
                    (r != null && r.getSource() != null
                            && r.getSource().toLowerCase().contains(source.trim().toLowerCase()));
            boolean matchDest = (destination == null || destination.isBlank()) ||
                    (r != null && r.getDestination() != null
                            && r.getDestination().toLowerCase().contains(destination.trim().toLowerCase()));

            boolean matchDate = true;
            if (date != null && !date.isBlank() && s.getDepartureTime() != null) {
                try {
                    LocalDate searchDate = LocalDate.parse(date.trim());
                    matchDate = s.getDepartureTime().toLocalDate().isEqual(searchDate);
                } catch (Exception ignored) {
                    matchDate = true;
                }
            }

            if (matchSource && matchDest && matchDate) {
                results.add(mapScheduleToBusCard(s));
            }
        }

        // Only fall back to matching route if the user DID NOT specify a strict date filter
        if (results.isEmpty() && (date == null || date.isBlank())
                && ((source != null && !source.isBlank()) || (destination != null && !destination.isBlank()))) {
            for (Schedule s : schedules) {
                Route r = s.getRoute();
                boolean matchSource = (source == null || source.isBlank()) ||
                        (r != null && r.getSource() != null
                                && r.getSource().toLowerCase().contains(source.trim().toLowerCase()));
                boolean matchDest = (destination == null || destination.isBlank()) ||
                        (r != null && r.getDestination() != null
                                && r.getDestination().toLowerCase().contains(destination.trim().toLowerCase()));

                if (matchSource && matchDest) {
                    results.add(mapScheduleToBusCard(s));
                }
            }
        }

        // If search returned empty and no criteria specified, return all future/active schedules
        if (results.isEmpty() && (source == null || source.isBlank())
                && (destination == null || destination.isBlank())
                && (date == null || date.isBlank())) {
            for (Schedule s : schedules) {
                results.add(mapScheduleToBusCard(s));
            }
        }

        return results;
    }

    public Map<String, Object> getEnrichedBusById(Long id) {
        // Try finding by Schedule id first
        Optional<Schedule> schedOpt = scheduleRepository.findById(id);
        if (schedOpt.isPresent()) {
            return mapScheduleToBusCard(schedOpt.get());
        }

        // Try finding by Bus id
        Optional<Bus> busOpt = busRepository.findById(id);
        if (busOpt.isPresent()) {
            Bus b = busOpt.get();
            Optional<Schedule> sOpt = scheduleRepository.findFirstByBusId(b.getId());
            if (sOpt.isPresent()) {
                return mapScheduleToBusCard(sOpt.get());
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("busId", b.getId());
            map.put("name", b.getBusNumber());
            map.put("busNumber", b.getBusNumber());
            map.put("busType", b.getType());
            map.put("type", b.getType());
            map.put("capacity", b.getCapacity());
            map.put("source", "City Center");
            map.put("destination", "Central Station");
            map.put("departureTime", LocalDateTime.now().plusDays(1).withHour(8).withMinute(0));
            map.put("arrivalTime", LocalDateTime.now().plusDays(1).withHour(13).withMinute(30));
            map.put("fare", 500.0);
            map.put("availableSeats", b.getCapacity());
            return map;
        }

        throw new ResourceNotFoundException("Bus or Schedule not found with id: " + id);
    }

    @Transactional
    public List<Map<String, Object>> getSeatsForBus(Long id) {
        // Find schedule for this id (either schedule id or bus id)
        Schedule schedule = scheduleRepository.findById(id).orElse(null);
        if (schedule == null) {
            schedule = scheduleRepository.findFirstByBusId(id).orElse(null);
        }

        if (schedule == null) {
            // fallback to first schedule if exists
            schedule = scheduleRepository.findAll().stream().findFirst().orElse(null);
        }

        List<Seat> seats = Collections.emptyList();
        if (schedule != null) {
            seats = seatRepository.findByScheduleId(schedule.getId());
            if (seats.isEmpty()) {
                // Initialize default 24 seats
                int capacity = (schedule.getBus() != null && schedule.getBus().getCapacity() > 0)
                        ? schedule.getBus().getCapacity()
                        : 24;
                if (capacity > 40)
                    capacity = 40;
                List<Seat> newSeats = new ArrayList<>();
                for (int i = 1; i <= capacity; i++) {
                    Seat seat = Seat.builder()
                            .schedule(schedule)
                            .seatNumber("S" + i)
                            .isBooked(false)
                            .build();
                    newSeats.add(seatRepository.save(seat));
                }
                seats = newSeats;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Seat seat : seats) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", seat.getId());
            map.put("seatNumber", seat.getSeatNumber());
            map.put("isBooked", seat.isBooked());
            map.put("status", seat.isBooked() ? "BOOKED" : "AVAILABLE");
            map.put("scheduleId", schedule != null ? schedule.getId() : null);
            result.add(map);
        }
        return result;
    }

    private Map<String, Object> mapScheduleToBusCard(Schedule s) {
        Map<String, Object> map = new HashMap<>();
        Bus b = s.getBus();
        Route r = s.getRoute();

        map.put("id", s.getId());
        map.put("scheduleId", s.getId());
        map.put("busId", b != null ? b.getId() : s.getId());
        map.put("name", b != null ? b.getBusNumber() : "Express Coach");
        map.put("busNumber", b != null ? b.getBusNumber() : "EXP-" + s.getId());
        map.put("busType", b != null ? b.getType() : "AC Sleeper");
        map.put("type", b != null ? b.getType() : "AC Sleeper");
        map.put("source", r != null ? r.getSource() : "Source");
        map.put("destination", r != null ? r.getDestination() : "Destination");
        map.put("departureTime", s.getDepartureTime());
        map.put("arrivalTime", s.getArrivalTime());
        map.put("fare", s.getFare());
        map.put("capacity", b != null ? b.getCapacity() : 40);

        List<Seat> seats = seatRepository.findByScheduleId(s.getId());
        long bookedCount = seats.stream().filter(Seat::isBooked).count();
        int total = seats.isEmpty() ? (b != null ? b.getCapacity() : 40) : seats.size();
        map.put("availableSeats", Math.max(0, total - (int) bookedCount));
        map.put("totalSeats", total);

        boolean isDeparted = s.getDepartureTime() != null && s.getDepartureTime().isBefore(LocalDateTime.now());
        map.put("isDeparted", isDeparted);

        return map;
    }
}
