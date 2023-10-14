package com.sharing.example.Sharing.Demo.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@Getter
@Setter
public class DatabaseConfig {
    private List<String> urls;
    private String userName;
    private String password;
    private String driver;


}
