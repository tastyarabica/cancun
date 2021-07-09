package com.lasthotel.cancun.repositories;

import com.lasthotel.cancun.models.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class CustomReservationRepositoryImpl implements CustomReservationRepository {
    private final ReactiveMongoTemplate mongoTemplate;

    @Autowired
    public CustomReservationRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<Reservation> findOverlappingReservations(LocalDate from, LocalDate to) {
        return mongoTemplate.find(
                query(
                        where("from").lte(to).and("to").gte(from)
                ), Reservation.class);
    }
}
