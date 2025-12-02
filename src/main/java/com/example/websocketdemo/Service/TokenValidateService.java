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
    public String validateTokenAndGetUsername(String token) {
        if (token == null||token.isEmpty()) {
            return null;
        }
        try{
            //检验token是否符合jwt令牌格式
            if(!tokenUtils.validateJwtTokenFormat(token)){
                return null;
            }
            //从JwtUtil解析token获取用户名
            String username = tokenUtils.getUsernameFromJwtToken(token);
            //把解析的与redis存的做对比
            return userCacheService.isValidJwtToken(token,username)?username:null;
        }catch (Exception e){
            throw new BusinessException(401,e.getMessage());
        }
    }
}
