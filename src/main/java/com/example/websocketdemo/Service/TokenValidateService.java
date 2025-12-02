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
    public String validateJwtToken(String token) {
        if (token == null||token.isEmpty()) {
            return null;
        }
        try{
            if(!tokenUtils.validateJwtTokenFormat(token)){
                return null;
            }
            //jwt解析
            String username = tokenUtils.getUsernameFromJwtToken(token);
            //从redis取

            userCacheService.isValidJwtToken(token,username);
        }catch (Exception e){
            throw new BusinessException(401,e.getMessage());
        }


    }
}
