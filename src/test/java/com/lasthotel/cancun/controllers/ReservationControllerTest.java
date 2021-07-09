package com.lasthotel.cancun.controllers;

import com.lasthotel.cancun.AppConfigTest;
import com.lasthotel.cancun.models.Reservation;
import com.lasthotel.cancun.repositories.ReservationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = AppConfigTest.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReservationControllerTest {
    @Autowired
    private ReservationRepository repository;

    @Autowired
    private WebTestClient webClient;

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
    public void when_datesAreInvalid_then_returnsBadRequest() {
        final var dates = List.of(
                Pair.of("", ""),
                Pair.of("2021-07-19", "2021-07-18"),
                Pair.of("2021-07-18", "2021-08-06"),
                Pair.of("2021-07-06", "2021-07-07"),
                Pair.of("2021-07-18", "2021-07-21")
        );

        for (Pair<String, String> date : dates) {
            webClient.get().uri(uriBuilder -> uriBuilder.path("reservation")
                    .queryParam("from", date.getLeft())
                    .queryParam("to", date.getRight())
                    .build())
                    .accept(MediaType.TEXT_PLAIN)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Test
    public void when_reservationIsTaken_then_returnsFalse() {
        final var dates = List.of(
                Pair.of("2021-07-07", "2021-07-09"),
                Pair.of("2021-07-09", "2021-07-11"),
                Pair.of("2021-07-09", "2021-07-09")
        );

        for (Pair<String, String> date : dates) {
            webClient.get().uri(uriBuilder -> uriBuilder.path("reservation")
                    .queryParam("from", date.getLeft())
                    .queryParam("to", date.getRight())
                    .build())
                    .accept(MediaType.TEXT_PLAIN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class).isEqualTo("false");
        }
    }

    @Test
    public void when_reservationIsAvailable_then_returnsTrue() {
        webClient.get().uri(uriBuilder -> uriBuilder.path("reservation")
                .queryParam("from", "2021-07-18")
                .queryParam("to", "2021-07-20")
                .build())
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("true");

    }

    @Test
    public void when_gettingReservationsForUser_then_returnsOnlyUsersReservations() {
        final String json1 = "[{\"id\":\"reservation2\",\"user\":\"test\",\"from\":\"2021-07-12\",\"to\":\"2021-07-14\"},{\"id\":\"reservation3\",\"user\":\"test\",\"from\":\"2021-07-17\",\"to\":\"2021-07-17\"},{\"id\":\"reservation1\",\"user\":\"test\",\"from\":\"2021-07-08\",\"to\":\"2021-07-10\"}]";
        final String json2 = "[{\"id\":\"reservation4\",\"user\":\"anotherUser\",\"from\":\"2021-07-16\",\"to\":\"2021-07-16\"}]";

        webClient.get().uri(uriBuilder -> uriBuilder.path("reservation/user/test").build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(json1);

        webClient.get().uri(uriBuilder -> uriBuilder.path("reservation/user/anotherUser").build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(json2);

        webClient.get().uri(uriBuilder -> uriBuilder.path("reservation/user/none").build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");

        webClient.get().uri(uriBuilder -> uriBuilder.path("reservation/user/").build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void when_makingReservationWithAvailableDate_then_reservationMade() {
        final String json = "{\n" +
                "  \"id\": \"reservation5\",\n" +
                "  \"from\": \"2021-07-19\",\n" +
                "  \"to\": \"2021-07-21\",\n" +
                "  \"user\": \"test1\"\n" +
                "}";

        final Reservation reservation = getReservation(LocalDate.of(2021, 7, 19), LocalDate.of(2021, 7, 21));
        reservation.setId("reservation5");
        reservation.setUser("test1");

        webClient.post().uri(uriBuilder -> uriBuilder.path("reservation").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(reservation), Reservation.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(json);
    }

    @Test
    public void when_makingReservationWithMalformedData_then_badRequest() {
        final Reservation reservation = getReservation(LocalDate.of(2021, 7, 19), LocalDate.of(2021, 7, 21));
        reservation.setId("reservation5");
        reservation.setUser("");

        webClient.post().uri(uriBuilder -> uriBuilder.path("reservation").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(reservation), Reservation.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void when_makingReservationWithInvalidDates_then_badRequest() {
        final Reservation reservation = getReservation(LocalDate.of(2021, 7, 21), LocalDate.of(2021, 7, 19));

        webClient.post().uri(uriBuilder -> uriBuilder.path("reservation/").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(reservation), Reservation.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void when_updateReservationWithInvalidDates_then_badRequest() {
        final Reservation reservation1 = getReservation(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 10));
        final Reservation updated = getReservation(LocalDate.of(2021, 7, 10), LocalDate.of(2021, 7, 12));
        final Reservation invalid = getReservation(LocalDate.of(2021, 7, 21), LocalDate.of(2021, 7, 19));
        reservation1.setId("reservation1");
        updated.setId("reservation1");
        invalid.setId("reservation1");

        webClient.put().uri(uriBuilder -> uriBuilder.path("reservation/reservation1").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(updated), Reservation.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Reservation.class).isEqualTo(reservation1);

        webClient.put().uri(uriBuilder -> uriBuilder.path("reservation/reservation1").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(invalid), Reservation.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void when_updateReservationWithValidDates_then_returnsUpdatedReservation() {
        final Reservation updated = getReservation(LocalDate.of(2021, 7, 19), LocalDate.of(2021, 7, 21));
        updated.setId("reservation1");

        webClient.put().uri(uriBuilder -> uriBuilder.path("reservation/reservation1").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(updated), Reservation.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Reservation.class).isEqualTo(updated);
    }

    @Test
    public void when_updateNonExistentReservation_then_returnsNotFound() {
        final Reservation updated = getReservation(LocalDate.of(2021, 7, 19), LocalDate.of(2021, 7, 21));
        updated.setId("reservation12341243");

        webClient.put().uri(uriBuilder -> uriBuilder.path("reservation/reservation12341243").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(updated), Reservation.class)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void when_deletingNonExistentReservation_then_returnsNotFound() {
        webClient.delete().uri(uriBuilder -> uriBuilder.path("reservation/reservation12341243").build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void when_deletingReservation_then_returnsDeletedReservation() {
        final Reservation reservation1 = getReservation(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 10));
        reservation1.setId("reservation1");

        webClient.delete().uri(uriBuilder -> uriBuilder.path("reservation/reservation1").build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Reservation.class).isEqualTo(reservation1);
    }

    private Reservation getReservation(LocalDate from, LocalDate to) {
        final Reservation reservation = new Reservation();
        reservation.setFrom(from);
        reservation.setTo(to);
        reservation.setUser("test");

        return reservation;
    }
}
