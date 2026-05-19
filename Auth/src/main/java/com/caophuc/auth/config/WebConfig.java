package com.caophuc.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.caophuc.auth.converter.RegisterRequestToUserConverter;
import com.caophuc.auth.converter.UserToAuthResponseConverter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RegisterRequestToUserConverter registerRequestToUserConverter;
    private final UserToAuthResponseConverter userToAuthResponseConverter;

    public WebConfig(RegisterRequestToUserConverter registerRequestToUserConverter,
                     UserToAuthResponseConverter userToAuthResponseConverter) {
        this.registerRequestToUserConverter = registerRequestToUserConverter;
        this.userToAuthResponseConverter = userToAuthResponseConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(registerRequestToUserConverter);
        registry.addConverter(userToAuthResponseConverter);
    }
}
