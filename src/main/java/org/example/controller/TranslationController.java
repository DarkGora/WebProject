package org.example.controller;

import com.example.translation.properties.TranslatorProperties;
import com.example.translation.service.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/translation")
public class TranslationController {

    @GetMapping
    public ResponseEntity<String> translate(@RequestParam String text) {
        TranslatorProperties properties = new TranslatorProperties();
        properties.setLanguageFrom("RU");
        properties.setLanguageTo("PL");
        RestTemplate restTemplate = new RestTemplate();
        TranslationService service = new TranslationService(properties, restTemplate);
        String translation = service.translate(text);
        return ResponseEntity.ok(translation);
    }
}