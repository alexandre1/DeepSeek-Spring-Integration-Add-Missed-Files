package com.deepseek_app.ai.deepseek_spring_integration;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final DeepSeekService deepSeekService;

    public AIController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/generate")
    public org.springframework.http.ResponseEntity<String> generateContent(@RequestBody String prompt) {
        try {
            String response = deepSeekService.generateText(prompt);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request: " + e.getMessage());
        }
    }
}