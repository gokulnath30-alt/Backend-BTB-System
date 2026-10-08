package com.busticket.service;

import com.busticket.entity.Route;
import com.busticket.exception.ResourceNotFoundException;
import com.busticket.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;

    public Route createRoute(Route route) {
        return routeRepository.save(route);
    }

    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    public Route getRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
    }

    public Route updateRoute(Long id, Route updatedRoute) {
        Route route = getRouteById(id);
        if (updatedRoute.getSource() != null && !updatedRoute.getSource().trim().isEmpty()) {
            route.setSource(updatedRoute.getSource().trim());
        }
        if (updatedRoute.getDestination() != null && !updatedRoute.getDestination().trim().isEmpty()) {
            route.setDestination(updatedRoute.getDestination().trim());
        }
        if (updatedRoute.getDistance() > 0) {
            route.setDistance(updatedRoute.getDistance());
        }
        return routeRepository.save(route);
    }

    public void deleteRoute(Long id) {
        Route route = getRouteById(id);
        routeRepository.delete(route);
    }
}
