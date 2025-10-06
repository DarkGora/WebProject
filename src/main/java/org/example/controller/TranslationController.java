package org.example.controller;

import com.example.translation.properties.TranslatorProperties;
import com.example.translation.service.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;


@RestController
@RequestMapping("/translation")
public class TranslationController {

    private final RestTemplate restTemplate;

    public TranslationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<String> translate(@RequestParam String text) {
        TranslatorProperties properties = new TranslatorProperties();
        properties.setLanguageFrom("ru");
        properties.setLanguageTo("pl");
        TranslationService service = new TranslationService(properties,restTemplate);
        String translation = service.translate(text);
        return ResponseEntity.ok(translation);
    }
}