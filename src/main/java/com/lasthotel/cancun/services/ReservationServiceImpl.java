package com.lasthotel.cancun.services;

import com.lasthotel.cancun.models.Reservation;
import com.lasthotel.cancun.repositories.ReservationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public ReservationServiceImpl(ReservationRepository reservationRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Override
    public Mono<Boolean> isReservationTaken(LocalDate from, LocalDate to) {
        return reservationRepository
                .findOverlappingReservations(from, to)
                .collectList()
                .map(reservations -> !reservations.isEmpty());
    }

    @Override
    public Mono<List<Reservation>> getAvailableReservationIntervals() {
        final List<Reservation> availableReservations = new ArrayList<>();
        return getAllReservations()
                .index()
                .reduce((first, second) -> {
                    final Reservation curr = first.getT2();
                    final Reservation next = second.getT2();

                    // Special treatment in case the first reservation starts later than tomorrow
                    if (first.getT1() == 0 && ChronoUnit.DAYS.between(LocalDate.now(clock).plusDays(1), curr.getFrom()) > 0) {
                        final var reservation = new Reservation();
                        reservation.setFrom(LocalDate.now(clock).plusDays(1));
                        reservation.setTo(curr.getFrom().minusDays(1));
                        reservation.setUser("available");
                        availableReservations.add(reservation);
                    }

                    if (ChronoUnit.DAYS.between(curr.getTo(), next.getFrom()) > 1) {
                        final var reservation = new Reservation();
                        reservation.setFrom(curr.getTo().plusDays(1));
                        reservation.setTo(next.getFrom().minusDays(1));
                        reservation.setUser("available");
                        availableReservations.add(reservation);
                    }

                    return second;
                })
                .flatMap(last -> {
                    final Reservation lastReservation = last.getT2();

                    // Need to treat this here as well, since we can have a collection with 1 element
                    if (last.getT1() == 0 && ChronoUnit.DAYS.between(LocalDate.now(clock).plusDays(1), lastReservation.getFrom()) > 0) {
                        final var reservation = new Reservation();
                        reservation.setFrom(LocalDate.now(clock).plusDays(1));
                        reservation.setTo(lastReservation.getFrom().minusDays(1));
                        reservation.setUser("available");
                        availableReservations.add(reservation);
                    }

                    if (ChronoUnit.DAYS.between(lastReservation.getTo(), LocalDate.now().plusDays(30)) > 1) {
                        final var reservation = new Reservation();
                        reservation.setFrom(lastReservation.getTo().plusDays(1));
                        reservation.setTo(LocalDate.now(clock).plusDays(30));
                        reservation.setUser("available");
                        availableReservations.add(reservation);
                    }
                    return Mono.just(availableReservations);
                })
                .defaultIfEmpty(
                        List.of(new Reservation("available", LocalDate.now(clock).plusDays(1), LocalDate.now(clock).plusDays(30)))
                );
    }

    @Override
    public Mono<Reservation> makeReservation(Reservation reservation) {
        return isReservationTaken(reservation.getFrom(), reservation.getTo())
                .flatMap(isTaken -> {
                    if (isTaken) {
                        return Mono.empty();
                    } else {
                        return reservationRepository.save(reservation);
                    }
                });
    }

    @Override
    public Mono<Reservation> updateReservation(String id, Reservation reservation) {
        return deleteReservation(id)
                .flatMap(deletedReservation -> {
                    final var updated = new Reservation();
                    updated.setId(deletedReservation.getId());
                    updated.setUser(deletedReservation.getUser());
                    updated.setFrom(reservation.getFrom());
                    updated.setTo(reservation.getTo());

                    return Mono.zip(Mono.just(deletedReservation), Mono.just(updated), isReservationTaken(updated.getFrom(), updated.getTo()));
                })
                .flatMap(tuple -> {
                    if (tuple.getT3()) {
                        return reservationRepository.save(tuple.getT1());
                    } else {
                        return reservationRepository.save(tuple.getT2());
                    }
                });
    }

    @Override
    public Mono<Reservation> deleteReservation(String id) {
        return reservationRepository
                .findById(id)
                .flatMap(reservation ->
                        reservationRepository.delete(reservation).then(Mono.just(reservation))
                );
    }

    @Override
    public Flux<Reservation> getReservationsForUser(String user) {
        return reservationRepository.findAllByUser(user);
    }

    @Override
    public Mono<Reservation> getReservationById(String id) {
        return reservationRepository.findById(id);
    }

    @Override
    public Flux<Reservation> getAllReservations() {
        return reservationRepository.findAllByToAfter(LocalDate.now(clock)).sort(Comparator.comparing(Reservation::getTo));
    }
}
