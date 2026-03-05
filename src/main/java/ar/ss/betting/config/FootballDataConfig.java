package ar.ss.betting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// RestClient with default header, api token as env var in bashrc, footballdata free api
@Configuration
public class FootballDataConfig {

    @Bean
    public RestClient footballDataRestClient(
            @Value("${footballdata.base-url}") String baseUrl,
            @Value("${footballdata.token}") String token
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing football-data token. Set env var FOOTBALL_DATA_TOKEN.");
        }

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Auth-Token", token)
                .build();
    }
}