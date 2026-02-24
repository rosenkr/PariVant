package ar.ss.betting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point.
 *
 * This bootstraps the embedded server and component scanning
 * for packages under ar.ss.betting.*.
 */
@SpringBootApplication
public class BettingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BettingApplication.class, args);
    }
}