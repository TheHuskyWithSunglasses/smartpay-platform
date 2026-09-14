package com.smartpay.gateway.filter;

import com.smartpay.gateway.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Test
    void filter_whenPublicPath_skipsAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain);

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void filter_whenMissingAuthHeader_returnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/payments")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_whenInvalidToken_returnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalidtoken")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(jwtService.isTokenValid("invalidtoken")).thenThrow(new JwtException("invalid"));

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}