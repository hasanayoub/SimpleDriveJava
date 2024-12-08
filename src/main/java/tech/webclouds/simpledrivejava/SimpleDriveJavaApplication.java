package tech.webclouds.simpledrivejava;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tech.webclouds.simpledrivejava.configs.ApplicationProperties;

@OpenAPIDefinition(info = @Info(title = "Simple Drive API", version = "1.0", description = "API documentation for Simple Drive project"))
@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class SimpleDriveJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleDriveJavaApplication.class, args);
    }

}
