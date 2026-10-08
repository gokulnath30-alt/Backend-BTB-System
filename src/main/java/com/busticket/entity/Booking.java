package com.busticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    private int numberOfSeats;

    private double totalAmount;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, CANCELLED

    private LocalDateTime bookingDate;

    // Traveler & Route Stop Details
    @Column(name = "passenger_name")
    private String passengerName;

    @Column(name = "passenger_phone")
    private String passengerPhone;

    @Column(name = "passenger_email")
    private String passengerEmail;

    @Column(name = "boarding_point")
    private String boardingPoint;

    @Column(name = "ending_point")
    private String endingPoint;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "cancellation_fee")
    private Double cancellationFee;

    public LocalDateTime getCreatedAt() {
        return bookingDate;
    }
}
