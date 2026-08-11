package com.yawar.nextforgeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient brevoRestClient(){
        return RestClient.builder()
                .baseUrl("http://api.brevo.com")
                .build();
    }
}
