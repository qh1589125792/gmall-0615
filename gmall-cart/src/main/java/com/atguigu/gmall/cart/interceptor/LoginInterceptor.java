package com.atguigu.gmall.cart.interceptor;

import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.core.bean.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor extends HandlerInterceptorAdapter {

    private static final ThreadLocal<UserInfo> THREAD_LOCAL =  new ThreadLocal<>();
    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //theadLocal中的载荷信息
        UserInfo userInfo = new UserInfo();

        //获取cookie信息
        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request, this.jwtProperties.getUserKeyName());

        if(StringUtils.isEmpty(userKey)){
            String uuid = UUID.randomUUID().toString();
            CookieUtils.setCookie(request,response,this.jwtProperties.getUserKeyName(),uuid,this.jwtProperties.getExpire());
        }
        userInfo.setUserKey(userKey);
        if (StringUtils.isEmpty(token)){
            THREAD_LOCAL.set(userInfo);
            return true;
        }

        try {
            //解析gmall_token
            Map<String, Object> userInfoMap = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
            userInfo.setUserId(Long.valueOf(userInfoMap.get("id").toString()));

        } catch (Exception e) {
            e.printStackTrace();
        }
        THREAD_LOCAL.set(userInfo);

        return true;
    }

    public static UserInfo get(){
        return THREAD_LOCAL.get();

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
