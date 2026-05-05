package com.example.productai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST pour la génération de clés aléatoires sécurisées.
 *
 * <p>Expose l'endpoint {@code GET /generate-key} qui génère une clé
 * cryptographiquement sûre via {@link SecureRandom} et l'encode en Base64.</p>
 *
 * @author Elkamel Fahmi
 * @version 1.0.0
 */
@RestController
@RequestMapping("/generate-key")
@Tag(name = "Key Generator", description = "API de génération de clés aléatoires sécurisées")
public class KeyGeneratorController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int KEY_BYTE_LENGTH = 32;

    /**
     * Génère et retourne une clé aléatoire sécurisée.
     *
     * <p>La clé est générée via {@link SecureRandom} (256 bits) et encodée
     * en Base64 URL-safe. Un UUID v4 est également retourné en complément.</p>
     *
     * @return une {@link Map} contenant :
     *         <ul>
     *           <li>{@code key} : clé aléatoire encodée Base64 (256 bits)</li>
     *           <li>{@code uuid} : identifiant unique UUID v4</li>
     *           <li>{@code algorithm} : algorithme utilisé</li>
     *         </ul>
     */
    @GetMapping
    @Operation(
        summary = "Générer une clé aléatoire",
        description = "Génère une clé cryptographiquement sécurisée (256 bits) via SecureRandom encodée en Base64, accompagnée d'un UUID v4 unique.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Clé générée avec succès",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(value = """
                        {
                          "key": "xK9mP2qL8nT5vR1wA3bC6dE0fG4hI7jM",
                          "uuid": "550e8400-e29b-41d4-a716-446655440000",
                          "algorithm": "SecureRandom-Base64"
                        }
                        """)
                )
            )
        }
    )
    public Map<String, String> generateKey() {
        byte[] randomBytes = new byte[KEY_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        return Map.of(
                "key", key,
                "uuid", UUID.randomUUID().toString(),
                "algorithm", "SecureRandom-Base64"
        );
    }
}
