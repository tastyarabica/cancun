package com.lasthotel.cancun.services;

import com.lasthotel.cancun.AppConfigTest;
import com.lasthotel.cancun.models.Reservation;
import com.lasthotel.cancun.repositories.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = AppConfigTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReservationServiceTest {
    @Autowired
    private ReservationRepository repository;

    @Autowired
    private ReservationService reservationService;

    @BeforeEach
    public void setUpRepository() {
        final Reservation reservation1 = getReservation(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 10));
        final Reservation reservation2 = getReservation(LocalDate.of(2021, 7, 12), LocalDate.of(2021, 7, 14));
        final Reservation reservation3 = getReservation(LocalDate.of(2021, 7, 17), LocalDate.of(2021, 7, 17));
        final Reservation reservation4 = getReservation(LocalDate.of(2021, 7, 16), LocalDate.of(2021, 7, 16));
        reservation1.setId("reservation1");
        reservation2.setId("reservation2");
        reservation3.setId("reservation3");
        reservation4.setId("reservation4");
        reservation4.setUser("anotherUser");

        repository.saveAll(List.of(reservation1, reservation2, reservation3, reservation4))
                .then()
                .block();
    }

    @Test
    public void when_makingOverlappingReservations_then_noReservationMade() {
        final var reservation1 = getReservation(LocalDate.of(2021, 7, 7), LocalDate.of(2021, 7, 9));
        final var reservation2 = getReservation(LocalDate.of(2021, 7, 7), LocalDate.of(2021, 7, 11));
        final var reservation3 = getReservation(LocalDate.of(2021, 7, 9), LocalDate.of(2021, 7, 11));
        final var reservation4 = getReservation(LocalDate.of(2021, 7, 9), LocalDate.of(2021, 7, 9));

        final Mono<Reservation> trial1 = reservationService.makeReservation(reservation1);
        final Mono<Reservation> trial2 = reservationService.makeReservation(reservation2);
        final Mono<Reservation> trial3 = reservationService.makeReservation(reservation3);
        final Mono<Reservation> trial4 = reservationService.makeReservation(reservation4);

        StepVerifier.create(Flux.concat(trial1, trial2, trial3, trial4))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    public void when_makingReservationWithAvailableDate_then_reservationMade() {
        final var reservation1 = getReservation(LocalDate.of(2021, 7, 18), LocalDate.of(2021, 7, 20));

        final Mono<Reservation> save = reservationService.makeReservation(reservation1);

        StepVerifier.create(Flux.concat(save).map(r -> {
            reservation1.setId(r.getId());
            return r;
        }))
                .expectNext(reservation1)
                .verifyComplete();
    }

    @Test
    public void when_updatingReservationOverlaps_then_doesNotUpdate() {
        final Reservation reservation1 = getReservation(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 10));
        final Reservation updated = getReservation(LocalDate.of(2021, 7, 10), LocalDate.of(2021, 7, 12));
        reservation1.setId("reservation1");
        updated.setId("reservation1");

        final Mono<Reservation> updateMono = reservationService.updateReservation("reservation1", updated);

        StepVerifier.create(updateMono)
                .expectNext(reservation1)
                .verifyComplete();
    }

    @Test
    public void when_updatingWithAvailableDates_then_updates() {
        final Reservation reservation1 = getReservation(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 10));
        final Reservation updated = getReservation(LocalDate.of(2021, 7, 10), LocalDate.of(2021, 7, 10));
        reservation1.setId("reservation1");
        updated.setId("reservation1");

        final Mono<Reservation> updateMono = reservationService.updateReservation("reservation1", updated);

        StepVerifier.create(updateMono)
                .expectNext(updated)
                .verifyComplete();
    }

    @Test
    public void when_deletingExistingReservation_then_deletes() {
        final Reservation reservation1 = getReservation(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 10));
        reservation1.setId("reservation1");

        final Mono<Reservation> deleteMono = reservationService.deleteReservation("reservation1");
        final Mono<Reservation> searchMono = reservationService.getReservationById("reservation1");

        StepVerifier.create(deleteMono)
                .expectNext(reservation1)
                .verifyComplete();

        StepVerifier.create(searchMono)
                .expectComplete()
                .verify();
    }

    @Test
    public void when_deletingNonExistentReservation_then_returnsEmpty() {
        final Mono<Reservation> deleteMono = reservationService.deleteReservation("reservation999");
        final Flux<Reservation> allReservationsFlux = reservationService.getAllReservations();

        StepVerifier.create(deleteMono)
                .expectComplete()
                .verify();

        StepVerifier.create(allReservationsFlux)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    public void when_gettingAllReservations_then_returnsSortedByToDateAscending() {
        final Flux<String> reservationDatesAscending = reservationService
                .getAllReservations()
                .map(Reservation::getTo)
                .map(LocalDate::toString);

        StepVerifier.create(reservationDatesAscending)
                .expectNext("2021-07-10", "2021-07-14", "2021-07-16", "2021-07-17")
                .verifyComplete();
    }

    @Test
    public void when_gettingAvailableIntervals_then_returnsCorrectIntervals() {
        final Reservation interval1 = getReservation(LocalDate.of(2021, 7, 7), LocalDate.of(2021, 7, 7));
        final Reservation interval2 = getReservation(LocalDate.of(2021, 7, 11), LocalDate.of(2021, 7, 11));
        final Reservation interval3 = getReservation(LocalDate.of(2021, 7, 15), LocalDate.of(2021, 7, 15));
        final Reservation interval4 = getReservation(LocalDate.of(2021, 7, 18), LocalDate.of(2021, 8, 5));
        interval1.setUser("available");
        interval2.setUser("available");
        interval3.setUser("available");
        interval4.setUser("available");

        final Mono<List<Reservation>> intervals = reservationService.getAvailableReservationIntervals();

        StepVerifier.create(intervals)
                .expectNext(List.of(interval1, interval2, interval3, interval4))
                .verifyComplete();
    }

    @Test
    public void when_gettingReservationsByUser_then_returnsOnlyUsersReservations() {
        final Flux<Reservation> trial1 = reservationService.getReservationsForUser("test");
        final Flux<Reservation> trial2 = reservationService.getReservationsForUser("anotherUser");
        final Flux<Reservation> trial3 = reservationService.getReservationsForUser("none");

        StepVerifier.create(trial1)
                .expectNextCount(3)
                .verifyComplete();

        StepVerifier.create(trial2)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(trial3).verifyComplete();
    }

    @Test
    public void when_gettingReservationById_then_returnsCorrectReservation() {
        final Reservation reservation1 = getReservation(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 10));
        reservation1.setId("reservation1");

        final Mono<Reservation> reservationMono = reservationService.getReservationById("reservation1");

        StepVerifier.create(reservationMono)
                .expectNext(reservation1)
                .verifyComplete();
    }

    private Reservation getReservation(LocalDate from, LocalDate to) {
        final Reservation reservation = new Reservation();
        reservation.setFrom(from);
        reservation.setTo(to);
        reservation.setUser("test");

        return reservation;
    }
}
