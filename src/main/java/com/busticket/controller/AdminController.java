package com.busticket.controller;

import com.busticket.entity.*;
import com.busticket.exception.ResourceNotFoundException;
import com.busticket.repository.*;
import com.busticket.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private static final String MASTER_ADMIN_EMAIL = "admin@busticket.com";
    private final UserRepository userRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    private void verifyMasterAdmin(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Access Restricted: Authentication required.");
        }
        boolean isMasterEmail = MASTER_ADMIN_EMAIL.equalsIgnoreCase(authentication.getName());
        boolean hasAdminRole = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isMasterEmail && !hasAdminRole) {
            throw new AccessDeniedException("Access Restricted: Only the designated primary System Administrator (admin@busticket.com) is authorized.");
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication authentication) {
        verifyMasterAdmin(authentication);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalBuses", busRepository.count());
        stats.put("totalRoutes", routeRepository.count());
        stats.put("totalSchedules", scheduleRepository.count());
        stats.put("totalBookings", bookingRepository.count());

        List<Booking> allBookings = bookingRepository.findAll();
        double confirmedRevenue = 0.0;
        double cancellationProfit = 0.0;
        double totalRefunded = 0.0;
        long confirmedCount = 0;
        long cancelledCount = 0;

        for (Booking b : allBookings) {
            if ("CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                confirmedRevenue += b.getTotalAmount();
                confirmedCount++;
            } else if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                cancelledCount++;
                double fee = b.getCancellationFee() != null ? b.getCancellationFee() : Math.round(b.getTotalAmount() * 0.30 * 100.0) / 100.0;
                double refund = b.getRefundAmount() != null ? b.getRefundAmount() : Math.round((b.getTotalAmount() - fee) * 100.0) / 100.0;
                cancellationProfit += fee;
                totalRefunded += refund;
            }
        }

        stats.put("confirmedBookings", confirmedCount);
        stats.put("cancelledBookings", cancelledCount);
        stats.put("confirmedRevenue", Math.round(confirmedRevenue * 100.0) / 100.0);
        stats.put("cancellationProfit", Math.round(cancellationProfit * 100.0) / 100.0);
        stats.put("totalRefunded", Math.round(totalRefunded * 100.0) / 100.0);
        stats.put("totalNetProfit", Math.round((confirmedRevenue + cancellationProfit) * 100.0) / 100.0);

        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // 1. USERS MANAGEMENT
    // ==========================================
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(Authentication authentication) {
        verifyMasterAdmin(authentication);
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName() != null ? u.getName() : "");
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("email", u.getEmail());
            map.put("phoneNumber", u.getPhoneNumber() != null ? u.getPhoneNumber() : "");
            map.put("role", u.getRole() != null ? u.getRole().name() : "USER");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id, Authentication authentication) {
        verifyMasterAdmin(authentication);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (MASTER_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail()) || user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Action Forbidden: Cannot delete the primary System Administrator account.");
        }

        // Clean up user bookings, payments, and seat reservations
        List<Booking> userBookings = bookingRepository.findByUserId(id);
        for (Booking b : userBookings) {
            List<Seat> seats = seatRepository.findByBookingId(b.getId());
            for (Seat s : seats) {
                s.setBooked(false);
                s.setBooking(null);
                seatRepository.save(s);
            }
            paymentRepository.findByBookingId(b.getId()).ifPresent(paymentRepository::delete);
            bookingRepository.delete(b);
        }

        userRepository.delete(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User " + user.getEmail() + " deleted successfully.");
        response.put("deletedId", id);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. BUSES / FLEET MANAGEMENT
    // ==========================================
    @GetMapping("/buses")
    public ResponseEntity<List<Bus>> getAllBuses(Authentication authentication) {
        verifyMasterAdmin(authentication);
        return ResponseEntity.ok(busRepository.findAll());
    }

    @PostMapping("/buses")
    public ResponseEntity<Bus> createBus(@RequestBody Bus bus, Authentication authentication) {
        verifyMasterAdmin(authentication);
        if (bus.getBusNumber() == null || bus.getBusNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Coach registration / Number plate is required.");
        }
        String formattedPlate = bus.getBusNumber().trim().toUpperCase();
        if (busRepository.findByBusNumber(formattedPlate).isPresent()) {
            throw new IllegalArgumentException("A coach with number plate '" + formattedPlate + "' already exists in fleet.");
        }
        bus.setBusNumber(formattedPlate);
        if (bus.getType() == null || bus.getType().trim().isEmpty()) {
            bus.setType("AC Sleeper");
        } else {
            bus.setType(bus.getType().trim());
        }
        if (bus.getCapacity() <= 0) {
            bus.setCapacity(40);
        }
        Bus savedBus = busRepository.save(bus);
        return ResponseEntity.ok(savedBus);
    }

    @PutMapping("/buses/{id}")
    public ResponseEntity<Bus> updateBus(@PathVariable Long id, @RequestBody Bus busDetails, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));

        if (busDetails.getBusNumber() != null && !busDetails.getBusNumber().trim().isEmpty()) {
            String newPlate = busDetails.getBusNumber().trim().toUpperCase();
            if (!newPlate.equalsIgnoreCase(bus.getBusNumber()) && busRepository.findByBusNumber(newPlate).isPresent()) {
                throw new IllegalArgumentException("A coach with number plate '" + newPlate + "' already exists in fleet.");
            }
            bus.setBusNumber(newPlate);
        }
        if (busDetails.getType() != null && !busDetails.getType().trim().isEmpty()) {
            bus.setType(busDetails.getType().trim());
        }
        if (busDetails.getCapacity() > 0) {
            bus.setCapacity(busDetails.getCapacity());
        }
        Bus updatedBus = busRepository.save(bus);
        return ResponseEntity.ok(updatedBus);
    }

    @DeleteMapping("/buses/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteBus(@PathVariable Long id, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));

        List<Schedule> schedules = scheduleRepository.findByBusId(id);
        for (Schedule s : schedules) {
            deleteScheduleCascade(s);
        }

        busRepository.delete(bus);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bus Coach " + bus.getBusNumber() + " deleted successfully.");
        response.put("deletedId", id);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 3. ROUTES MANAGEMENT
    // ==========================================
    @GetMapping("/routes")
    public ResponseEntity<List<Route>> getAllRoutes(Authentication authentication) {
        verifyMasterAdmin(authentication);
        return ResponseEntity.ok(routeRepository.findAll());
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<Route> getRouteById(@PathVariable Long id, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
        return ResponseEntity.ok(route);
    }

    @PostMapping("/routes")
    public ResponseEntity<Route> createRoute(@RequestBody Route route, Authentication authentication) {
        verifyMasterAdmin(authentication);
        if (route.getSource() == null || route.getSource().trim().isEmpty()) {
            throw new IllegalArgumentException("Origin city (source) is required.");
        }
        if (route.getDestination() == null || route.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination city is required.");
        }
        if (route.getSource().trim().equalsIgnoreCase(route.getDestination().trim())) {
            throw new IllegalArgumentException("Origin and Destination cannot be the same city.");
        }

        route.setSource(route.getSource().trim());
        route.setDestination(route.getDestination().trim());
        Route saved = routeRepository.save(route);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long id, @RequestBody Route updatedRoute, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        if (updatedRoute.getSource() != null && !updatedRoute.getSource().trim().isEmpty()) {
            route.setSource(updatedRoute.getSource().trim());
        }
        if (updatedRoute.getDestination() != null && !updatedRoute.getDestination().trim().isEmpty()) {
            route.setDestination(updatedRoute.getDestination().trim());
        }
        if (route.getSource().equalsIgnoreCase(route.getDestination())) {
            throw new IllegalArgumentException("Origin and Destination cannot be the same city.");
        }
        if (updatedRoute.getDistance() > 0) {
            route.setDistance(updatedRoute.getDistance());
        }

        Route saved = routeRepository.save(route);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/routes/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteRoute(@PathVariable Long id, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        List<Schedule> schedules = scheduleRepository.findByRouteId(id);
        for (Schedule s : schedules) {
            deleteScheduleCascade(s);
        }

        routeRepository.delete(route);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Route " + route.getSource() + " → " + route.getDestination() + " deleted successfully.");
        response.put("deletedId", id);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 4. SCHEDULES MANAGEMENT
    // ==========================================
    @GetMapping("/schedules")
    public ResponseEntity<List<Schedule>> getAllSchedules(Authentication authentication) {
        verifyMasterAdmin(authentication);
        return ResponseEntity.ok(scheduleRepository.findAll());
    }

    @DeleteMapping("/schedules/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteSchedule(@PathVariable Long id, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));

        deleteScheduleCascade(schedule);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Schedule #" + id + " deleted successfully.");
        response.put("deletedId", id);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 5. BOOKINGS MANAGEMENT
    // ==========================================
    @GetMapping("/bookings")
    public ResponseEntity<List<Map<String, Object>>> getAllBookings(Authentication authentication) {
        verifyMasterAdmin(authentication);
        List<Booking> bookings = bookingRepository.findAll();
        List<Map<String, Object>> list = bookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("passengerName", b.getUser() != null ? b.getUser().getName() : "Unknown");
            map.put("passengerEmail", b.getUser() != null ? b.getUser().getEmail() : "—");
            map.put("numberOfSeats", b.getNumberOfSeats());
            map.put("totalAmount", b.getTotalAmount());
            map.put("status", b.getStatus());
            map.put("bookingDate", b.getBookingDate());
            Double fee = b.getCancellationFee();
            Double refund = b.getRefundAmount();
            if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                if (fee == null) fee = Math.round(b.getTotalAmount() * 0.30 * 100.0) / 100.0;
                if (refund == null) refund = Math.round((b.getTotalAmount() - fee) * 100.0) / 100.0;
            }
            map.put("cancellationFee", fee);
            map.put("refundAmount", refund);
            if (b.getSchedule() != null) {
                map.put("scheduleId", b.getSchedule().getId());
                map.put("departureTime", b.getSchedule().getDepartureTime());
                map.put("fare", b.getSchedule().getFare());
                if (b.getSchedule().getRoute() != null) {
                    map.put("source", b.getSchedule().getRoute().getSource());
                    map.put("destination", b.getSchedule().getRoute().getDestination());
                }
                if (b.getSchedule().getBus() != null) {
                    map.put("busNumber", b.getSchedule().getBus().getBusNumber());
                    map.put("busType", b.getSchedule().getBus().getType());
                }
            }
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/bookings/{id}/cancel")
    @Transactional
    public ResponseEntity<Map<String, Object>> cancelBookingByAdmin(@PathVariable Long id, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Booking booking = bookingService.cancelBooking(id, authentication.getName());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Booking #" + id + " cancelled. 70% refunded (₹" + booking.getRefundAmount() + "), 30% retained as platform profit (₹" + booking.getCancellationFee() + ").");
        response.put("booking", booking);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/bookings/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteBooking(@PathVariable Long id, Authentication authentication) {
        verifyMasterAdmin(authentication);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // Free up reserved seats
        List<Seat> seats = seatRepository.findByBookingId(id);
        for (Seat s : seats) {
            s.setBooked(false);
            s.setBooking(null);
            seatRepository.save(s);
        }

        // Delete payment
        paymentRepository.findByBookingId(id).ifPresent(paymentRepository::delete);

        // Delete booking
        bookingRepository.delete(booking);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Booking #" + id + " deleted successfully.");
        response.put("deletedId", id);
        return ResponseEntity.ok(response);
    }

    private void deleteScheduleCascade(Schedule s) {
        List<Booking> bookings = bookingRepository.findByScheduleId(s.getId());
        for (Booking b : bookings) {
            List<Seat> seats = seatRepository.findByBookingId(b.getId());
            for (Seat seat : seats) {
                seat.setBooked(false);
                seat.setBooking(null);
                seatRepository.save(seat);
            }
            paymentRepository.findByBookingId(b.getId()).ifPresent(paymentRepository::delete);
            bookingRepository.delete(b);
        }
        List<Seat> scheduleSeats = seatRepository.findByScheduleId(s.getId());
        seatRepository.deleteAll(scheduleSeats);
        scheduleRepository.delete(s);
    }
}
