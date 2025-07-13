package umc.cockple.demo.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DockerController {

    @GetMapping("/")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("도커테스트");
    }
}
