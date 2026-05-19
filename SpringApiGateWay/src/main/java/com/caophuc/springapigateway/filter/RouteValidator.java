package com.caophuc.springapigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // Danh sách các endpoint mở, không yêu cầu token
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/public",
            "/eureka",
            // Thêm API Webhook của MoMo vào danh sách không cần Token
            "/api/payments/payments/momo/success"
    );

    // Predicate kiểm tra xem request hiện tại có nằm trong danh sách mở hay không
    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}