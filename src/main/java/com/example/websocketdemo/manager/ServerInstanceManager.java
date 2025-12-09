package com.example.websocketdemo.manager;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Getter
@Component
public class ServerInstanceManager {

    @Value("${server.address:0.0.0.0}")
    private String address;

    @Value("${server.port:8080}")
    private int port;

    private String instanceId;

    @PostConstruct
    public void init() {
        // @PostConstruct 在所有依赖注入完成后执行
        // 此时命令行参数已经加载
        String actualHost = "0.0.0.0".equals(address) ? getRealAddress() : address;
        this.instanceId = String.format("%s:%d", actualHost, port);
        System.out.println("实例ID初始化: " + instanceId);
    }

    public String getRealAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}