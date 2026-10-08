package com.busticket.config;

import com.busticket.entity.Bus;
import com.busticket.entity.Role;
import com.busticket.entity.Route;
import com.busticket.entity.Schedule;
import com.busticket.entity.Seat;
import com.busticket.entity.User;
import com.busticket.repository.BusRepository;
import com.busticket.repository.RouteRepository;
import com.busticket.repository.ScheduleRepository;
import com.busticket.repository.SeatRepository;
import com.busticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RouteDataInitializer implements CommandLineRunner {

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            seedDefaultUsers();
            seedAllRoutesAndSchedules();
        } catch (Exception e) {
            log.error("Error initializing route seed data: {}", e.getMessage(), e);
        }
    }

    private void seedDefaultUsers() {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .email("admin@example.com")
                    .name("System Admin")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.ADMIN)
                    .phone("+1-555-0100")
                    .build());

            userRepository.save(User.builder()
                    .email("operator1@example.com")
                    .name("Express Operator")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.OPERATOR)
                    .phone("+1-555-0101")
                    .build());

            userRepository.save(User.builder()
                    .email("customer1@example.com")
                    .name("Alex Customer")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.USER)
                    .phone("+1-555-0102")
                    .build());
            log.info("Default seed users (Admin, Operator, Customer) created successfully.");
        }
    }

    private void seedAllRoutesAndSchedules() {
        // Ensure standard routes exist
        Route delhiToJaipur = getOrCreateRoute("Delhi", "Jaipur", 280.0);
        Route mumbaiToPune = getOrCreateRoute("Mumbai", "Pune", 150.0);
        Route blrToChennai = getOrCreateRoute("Bangalore", "Chennai", 345.0);
        Route hydToBlr = getOrCreateRoute("Hyderabad", "Bangalore", 570.0);
        Route salemToChennai = getOrCreateRoute("Salem", "Chennai", 340.0);
        Route salemToBangalore = getOrCreateRoute("Salem", "Bangalore", 205.0);
        Route blrToGoa = getOrCreateRoute("Bangalore", "Goa", 620.0);
        Route goaToBlr = getOrCreateRoute("Goa", "Bangalore", 620.0);

        // Ensure standard buses exist
        Bus busDelhi = getOrCreateBus("DL-01-EXP-1001", "AC Sleeper", 30);
        Bus busMumbai = getOrCreateBus("MH-02-EXP-2002", "AC Seater", 40);
        Bus busBlr = getOrCreateBus("KA-03-CTY-3003", "AC Sleeper", 32);
        Bus busHyd = getOrCreateBus("TN-04-CTY-4004", "Non-AC Seater", 40);
        Bus busSalem1 = getOrCreateBus("TN-30-SLM-5005", "AC Sleeper", 32);
        Bus busSalem2 = getOrCreateBus("TN-30-SLM-6006", "AC Seater", 40);
        Bus busSalem3 = getOrCreateBus("TN-27-SLM-7007", "AC Sleeper", 36);
        Bus busGoa = getOrCreateBus("KA-01-VIP-5555", "Multi-Axle Volvo Sleeper", 54);

        // Ensure schedules exist for Today (offset 0), Tomorrow (offset 1), and Day After (offset 2)
        for (int offset = 0; offset <= 3; offset++) {
            LocalDate targetDate = LocalDate.now().plusDays(offset);

            // Delhi -> Jaipur
            if (!hasScheduleOnDate(delhiToJaipur.getId(), targetDate)) {
                createScheduleWithSeats(busDelhi, delhiToJaipur, LocalTime.of(6, 0), 5, 750.0, offset);
                createScheduleWithSeats(busDelhi, delhiToJaipur, LocalTime.of(14, 30), 5, 700.0, offset);
                createScheduleWithSeats(busDelhi, delhiToJaipur, LocalTime.of(22, 0), 5, 850.0, offset);
            }

            // Mumbai -> Pune
            if (!hasScheduleOnDate(mumbaiToPune.getId(), targetDate)) {
                createScheduleWithSeats(busMumbai, mumbaiToPune, LocalTime.of(8, 0), 3, 450.0, offset);
                createScheduleWithSeats(busMumbai, mumbaiToPune, LocalTime.of(17, 30), 3, 480.0, offset);
            }

            // Bangalore -> Chennai
            if (!hasScheduleOnDate(blrToChennai.getId(), targetDate)) {
                createScheduleWithSeats(busBlr, blrToChennai, LocalTime.of(7, 30), 6, 850.0, offset);
                createScheduleWithSeats(busBlr, blrToChennai, LocalTime.of(22, 30), 6, 950.0, offset);
            }

            // Hyderabad -> Bangalore
            if (!hasScheduleOnDate(hydToBlr.getId(), targetDate)) {
                createScheduleWithSeats(busHyd, hydToBlr, LocalTime.of(21, 0), 9, 650.0, offset);
            }

            // Salem -> Chennai
            if (!hasScheduleOnDate(salemToChennai.getId(), targetDate)) {
                createScheduleWithSeats(busSalem1, salemToChennai, LocalTime.of(6, 30), 6, 550.0, offset);
                createScheduleWithSeats(busSalem2, salemToChennai, LocalTime.of(14, 0), 6, 450.0, offset);
                createScheduleWithSeats(busSalem1, salemToChennai, LocalTime.of(22, 30), 6, 650.0, offset);
            }

            // Salem -> Bangalore
            if (!hasScheduleOnDate(salemToBangalore.getId(), targetDate)) {
                createScheduleWithSeats(busSalem3, salemToBangalore, LocalTime.of(7, 0), 4, 480.0, offset);
                createScheduleWithSeats(busSalem2, salemToBangalore, LocalTime.of(15, 30), 4, 380.0, offset);
                createScheduleWithSeats(busSalem3, salemToBangalore, LocalTime.of(23, 0), 4, 520.0, offset);
            }

            // Bangalore -> Goa
            if (!hasScheduleOnDate(blrToGoa.getId(), targetDate)) {
                createScheduleWithSeats(busGoa, blrToGoa, LocalTime.of(21, 0), 11, 1650.0, offset);
            }

            // Goa -> Bangalore
            if (!hasScheduleOnDate(goaToBlr.getId(), targetDate)) {
                createScheduleWithSeats(busGoa, goaToBlr, LocalTime.of(19, 30), 11, 1650.0, offset);
            }

            // Auto-provision schedules for any other route existing in the database
            List<Route> allRoutes = routeRepository.findAll();
            List<Bus> allBuses = busRepository.findAll();
            Bus fallbackBus = allBuses.isEmpty() ? busGoa : allBuses.get(0);
            for (Route r : allRoutes) {
                if (!hasScheduleOnDate(r.getId(), targetDate)) {
                    int durHours = r.getDistance() > 0 ? Math.max(2, (int) Math.round(r.getDistance() / 55.0)) : 5;
                    double estFare = r.getDistance() > 0 ? Math.max(300.0, Math.round(r.getDistance() * 2.2)) : 650.0;
                    createScheduleWithSeats(fallbackBus, r, LocalTime.of(10, 0), durHours, estFare, offset);
                }
            }
        }
        log.info("Finished ensuring current and upcoming schedules for all active routes.");
    }

    private boolean hasScheduleOnDate(Long routeId, LocalDate date) {
        List<Schedule> list = scheduleRepository.findByRouteId(routeId);
        return list.stream().anyMatch(s -> s.getDepartureTime() != null && s.getDepartureTime().toLocalDate().isEqual(date));
    }

    private Route getOrCreateRoute(String source, String destination, double distance) {
        return routeRepository.findBySourceIgnoreCaseAndDestinationIgnoreCase(source, destination)
                .orElseGet(() -> routeRepository.save(Route.builder()
                        .source(source)
                        .destination(destination)
                        .distance(distance)
                        .build()));
    }

    private Bus getOrCreateBus(String busNumber, String type, int capacity) {
        return busRepository.findByBusNumber(busNumber)
                .orElseGet(() -> busRepository.save(Bus.builder()
                        .busNumber(busNumber)
                        .type(type)
                        .capacity(capacity)
                        .build()));
    }

    private void createScheduleWithSeats(Bus bus, Route route, LocalTime depTime, int durationHours, double fare, int dayOffset) {
        LocalDate date = LocalDate.now().plusDays(dayOffset);
        LocalDateTime departure = LocalDateTime.of(date, depTime);
        LocalDateTime arrival = departure.plusHours(durationHours);

        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .bus(bus)
                .route(route)
                .departureTime(departure)
                .arrivalTime(arrival)
                .fare(fare)
                .build());

        int capacity = bus.getCapacity() > 0 ? bus.getCapacity() : 30;
        for (int i = 1; i <= capacity; i++) {
            boolean isBooked = (i == 2 || i == 5 || i == 9);
            seatRepository.save(Seat.builder()
                    .schedule(schedule)
                    .seatNumber("S" + i)
                    .isBooked(isBooked)
                    .build());
        }
    }
}
