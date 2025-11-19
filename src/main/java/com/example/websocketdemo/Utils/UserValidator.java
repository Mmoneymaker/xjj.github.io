package com.example.websocketdemo.Utils;

import com.example.websocketdemo.DTO.User;
import io.micrometer.common.util.StringUtils;

public class UserValidator {

    public static boolean isInvalid(User user) {
        return StringUtils.isBlank(user.getUsername())||StringUtils.isBlank(user.getPassword());
    }
}
