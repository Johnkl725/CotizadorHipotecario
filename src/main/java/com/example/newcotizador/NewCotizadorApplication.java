package com.example.newcotizador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NewCotizadorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewCotizadorApplication.class, args);
    }

}
