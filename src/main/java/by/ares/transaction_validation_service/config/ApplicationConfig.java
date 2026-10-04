package by.ares.transaction_validation_service.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

import static by.ares.transaction_validation_service.util.TransactionValidationServiceConst.ZONE;

@Configuration
@EnableFeignClients(basePackages = "by.ares.transaction_validation_service")
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}