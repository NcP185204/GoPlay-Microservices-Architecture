package com.caophuc.auth.converter;

import com.caophuc.auth.dto.RegisterRequest;
import com.caophuc.auth.model.User;
import com.caophuc.auth.util.UserRole;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RegisterRequestToUserConverter implements Converter<RegisterRequest, User> {

    @Override
    public User convert(RegisterRequest source) {
        if (source == null) {
            return null;
        }

        User target = new User();
        // Copy các trường có tên giống nhau (fullName, email, phoneNumber)
        // Mật khẩu sẽ được mã hóa bên Service nên ta cũng có thể copy tạm hoặc bỏ qua, nhưng mặc định sẽ copy thẳng
        BeanUtils.copyProperties(source, target);
        
        // Mặc định gán Role là PLAYER khi đăng ký mới
        target.setRole(UserRole.PLAYER);
        
        return target;
    }
}
