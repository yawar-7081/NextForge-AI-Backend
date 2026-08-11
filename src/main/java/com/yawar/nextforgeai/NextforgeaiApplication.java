package com.yawar.nextforgeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class NextforgeaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NextforgeaiApplication.class, args);
	}

}
