package com.busticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    private String seatNumber;

    private boolean isBooked;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    public String getStatus() {
        return isBooked ? "BOOKED" : "AVAILABLE";
    }
}

