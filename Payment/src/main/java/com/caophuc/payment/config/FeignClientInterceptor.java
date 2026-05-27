package com.caophuc.payment.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // 1. Lấy ra Request hiện tại (request gọi vào Payment)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 2. Trích xuất Header X-User-Id và X-User-Role (nếu có)
            String userId = request.getHeader("X-User-Id");
            String userRole = request.getHeader("X-User-Role");

            // 3. Nhét Header vào Request chuẩn bị gửi đi sang service khác
            if (userId != null) {
                requestTemplate.header("X-User-Id", userId);
                log.info("Đã đính kèm Header X-User-Id: {} vào FeignClient", userId);
            }
            if (userRole != null) {
                requestTemplate.header("X-User-Role", userRole);
            }
        } else {
            log.warn("Không tìm thấy Request Context. Header có thể sẽ bị rớt!");
        }
    }
}
