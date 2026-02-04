//package io.github.atengk.ai.config;
//
//import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
//import org.apache.hc.client5.http.impl.classic.HttpClients;
//import org.apache.hc.core5.http.HttpHost;
//import org.springframework.context.annotation.Bean;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.web.client.RestClient;
//
////@Configuration
//public class RestClientProxyConfig {
//
//    @Bean
//    RestClient.Builder restClientBuilder() {
//        HttpHost proxy = new HttpHost("http", "127.0.0.1", 7890);
//
//        CloseableHttpClient httpClient = HttpClients.custom()
//                .setProxy(proxy)
//                .build();
//
//        HttpComponentsClientHttpRequestFactory factory =
//                new HttpComponentsClientHttpRequestFactory(httpClient);
//
//        return RestClient.builder()
//                .requestFactory(factory);
//    }
//}
//
