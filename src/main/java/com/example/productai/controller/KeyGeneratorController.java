package com.example.productai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/generate-key")
public class KeyGeneratorController {

    private static final SecureRandom secureRandom = new SecureRandom();

    @GetMapping
    public Map<String, String> generateKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        return Map.of(
                "key", key,
                "uuid", UUID.randomUUID().toString(),
                "algorithm", "SecureRandom-Base64"
        );
    }
}
