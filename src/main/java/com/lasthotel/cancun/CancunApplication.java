package com.lasthotel.cancun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class CancunApplication {

    public static void main(String[] args) {
        SpringApplication.run(CancunApplication.class, args);
    }

}
