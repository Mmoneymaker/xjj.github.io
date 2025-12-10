package com.example.websocketdemo.manager;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.management.ManagementFactory;

@Getter
@Component
public class ServerInstanceManager {

    @Value("${server.address:0.0.0.0}")
    private String address;

    @Value("${server.port:8080}")
    private int port;

    @Autowired
    private Environment environment;

    private String instanceId;

    @PostConstruct
    public void init() {
        // 获取实际运行的端口（从Environment获取，这样能读取到命令行参数覆盖的端口）
        int actualPort = environment.getProperty("server.port", Integer.class, port);

        // 获取主机IP
        String hostIp = getRealAddress();

        // 获取进程ID确保唯一性
        String pid = getProcessId();

        // 实例ID格式：IP:PORT-PID (例如: 10.49.6.8:8080-12345)
        this.instanceId = String.format("%s:%d", hostIp, actualPort);

        System.out.println("🚀 实例ID初始化: " + instanceId);
        System.out.println("📍 监听地址: " + address + ":" + actualPort);
        System.out.println("🔢 进程ID: " + pid);
    }

    public String getRealAddress() {
        try {
            // 如果是0.0.0.0，尝试获取真实的IP地址
            if ("0.0.0.0".equals(address)) {
                InetAddress localHost = InetAddress.getLocalHost();
                return localHost.getHostAddress();
            }
            return address;
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * 获取当前进程ID
     */
    private String getProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return jvmName.split("@")[0];
    }

    /**
     * 获取可用于HTTP请求的基础URL (不包含进程ID)
     */
    public String getBaseUrl() {
        int actualPort = environment.getProperty("server.port", Integer.class, port);
        return String.format("http://%s:%d", getRealAddress(), actualPort);
    }

    /**
     * 获取实际运行的端口
     */
    public int getActualPort() {
        return environment.getProperty("server.port", Integer.class, port);
    }
}