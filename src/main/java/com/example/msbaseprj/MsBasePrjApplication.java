package com.example.msbaseprj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.msbaseprj.api.configuration.client.ClientProperties;

@SpringBootApplication
@EnableConfigurationProperties(value = {ClientProperties.class})
@EnableScheduling
public class MsBasePrjApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsBasePrjApplication.class, args);
	}

}
