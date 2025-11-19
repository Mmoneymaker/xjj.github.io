package com.example.websocketdemo.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

//import javax.servlet.*;
//import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RegisterFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response=(HttpServletResponse) servletResponse;
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setHeader("TestFilter","ok");
        filterChain.doFilter(servletRequest, response);
    }
}
