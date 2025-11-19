package com.example.websocketdemo.Utils;

public class RedisKeyUtils {
    public static final String USER_HASH = "user:";
    public static String tokenKey(String token) {
        return "user:token:" + token;
    }
}

