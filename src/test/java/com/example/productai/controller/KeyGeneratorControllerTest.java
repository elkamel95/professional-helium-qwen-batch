package com.example.productai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour {@link KeyGeneratorController}.
 *
 * <p>Vérifie le comportement de l'endpoint {@code GET /generate-key} :</p>
 * <ul>
 *   <li>Présence et format des champs retournés</li>
 *   <li>Unicité des clés générées</li>
 *   <li>Validité de l'UUID</li>
 *   <li>Algorithme utilisé</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(KeyGeneratorController.class)
@DisplayName("KeyGeneratorController — Tests unitaires")
class KeyGeneratorControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    private Map<String, String> response;

    @BeforeEach
    void setUp() {
        response = webTestClient.get()
                .uri("/generate-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    @DisplayName("L'endpoint retourne un statut HTTP 200")
    void shouldReturn200() {
        webTestClient.get()
                .uri("/generate-key")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("La réponse contient le champ 'key'")
    void shouldContainKeyField() {
        assertThat(response).containsKey("key");
        assertThat(response.get("key")).isNotBlank();
    }

    @Test
    @DisplayName("La clé générée a une longueur suffisante (>= 40 caractères)")
    void keyShouldHaveSufficientLength() {
        String key = response.get("key");
        assertThat(key).hasSizeGreaterThanOrEqualTo(40);
    }

    @Test
    @DisplayName("La réponse contient le champ 'uuid' valide")
    void shouldContainValidUuid() {
        String uuid = response.get("uuid");
        assertThat(uuid).isNotBlank();
        assertThat(uuid).matches(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
        );
    }

    @Test
    @DisplayName("La réponse contient le champ 'algorithm'")
    void shouldContainAlgorithmField() {
        assertThat(response).containsKey("algorithm");
        assertThat(response.get("algorithm")).isEqualTo("SecureRandom-Base64");
    }

    @RepeatedTest(10)
    @DisplayName("Chaque appel génère une clé différente (unicité)")
    void keysShouldBeUnique() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Map<String, String> resp = webTestClient.get()
                    .uri("/generate-key")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Map.class)
                    .returnResult()
                    .getResponseBody();
            keys.add(resp.get("key"));
        }
        assertThat(keys).hasSize(5);
    }

    @Test
    @DisplayName("La clé ne contient que des caractères Base64 URL-safe")
    void keyShouldBeBase64UrlSafe() {
        String key = response.get("key");
        assertThat(key).matches("^[A-Za-z0-9_-]+$");
    }
}
