package com.trt.contentengagement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class ProductionComposeConfigurationTest {
    @Test
    @SuppressWarnings("unchecked")
    void onlyTheHttpsWebGatewayPublishesHostPorts() throws IOException {
        Map<String, Object> compose = new Yaml().load(
                Files.readString(Path.of("compose.production.yaml"))
        );
        Map<String, Map<String, Object>> services =
                (Map<String, Map<String, Object>>) compose.get("services");

        assertThat(services).containsKeys(
                "web", "backend", "leaderboard-service", "postgres",
                "leaderboard-postgres", "redis", "leaderboard-redis", "rabbitmq"
        );
        assertThat(services.get("web").get("ports")).isEqualTo(
                java.util.List.of("80:80", "443:443", "443:443/udp")
        );
        services.entrySet().stream()
                .filter(service -> !service.getKey().equals("web"))
                .forEach(service -> assertThat(service.getValue())
                        .as(service.getKey() + " must stay on the private Docker network")
                        .doesNotContainKey("ports"));
    }
}
