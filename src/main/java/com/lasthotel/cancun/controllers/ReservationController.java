package com.lasthotel.cancun.controllers;

import com.lasthotel.cancun.models.Reservation;
import com.lasthotel.cancun.services.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/reservation")
@Tag(name = "Reservations", description = "Check reservation dates and maintain reservations for the hotel")
public class ReservationController {
    private final ReservationService reservationService;
    private final Clock clock;

    @Autowired
    public ReservationController(ReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;
    }


    @Operation(summary = "Get available reservation intervals")
    @ApiResponse(responseCode = "200", description = "Available reservation intervals")
    @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Reservation>> availableReservationDates() {
        return reservationService.getAvailableReservationIntervals();
    }

    @Operation(summary = "Get all reservations for a user")
    @ApiResponse(responseCode = "200", description = "Reservations found for the provided user")
    @GetMapping(path = "/user/{userName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Reservation>>> getReservationsForUser(@PathVariable String userName) {
        return reservationService.getReservationsForUser(userName)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Check if a certain date interval is available for reservation")
    @ApiResponse(responseCode = "200", description = "True if interval is available, false otherwise")
    @ApiResponse(responseCode = "400", description = "Invalid date range provided", content = @Content)
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> isReservationAvailable(@RequestParam("from")
                                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                       LocalDate from,
                                                               @RequestParam("to")
                                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                       LocalDate to) {
        if (areDatesInvalid(from, to)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return reservationService.isReservationTaken(from, to)
                .map(isTaken -> ResponseEntity.ok(isTaken ? "false" : "true"));
    }

    @Operation(summary = "Create a new reservation")
    @ApiResponse(responseCode = "200", description = "Reservation created")
    @ApiResponse(responseCode = "400", description = "Invalid dates or malformed reservation provided", content = @Content)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Reservation>> createReservation(@RequestBody Reservation reservation) {
        if (areDatesInvalid(reservation.getFrom(), reservation.getTo())) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (reservation.getUser() == null || reservation.getUser().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return reservationService.makeReservation(reservation)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @Operation(summary = "Update an existing reservation")
    @ApiResponse(responseCode = "200", description = "Reservation updated")
    @ApiResponse(responseCode = "400", description = "Invalid dates or malformed reservation provided")
    @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content)
    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Reservation>> updateReservation(@PathVariable String id, @RequestBody Reservation reservation) {
        if (areDatesInvalid(reservation.getFrom(), reservation.getTo())) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return reservationService.updateReservation(id, reservation)
                .map(updatedReservation -> {
                    if (reservation.getFrom().equals(updatedReservation.getFrom())
                            && reservation.getTo().equals(updatedReservation.getTo())) {
                        return ResponseEntity.ok(updatedReservation);
                    } else {
                        return ResponseEntity.badRequest().body(updatedReservation);
                    }
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete an existing reservation")
    @ApiResponse(responseCode = "200", description = "Reservation deleted")
    @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content)
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Reservation>> deleteReservation(@PathVariable String id) {
        return reservationService.deleteReservation(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private boolean areDatesInvalid(LocalDate from, LocalDate to) {
        return (from == null || to == null)
                || to.isBefore(from)
                || to.isAfter(LocalDate.now(clock).plusDays(30))
                || from.isBefore(LocalDate.now(clock).plusDays(1))
                || ChronoUnit.DAYS.between(from, to) > 2;
    }

}
