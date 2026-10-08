package com.busticket.repository;

import com.busticket.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByRouteId(Long routeId);
    List<Schedule> findByBusId(Long busId);
    Optional<Schedule> findFirstByBusId(Long busId);
}

