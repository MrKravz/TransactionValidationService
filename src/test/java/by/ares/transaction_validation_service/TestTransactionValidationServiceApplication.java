package by.ares.transaction_validation_service;

import org.springframework.boot.SpringApplication;

public class TestTransactionValidationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TransactionValidationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
