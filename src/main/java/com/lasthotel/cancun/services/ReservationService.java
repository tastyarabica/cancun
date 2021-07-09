package com.lasthotel.cancun.services;

import com.lasthotel.cancun.models.Reservation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService {
    Mono<Boolean> isReservationTaken(LocalDate from, LocalDate to);

    Mono<List<Reservation>> getAvailableReservationIntervals();

    Mono<Reservation> makeReservation(Reservation reservation);

    Mono<Reservation> updateReservation(String id, Reservation reservation);

    Mono<Reservation> deleteReservation(String id);

    Flux<Reservation> getReservationsForUser(String user);

    Mono<Reservation> getReservationById(String id);

    Flux<Reservation> getAllReservations();
}
