package com.clearticket.clearticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClearTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearTicketApplication.class, args);
    }

}
