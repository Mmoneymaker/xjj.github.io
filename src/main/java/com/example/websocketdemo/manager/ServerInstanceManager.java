package com.example.websocketdemo.manager;

//负责处理每个服务器实例的地址

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Getter
@Component
public class ServerInstanceManager {

    private final String address;
    private final int port;
    private final String instanceId;

    @Autowired
    public ServerInstanceManager(
            @Value("${server.address:0.0.0.0}") String address,
            @Value("${server.port:8080}") int port) {

        this.address = address;
        this.port = port;

        String actualHost = "0.0.0.0".equals(address) ? getRealAddress() : address;
        this.instanceId = String.format("%s:%d", actualHost, port);
    }

    public String getRealAddress(){
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1"; // 回退地址
        }
    }
}
