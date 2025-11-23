//package com.project.unifiedMarketingGateway.connectors;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ReactiveHttpOutputMessage;
//import org.springframework.http.client.reactive.ClientHttpResponse;
//import org.springframework.web.reactive.function.BodyExtractors;
//import org.springframework.web.reactive.function.BodyInserter;
//import org.springframework.web.reactive.function.BodyInserters;
//import org.springframework.web.reactive.function.client.ClientRequest;
//import org.springframework.web.reactive.function.client.ClientResponse;
//import org.springframework.web.reactive.function.client.ExchangeFunction;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.nio.charset.StandardCharsets;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.http.HttpStatus.BAD_REQUEST;
//import static org.springframework.http.HttpStatus.OK;
//
//@ExtendWith(MockitoExtension.class)
//class TelegramHttpConnectorTest {
//
//    private TelegramHttpConnector connector;
//    private String baseUrl = "https://api.telegram.org";
//    private String botToken = "123456:ABCDEF";
//
//    @BeforeEach
//    void setUp() throws Exception {
//        connector = new TelegramHttpConnector(baseUrl);
//        Field botTokenField = TelegramHttpConnector.class.getDeclaredField("botToken");
//        botTokenField.setAccessible(true);
//        botTokenField.set(connector, botToken);
//    }
//
//    @Test
//    void testSendMarketingRequest_Success() throws Exception {
//        // Arrange
//        String method = "sendMessage";
//        Map<String, Object> payload = Map.of("chat_id", "123456", "text", "Hello");
//        String expectedResponse = "{\"ok\":true}";
//        ExchangeFunction exchangeFunction = (request, context) -> {
//            // verify request url if needed
//            String expectedUri = baseUrl + "/bot" + botToken + "/" + method;
//            assertEquals(expectedUri, request.url().toString());
//
//            // create a JSON body as a Publisher<String>
//            Mono<String> jsonMono = Mono.just("{\"ok\":true,\"result\":{}}");
//
//            // create a BodyInserter whose Publisher type matches what ClientResponse.Builder expects
//            BodyInserter<Mono<String>, ReactiveHttpOutputMessage> inserter =
//                    BodyInserters.fromPublisher(jsonMono, String.class);
//
//            ClientResponse response = ClientResponse.create(HttpStatus.OK)
//                    .headers(h -> h.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
//                    .body(inserter)   // now the generics match
//                    .build();
//
//            return Mono.just(response);
//        };
//
//        WebClient webClient = WebClient.builder()
//                .exchangeFunction(exchangeFunction)
//                .build();
//
//        Field webClientField = TelegramHttpConnector.class.getDeclaredField("webClient");
//        webClientField.setAccessible(true);
//        webClientField.set(connector, webClient);
//
//        // Act
//        String result = connector.sendMarketingRequest(method, payload).block();
//
//        // Assert
//        assertEquals(expectedResponse, result);
//    }
//
//    @Test
//    void testSendMarketingRequest_Error() throws Exception {
//        // Arrange
//        String method = "sendMessage";
//        Map<String, Object> payload = Map.of("chat_id", "123456", "text", "Hello");
//
//        ExchangeFunction exchangeFunction = (request, context) -> {
//            return Mono.just(ClientResponse.create(OK)
//                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//                    .body(BodyExtractors.toMono(String.class))
//                    .build());
//        };
//
//        WebClient webClient = WebClient.builder()
//                .exchangeFunction(exchangeFunction)
//                .build();
//
//        Field webClientField = TelegramHttpConnector.class.getDeclaredField("webClient");
//        webClientField.setAccessible(true);
//        webClientField.set(connector, webClient);
//
//        // Act & Assert
//        assertThrows(WebClientResponseException.class, () -> {
//            connector.sendMarketingRequest(method, payload).block();
//        });
//    }
//
//    @Test
//    void testApiPath() throws Exception {
//        // Arrange
//        String method = "sendMessage";
//        Method apiPathMethod = TelegramHttpConnector.class.getDeclaredMethod("apiPath", String.class);
//        apiPathMethod.setAccessible(true);
//
//        // Act
//        String result = (String) apiPathMethod.invoke(connector, method);
//
//        // Assert
//        assertEquals("/bot" + botToken + "/" + method, result);
//    }
//
//    @Test
//    void testApiPath_DefaultToken() throws Exception {
//        // Arrange
//        String method = "sendMessage";
//        TelegramHttpConnector connector = new TelegramHttpConnector(baseUrl);
//        Method apiPathMethod = TelegramHttpConnector.class.getDeclaredMethod("apiPath", String.class);
//        apiPathMethod.setAccessible(true);
//
//        // Act
//        String result = (String) apiPathMethod.invoke(connector, method);
//
//        // Assert
//        assertEquals("/botdefaultToken/" + method, result);
//    }
//}
//
