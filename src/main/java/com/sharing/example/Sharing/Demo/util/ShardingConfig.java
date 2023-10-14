package com.sharing.example.Sharing.Demo.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;

@Configuration(enforceUniqueMethods = false)
public class ShardingConfig {

    @Bean
    public ConsistentHashing consistentHashing(int numberOfReplica) {
        return new ConsistentHashing(numberOfReplica);
    }

    @Bean
    public ConsistentHashing consistentHashing() {
        return new ConsistentHashing(3);
    }
}
