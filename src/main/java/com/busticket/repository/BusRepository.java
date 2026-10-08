package com.busticket.repository;

import com.busticket.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByBusNumber(String busNumber);
}
