package com.g9team04.techmind.conteudo.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClassifierRestClientConfig {

    @Bean
    RestClient ociClassifierRestClient(
            @Value("${techmind.classificador.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${techmind.api-key}") String apiKey) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // evita upgrade h2c, incompatível com uvicorn
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
    }
}
