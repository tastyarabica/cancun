# The Last Hotel in Cancun

A simple API for managing reservations for The Last Hotel, in Cancun.

## How to Run
- (Optional) Have a local MongoDB with default settings
- If you want to run with an embedded MongoDB:
    - Change the following line in `build.gradle` from:
      ```
      testImplementation 'de.flapdoodle.embed:de.flapdoodle.embed.mongo:3.0.0'
      ```
    - To:
      ```
      implementation 'de.flapdoodle.embed:de.flapdoodle.embed.mongo:3.0.0'
      ```
- `gradlew bootRun` to run, `gradlew test` to run the test suite
- Runs at `localhost:8080` by default
- There is a Swagger UI at `localhost:8080/swagger-ui.html` to test requests.
- All API documentation and expected behavior is available on the Swagger page

## Requirements
1. API will be maintained by the hotel’s IT department.
2. As it’s the very last hotel, the quality of service must be 99.99 to 100% => no downtime
3. For the purpose of the test, we assume the hotel has only one room available
4. To give a chance to everyone to book the room, the stay can’t be longer than 3 days and can’t be reserved more than 30 days in advance
5. All reservations start at least the next day of booking
6. To simplify the use case, a “DAY’ in the hotel room starts from 00:00 to 23:59:59
7. Every end-user can check the room availability, place a reservation, cancel it or modify it.
8. To simplify the API is insecure.

## Assumptions
- Requirement 4: Considered both start and end date inclusive in the 3-day interval. Also considered D+30 valid for reservations.
- Requirement 5: Interpreted as not allowing a reservation starting on the current day.

## Technologies used
- Spring (Boot, Webflux, Reactive Data MongoDB)
  - Industry standard for backend development with Java.
  - Used reactive streams via Webflux and Reactive Data to implement non-blocking, event-driven operations, both on API and DB, making it easier to scale horizontally if need be.
- MongoDB
  - Mature, production ready document-based database.
  - Used here for ease of development, though considering this is the last hotel in Cancun, Mongo is a good choice since it scales easily.
- JUnit
  - Industry standard testing library for Java.
  - Used with plugins provided by Spring to test reactive code.
- Springdoc
  - Library that bundles API documentation annotations and Swagger.
  - Useful for documenting the API via code and providing an interface to test various requests.

## Possible improvements
- Use caching on API endpoints that read data from the DB to avoid backpressure, and improve latency.
- Use automated documentation generation solutions for the API to avoid documentation becoming stale.
- Use MongoDB transactions, or switch to an RDBMS to avoid problems with concurrent reservation (creation, update, delete) requests.
- Do load testing to discover possible inefficiencies.