package com.github.echodot69420.fakeartist;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
class PageController {
    @GetMapping("/")
    ResponseEntity<Resource> homePage() {
        return ResponseEntity.ok(new FileSystemResource(Path.of("index.html")));
    }

    @GetMapping("/lobby")
    ResponseEntity<Resource> lobbyPage() {
        return ResponseEntity.ok(new FileSystemResource(Path.of("lobby.html")));
    }
}
