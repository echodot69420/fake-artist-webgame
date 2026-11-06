package com.github.echodot69420.fakeartist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class FakeArtistApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FakeArtistApplication.class);
        app.setDefaultProperties(Map.of("server.port", "8000"));
        app.run(args);
    }
}
