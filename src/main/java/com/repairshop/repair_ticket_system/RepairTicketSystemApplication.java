package com.repairshop.repair_ticket_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RepairTicketSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepairTicketSystemApplication.class, args);
	}

}
