package com.example.websocketdemo.Service;

import com.example.websocketdemo.Utils.TokenUtils;
import com.example.websocketdemo.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.token.TokenService;
import org.springframework.stereotype.Service;

@Service
public class TokenValidateService {

    @Autowired
    private UserCacheService userCacheService;

    @Autowired
    private TokenUtils tokenUtils;

    //从jwt里面取用户名，与redis里面存储的作对比
    public boolean validateToken(String token) {
        if (token == null||token.isEmpty()) {
            return false;
        }
        try{
            if(!tokenUtils.validateTokenFormat(token)){
                return false;
            }
            String username = tokenUtils.getUsernameFromToken(token);
            return userCacheService.isValidToken(token,username);
        }catch (Exception e){
            throw new BusinessException(401,e.getMessage());
        }


    }
}
