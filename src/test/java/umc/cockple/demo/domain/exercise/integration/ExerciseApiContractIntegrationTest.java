package umc.cockple.demo.domain.exercise.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import umc.cockple.demo.support.IntegrationTestBase;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseApiContractIntegrationTest extends IntegrationTestBase {

    private static final Map<String, List<String>> EXPECTED_ENDPOINTS = expectedEndpoints();

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("exercise API의 경로와 HTTP method를 유지한다")
    void exerciseEndpointsRemainStable() throws Exception {
        JsonNode apiDocument = getApiDocument();

        EXPECTED_ENDPOINTS.forEach((path, methods) -> {
            JsonNode pathItem = apiDocument.path("paths").path(path);

            assertThat(pathItem.isMissingNode())
                    .as("OpenAPI path %s가 존재해야 한다", path)
                    .isFalse();
            assertThat(fieldNames(pathItem))
                    .as("OpenAPI path %s의 HTTP method", path)
                    .containsAll(methods);
        });
    }

    @Test
    @DisplayName("exercise 요청 DTO의 Swagger schema 이름과 JSON 필드를 유지한다")
    void exerciseRequestSchemasRemainStable() throws Exception {
        JsonNode schemas = getApiDocument().path("components").path("schemas");

        assertSchemaProperties(schemas, "ExerciseCreateRequest", Set.of(
                "date", "buildingName", "roadAddress", "latitude", "longitude",
                "startTime", "endTime", "maxCapacity",
                "allowMemberGuestsInvitation", "allowExternalGuests", "notice"
        ));
        assertSchemaProperties(schemas, "ExerciseUpdateRequest", Set.of(
                "date", "buildingName", "roadAddress", "latitude", "longitude",
                "startTime", "endTime", "maxCapacity",
                "allowMemberGuestsInvitation", "allowExternalGuests", "notice"
        ));
        assertSchemaProperties(schemas, "ExerciseGuestInviteRequest", Set.of(
                "guestName", "gender", "level"
        ));
        assertSchemaProperties(schemas, "ExerciseCancelByManagerRequest", Set.of(
                "isGuest"
        ));
    }

    private JsonNode getApiDocument() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private static void assertSchemaProperties(JsonNode schemas, String schemaName, Set<String> expectedProperties) {
        JsonNode schema = schemas.path(schemaName);

        assertThat(schema.isMissingNode())
                .as("OpenAPI schema %s가 존재해야 한다", schemaName)
                .isFalse();
        assertThat(fieldNames(schema.path("properties")))
                .as("OpenAPI schema %s의 JSON properties", schemaName)
                .containsExactlyInAnyOrderElementsOf(expectedProperties);
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static Map<String, List<String>> expectedEndpoints() {
        Map<String, List<String>> endpoints = new LinkedHashMap<>();
        endpoints.put("/api/parties/{partyId}/exercises", List.of("post"));
        endpoints.put("/api/parties/{partyId}/exercises/calender", List.of("get"));
        endpoints.put("/api/exercises/{exerciseId}", List.of("get", "patch", "delete"));
        endpoints.put("/api/exercises/{exerciseId}/for-edit", List.of("get"));
        endpoints.put("/api/exercises/{exerciseId}/participants", List.of("post"));
        endpoints.put("/api/exercises/{exerciseId}/participants/my", List.of("delete"));
        endpoints.put("/api/exercises/{exerciseId}/participants/{participantId}", List.of("delete"));
        endpoints.put("/api/exercises/{exerciseId}/guests", List.of("get", "post"));
        endpoints.put("/api/exercises/{exerciseId}/guests/{guestId}", List.of("delete"));
        endpoints.put("/api/exercises/my/calender", List.of("get"));
        endpoints.put("/api/exercises/parties/my", List.of("get"));
        endpoints.put("/api/exercises/parties/my/calendar", List.of("get"));
        endpoints.put("/api/exercises/my", List.of("get"));
        endpoints.put("/api/exercises/recommendations", List.of("get"));
        endpoints.put("/api/exercises/recommendations/calendar", List.of("get"));
        endpoints.put("/api/buildings/exercises/{date}", List.of("get"));
        endpoints.put("/api/buildings/map/monthly", List.of("get"));
        return endpoints;
    }
}
