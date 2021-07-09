package com.lasthotel.cancun.repositories;

import com.lasthotel.cancun.models.Reservation;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface CustomReservationRepository {
    Flux<Reservation> findOverlappingReservations(LocalDate from, LocalDate to);
}
