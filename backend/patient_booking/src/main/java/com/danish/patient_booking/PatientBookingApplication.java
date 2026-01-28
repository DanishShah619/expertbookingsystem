package com.danish.patient_booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PatientBookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientBookingApplication.class, args);
	}

}
