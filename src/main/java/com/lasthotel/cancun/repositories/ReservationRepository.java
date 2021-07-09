package com.lasthotel.cancun.repositories;

import com.lasthotel.cancun.models.Reservation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface ReservationRepository extends ReactiveMongoRepository<Reservation, String>, CustomReservationRepository {
    Flux<Reservation> findAllByUser(String user);

    Flux<Reservation> findAllByToAfter(LocalDate date);
}
