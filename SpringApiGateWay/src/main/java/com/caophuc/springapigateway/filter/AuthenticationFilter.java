package com.caophuc.springapigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.caophuc.springapigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // Constructor Injection
    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Kiểm tra xem route hiện tại có yêu cầu xác thực không
            if (validator.isSecured.test(request)) {
                
                // 2. Kiểm tra Header Authorization
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return onError(exchange, "Thiếu Authorization header trong request");
                }

                // 3. Trích xuất chuỗi JWT token
                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7); // Bỏ chữ "Bearer " để lấy thân Token
                } else {
                    return onError(exchange, "Định dạng Authorization header không hợp lệ");
                }

                try {
                    // 4. Xác thực Token bằng JJWT
                    jwtUtil.validateToken(authHeader);

                    // 5. Giải mã Payload Token và chuyển nó xuống các Downstream microservices thông qua Header
                    Claims claims = jwtUtil.extractAllClaims(authHeader);

                    // --- SỬA LỖI Ở ĐÂY ---
                    // Dựa vào payload thực tế của bạn, ID người dùng được lưu trong key "id" chứ không phải "userId"
                    String userId = String.valueOf(claims.get("id")); 
                    String role = String.valueOf(claims.get("role"));
                    String username = claims.getSubject(); // sub: nguyen.204@gmail.com

                    // Tạo một request mới (mutate) mang theo các custom headers này
                    request = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Name", username)
                            .header("X-User-Role", role)
                            .build();

                } catch (ExpiredJwtException e) {
                    return onError(exchange, "Token đã hết hạn vui lòng login lại");
                } catch (SignatureException e) {
                    return onError(exchange, "Chữ ký Token không hợp lệ");
                } catch (Exception e) {
                    return onError(exchange, "Không được phép truy cập");
                }
            }

            // Gửi request (đã được mutate) đến downstream service
            return chain.filter(exchange.mutate().request(request).build());
        });
    }

    /**
     * Hàm dùng để trả về chuỗi JSON lỗi ngay tại cấp độ API Gateway (luôn trả về 401 Unauthorized)
     */
    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Chuẩn bị payload báo lỗi
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
        errorDetails.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorDetails.put("message", errorMessage);
        errorDetails.put("path", exchange.getRequest().getURI().getPath());

        try {
            // Convert Map thành JSON Bytes
            byte[] bytes = objectMapper.writeValueAsBytes(errorDetails);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] bytes = "{\"message\": \"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    public static class Config {
        // Có thể để trống. Dùng khi bạn muốn truyền tham số từ file cấu hình .yml vào filter
    }
}