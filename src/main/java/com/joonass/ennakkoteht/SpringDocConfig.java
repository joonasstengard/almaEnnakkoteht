package com.joonass.ennakkoteht;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Config luokka OpenAPI dokumentaatiota varten, käyttäen SpringDoccia
@Configuration
public class SpringDocConfig {

    @Bean
    public GroupedOpenApi controllerApi() {
        return GroupedOpenApi.builder()
                .group("controller-api")
                .packagesToScan("com.joonass.ennakkoteht")
                .build();
    }

}