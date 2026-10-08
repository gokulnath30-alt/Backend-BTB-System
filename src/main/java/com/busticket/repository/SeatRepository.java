package com.busticket.repository;

import com.busticket.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScheduleId(Long scheduleId);
    List<Seat> findByBookingId(Long bookingId);
}
