package com.GetMyGraphicsCard.productservice.service;

import com.GetMyGraphicsCard.productservice.entity.Root;
import com.GetMyGraphicsCard.productservice.service.WebClientServiceImpl;
import com.google.gson.Gson;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource("/application-test.yaml")
@SpringBootTest
public class WebClientServiceTest {

    @Autowired
    private WebClientServiceImpl webClientServiceImpl;

    @Autowired
    private Gson gson;
    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUpMockWebServer() throws IOException {
        mockWebServer = new MockWebServer();

        mockWebServer.start(6000);
    }

    @Test
    void getProductsTest() throws IOException {
        ClassPathResource naverResource = new ClassPathResource("NaverShop.json");
        String NaverString = IOUtils.toString(naverResource.getInputStream(), StandardCharsets.UTF_8);
        Root NaverRoot = gson.fromJson(NaverString, Root.class);
        Mono<Root> NaverProducts = Mono.just(NaverRoot);
        Mono<Root> TestProducts = webClientServiceImpl.getProducts();
        System.out.println(NaverProducts.block());
        System.out.println(TestProducts.block());
        assertEquals(NaverProducts, TestProducts, "Connection Test");
    }
    @AfterAll
    static void removeWebServer() throws IOException {
        mockWebServer.shutdown();
    }



}
