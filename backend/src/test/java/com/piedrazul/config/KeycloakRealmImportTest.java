package com.piedrazul.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakRealmImportTest {

    @Test
    void audienceMappersMustUseAnExistingClientOrACustomAudience() throws Exception {
        JsonNode realm = new ObjectMapper().readTree(Path.of("keycloak", "piedrazul-realm.json").toFile());
        Set<String> clientIds = new HashSet<>();
        realm.path("clients").forEach(client -> {
            clientIds.add(client.path("clientId").asText());
            if (!client.path("id").asText().isBlank()) {
                clientIds.add(client.path("id").asText());
            }
        });

        for (JsonNode client : realm.path("clients")) {
            for (JsonNode mapper : client.path("protocolMappers")) {
                if (!"oidc-audience-mapper".equals(mapper.path("protocolMapper").asText())) {
                    continue;
                }
                JsonNode config = mapper.path("config");
                String targetClient = config.path("included.client.audience").asText();
                String customAudience = config.path("included.custom.audience").asText();
                assertTrue(!customAudience.isBlank() || clientIds.contains(targetClient),
                        "El mapper de audiencia de " + client.path("clientId").asText()
                                + " debe usar una audiencia personalizada o un cliente declarado en el realm.");
            }
        }
    }
}
