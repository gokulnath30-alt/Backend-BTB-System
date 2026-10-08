package com.busticket.service;

import com.busticket.dto.BookingRequest;
import com.busticket.entity.*;
import com.busticket.exception.ResourceNotFoundException;
import com.busticket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Booking createBooking(BookingRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Schedule schedule = null;
        if (request.getScheduleId() != null) {
            schedule = scheduleRepository.findById(request.getScheduleId()).orElse(null);
        }
        if (schedule == null && request.getBusId() != null) {
            schedule = scheduleRepository.findFirstByBusId(request.getBusId())
                    .orElseGet(() -> scheduleRepository.findById(request.getBusId()).orElse(null));
        }
        if (schedule == null) {
            // fallback: get first available schedule if any
            schedule = scheduleRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No schedule found for booking"));
        }

        // Validate that booking is only allowed for future departure dates/times, not for past
        if (schedule.getDepartureTime() != null && schedule.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking is not available for past departures (" 
                    + schedule.getDepartureTime() + "). Reservations are only allowed for current or future schedules.");
        }

        List<Seat> scheduleSeats = seatRepository.findByScheduleId(schedule.getId());
        List<String> targetSeats = new ArrayList<>();
        if (request.getSeatNumbers() != null) {
            targetSeats.addAll(request.getSeatNumbers());
        }
        if (request.getSeatIds() != null) {
            targetSeats.addAll(request.getSeatIds());
        }

        List<Seat> availableSeats = scheduleSeats.stream()
                .filter(seat -> targetSeats.contains(seat.getSeatNumber())
                        || targetSeats.contains(String.valueOf(seat.getId())))
                .distinct()
                .collect(Collectors.toList());

        // If targetSeats were specified but not yet in the DB for this schedule, create
        // them
        if (availableSeats.isEmpty() && !targetSeats.isEmpty()) {
            for (String sVal : targetSeats) {
                String seatNum = sVal.matches("^\\d+$") ? "S" + sVal : sVal;
                Seat seat = Seat.builder()
                        .schedule(schedule)
                        .seatNumber(seatNum)
                        .isBooked(false)
                        .build();
                availableSeats.add(seatRepository.save(seat));
            }
        }

        for (Seat seat : availableSeats) {
            if (seat.isBooked()) {
                throw new RuntimeException("Seat " + seat.getSeatNumber() + " is already booked.");
            }
        }

        int seatCount = Math.max(1, availableSeats.size());
        double fare = schedule.getFare() > 0 ? schedule.getFare() : 500.0;
        double totalAmount = fare * seatCount;

        String passName = (request.getPassengerName() != null && !request.getPassengerName().isBlank())
                ? request.getPassengerName().trim()
                : (user.getName() != null ? user.getName() : "Passenger");
        String passPhone = (request.getPassengerPhone() != null && !request.getPassengerPhone().isBlank())
                ? request.getPassengerPhone().trim()
                : (user.getPhone() != null ? user.getPhone() : "");
        String passEmail = (request.getPassengerEmail() != null && !request.getPassengerEmail().isBlank())
                ? request.getPassengerEmail().trim()
                : (user.getEmail() != null ? user.getEmail() : "");
        String boardingPt = (request.getBoardingPoint() != null && !request.getBoardingPoint().isBlank())
                ? request.getBoardingPoint().trim()
                : "Main City Terminal";
        String endingPt = (request.getEndingPoint() != null && !request.getEndingPoint().isBlank())
                ? request.getEndingPoint().trim()
                : ((request.getDroppingPoint() != null && !request.getDroppingPoint().isBlank())
                        ? request.getDroppingPoint().trim()
                        : "Destination Central Station");
        String emergContact = (request.getEmergencyContact() != null && !request.getEmergencyContact().isBlank())
                ? request.getEmergencyContact().trim()
                : "";

        String paymentMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod()
                : "UPI";

        Booking booking = Booking.builder()
                .user(user)
                .schedule(schedule)
                .numberOfSeats(seatCount)
                .totalAmount(totalAmount)
                .status("PENDING")
                .bookingDate(LocalDateTime.now())
                .passengerName(passName)
                .passengerPhone(passPhone)
                .passengerEmail(passEmail)
                .boardingPoint(boardingPt)
                .endingPoint(endingPt)
                .emergencyContact(emergContact)
                .paymentMethod(paymentMethod)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        for (Seat seat : availableSeats) {
            seat.setBooked(true);
            seat.setBooking(savedBooking);
            seatRepository.save(seat);
        }

        Payment payment = Payment.builder()
                .booking(savedBooking)
                .amount(totalAmount)
                .paymentMethod(paymentMethod)
                .paymentStatus("PENDING")
                .paymentDate(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        return savedBooking;
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    @Transactional
    public Booking payBooking(Long id, String paymentMethod) {
        Booking booking = getBookingById(id);
        booking.setStatus("CONFIRMED");
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            booking.setPaymentMethod(paymentMethod);
        }
        Booking updated = bookingRepository.save(booking);

        Payment payment = paymentRepository.findByBookingId(id).orElse(null);
        if (payment == null) {
            payment = Payment.builder()
                    .booking(updated)
                    .amount(updated.getTotalAmount())
                    .paymentMethod(paymentMethod != null ? paymentMethod : "UPI")
                    .paymentStatus("SUCCESS")
                    .paymentDate(LocalDateTime.now())
                    .build();
        } else {
            payment.setPaymentStatus("SUCCESS");
            if (paymentMethod != null && !paymentMethod.isBlank()) {
                payment.setPaymentMethod(paymentMethod);
            }
            payment.setPaymentDate(LocalDateTime.now());
        }
        paymentRepository.save(payment);
        return updated;
    }

    @Transactional
    public Booking cancelBooking(Long id, String userEmail) {
        Booking booking = getBookingById(id);

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            return booking;
        }

        if (userEmail != null && booking.getUser() != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null && !user.getEmail().equalsIgnoreCase(booking.getUser().getEmail())
                    && user.getRole() != Role.ADMIN) {
                throw new RuntimeException("Unauthorized: You can only cancel your own bookings");
            }
        }

        // Calculate 30% company profit / cancellation fee and 70% refund to passenger
        double total = booking.getTotalAmount();
        double fee = Math.round(total * 0.30 * 100.0) / 100.0;
        double refund = Math.round((total - fee) * 100.0) / 100.0;

        booking.setCancellationFee(fee);
        booking.setRefundAmount(refund);
        booking.setStatus("CANCELLED");
        Booking updated = bookingRepository.save(booking);

        // Free up the seats reserved for this booking
        List<Seat> bookedSeats = seatRepository.findByBookingId(id);
        for (Seat seat : bookedSeats) {
            seat.setBooked(false);
            seat.setBooking(null);
            seatRepository.save(seat);
        }

        // Update payment status if exists
        Payment payment = paymentRepository.findByBookingId(id).orElse(null);
        if (payment != null) {
            if ("SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {
                payment.setPaymentStatus("REFUNDED");
                payment.setRefundAmount(refund);
                payment.setCancellationFee(fee);
            } else {
                payment.setPaymentStatus("CANCELLED");
                payment.setRefundAmount(0.0);
                payment.setCancellationFee(0.0);
            }
            paymentRepository.save(payment);
        }

        return updated;
    }

    public List<Booking> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return bookingRepository.findByUserId(user.getId());
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}

