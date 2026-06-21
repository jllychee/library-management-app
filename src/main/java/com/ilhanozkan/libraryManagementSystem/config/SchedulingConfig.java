package com.ilhanozkan.libraryManagementSystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled support for periodic jobs such as the overdue notification scan.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
