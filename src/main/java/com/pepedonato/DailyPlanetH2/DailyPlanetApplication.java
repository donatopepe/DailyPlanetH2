package com.pepedonato.DailyPlanetH2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class DailyPlanetApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyPlanetApplication.class, args);
        System.out.println("SpringApplication.run");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        System.out.println("doSomethingAfterStartup");
    }

}
