package com.busticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private double amount;

    private String paymentMethod;

    private String paymentStatus; // SUCCESS, FAILED, REFUNDED, CANCELLED

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "cancellation_fee")
    private Double cancellationFee;

    private LocalDateTime paymentDate;
}
