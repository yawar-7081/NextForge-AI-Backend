package com.yawar.nextforgeai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "backblaze.b2")
public class BackblazeB2Properties {

    private String endpoint;
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
}
