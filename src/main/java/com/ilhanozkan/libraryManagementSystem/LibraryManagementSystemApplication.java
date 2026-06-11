package com.ilhanozkan.libraryManagementSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class LibraryManagementSystemApplication {

	public static void main(String[] args) {
		log.info("Starting Library Management System application...");
		SpringApplication.run(LibraryManagementSystemApplication.class, args);
		log.info("Library Management System application started successfully");
	}

}
