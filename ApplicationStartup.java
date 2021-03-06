/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pepedonato.DailyPlanetH2;

/**
 *
 * @author dr.ing. Pepe Donato
 */
import com.pepedonato.DailyPlanetH2.service.CategoriaServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup
        implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * This event is executed as late as conceivably possible to indicate that
     * the application is ready to service requests.
     *
     * @param event
     */


    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        // here your code ...
        System.out.println("ApplicationStartup");
    }

} // class
