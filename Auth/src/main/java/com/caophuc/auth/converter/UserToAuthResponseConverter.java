package com.caophuc.auth.converter;

import com.caophuc.auth.dto.AuthResponse;
import com.caophuc.auth.model.User;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UserToAuthResponseConverter implements Converter<User, AuthResponse> {

    @Override
    public AuthResponse convert(User source) {
        if (source == null) {
            return null;
        }

        AuthResponse target = new AuthResponse();
        // Copy email, fullName, role từ User sang AuthResponse
        BeanUtils.copyProperties(source, target);
        
        return target;
    }
}
