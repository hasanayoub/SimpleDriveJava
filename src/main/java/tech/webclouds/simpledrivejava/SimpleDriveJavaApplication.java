package tech.webclouds.simpledrivejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class SimpleDriveJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleDriveJavaApplication.class, args);
    }

}
