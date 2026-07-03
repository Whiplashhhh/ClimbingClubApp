package app.belay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;

/**
 * Exporte le contrat OpenAPI vers openapi.json à la racine du dépôt (contract-first : le back est
 * la source de vérité, le front consomme les types générés). Lancé par `./mvnw verify` ; la CI
 * échoue si le fichier committé n'est pas à jour.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiExportTest {

    private static final Path EXPORT_PATH = Path.of("..", "openapi.json");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exportOpenApiContract() throws Exception {
        String body = restTemplate.getForObject("/v3/api-docs", String.class);
        assertThat(body).isNotBlank();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("info").path("title").asText()).isEqualTo("Belay API");

        // Le port du serveur de test est aléatoire : normalisé pour un export déterministe.
        ((ObjectNode) json)
                .set(
                        "servers",
                        objectMapper
                                .createArrayNode()
                                .add(objectMapper.createObjectNode().put("url", "/")));

        String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json) + "\n";
        Files.writeString(EXPORT_PATH, pretty);
    }
}
