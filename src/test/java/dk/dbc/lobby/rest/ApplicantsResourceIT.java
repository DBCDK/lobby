package dk.dbc.lobby.rest;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.HttpPut;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.lobby.model.ApplicantEntity;
import dk.dbc.lobby.model.ApplicantStateList;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.bouncycastle.util.encoders.Base64;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;

class ApplicantsResourceIT extends AbstractLobbyServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicantsResourceIT.class);
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    void createOrReplaceApplicant_invalidJson() {
        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData("not json", "application/json")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}")
                        .bind("id", "unknown")
                        .build());

        try (Response response = httpClient.execute(httpPut)) {
            assertThat(response.getStatus(), is(400));
        }
    }

    @Test
    void createOrReplaceApplicant_invalidApplicant() {
        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withJsonData("{}")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}")
                        .bind("id", "unknown")
                        .build());

        try (Response response = httpClient.execute(httpPut)) {
            assertThat(response.getStatus(), is(422));
        }
    }

    @Test
    void createOrReplaceApplicant() {
        final String id = "createOrReplaceApplicant";

        Response response = createOrReplaceApplicant(id,
                "{\"id\":\"createOrReplaceApplicant\",\"category\":\"createOrReplaceApplicant\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\",\"additionalInfo\":{\"localId\": \"extId\"}}");
        assertThat("create response status", response.getStatus(),
                is(201));
        assertThat("row exists", getApplicantById(id).get("id"),
                is(id));

        response = createOrReplaceApplicant(id,
                "{\"id\":\"createOrReplaceApplicant\",\"category\":\"createOrReplaceApplicant_replace\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        assertThat("replace response status", response.getStatus(),
                is(200));
        assertThat("row updated", getApplicantById(id).get("category"),
                is("createOrReplaceApplicant_replace"));
    }

    @Test
    void changeApplicantState_applicantNotFound() {
        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData("ACCEPTED", "text/plain")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}/state")
                        .bind("id", "unknown")
                        .build());

        try (Response response = httpClient.execute(httpPut)) {
            assertThat(response.getStatus(), is(410));
        }
    }

    @Test
    void changeApplicantState_invalidState() {
        final String id = "changeApplicantState_invalidState";
        createOrReplaceApplicant(id,
                "{\"id\":\"changeApplicantState_invalidState\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");

        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData("NOT_A_KNOWN_STATE", "text/plain")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}/state")
                        .bind("id", id)
                        .build());

        try (Response response = httpClient.execute(httpPut)) {
            assertThat(response.getStatus(), is(422));
        }
    }

    @Test
    void changeApplicantState() {
        final String id = "changeApplicantState";
        createOrReplaceApplicant(id,
                "{\"id\":\"changeApplicantState\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");

        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData("ACCEPTED", "text/plain")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}/state")
                        .bind("id", id)
                        .build());

        try (Response response = httpClient.execute(httpPut)) {
            assertThat("response status", response.getStatus(),
                    is(200));
        }
        assertThat("state updated", getApplicantById(id).get("state"),
                is("ACCEPTED"));
    }

    @Test
    void changeApplicantsState_applicantNotFound() {
        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData(new ApplicantStateList()
                        .withState(ApplicantEntity.State.ACCEPTED)
                        .withIds(List.of("unknown", "even more unknown")), "application/json")
                .withPathElements("/v1/api/applicants/state");

        try (Response response = httpClient.execute(httpPut)) {
            assertThat(response.getStatus(), is(200));
        }
    }

    @Test
    void changeApplicantsState_invalidState() {
        final String id1 = "changeApplicantState_1_invalidState";
        final String id2 = "changeApplicantState_2_invalidState";
        createOrReplaceApplicant(id1,
                "{\"id\":\"" + id1 + "\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        createOrReplaceApplicant(id2,
                "{\"id\":\"" + id2 + "\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");

        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData("{\"state\":\"NOT_A_KNOWN_STATE\",\"ids\":[\"" + id1 + "\",\"" + id2 + "\"]}", "application/json")
                .withPathElements("/v1/api/applicants/state");

        try (Response response = httpClient.execute(httpPut)) {
            assertThat(response.getStatus(), is(400));
        }
    }

    @Test
    void changeApplicantsState() {
        final String id1 = "changeApplicantState_1";
        final String id2 = "changeApplicantState_2";
        final String id3 = "changeApplicantState_3";
        createOrReplaceApplicant(id1,
                "{\"id\":\"" + id1 + "\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        createOrReplaceApplicant(id2,
                "{\"id\":\"" + id2 + "\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        createOrReplaceApplicant(id3,
                "{\"id\":\"" + id3 + "\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");

        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData(new ApplicantStateList()
                        .withState(ApplicantEntity.State.ACCEPTED)
                        .withIds(List.of(id1, id2)), "application/json")
                .withPathElements("/v1/api/applicants/state");

        try (Response response = httpClient.execute(httpPut)) {
            assertThat("response status", response.getStatus(),
                    is(200));
        }
        assertThat("state updated", getApplicantById(id1).get("state"),
                is("ACCEPTED"));
        assertThat("state updated", getApplicantById(id2).get("state"),
                is("ACCEPTED"));
        assertThat("state not updated", getApplicantById(id3).get("state"),
                is("PENDING"));
    }

    @Test
    void getApplicantBody_applicantNotFound() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}/body")
                        .bind("id", "unknown")
                        .build());

        try (Response response = httpClient.execute(httpGet)) {
            assertThat(response.getStatus(), is(410));
        }
    }

    @Test
    void getApplicantBody() {
        final String id = "getApplicantBody";
        createOrReplaceApplicant(id,
                "{\"id\":\"getApplicantBody\",\"category\":\"changeApplicantState\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");

        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}/body")
                        .bind("id", id)
                        .build());

        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response status", response.getStatus(),
                    is(200));
            assertThat("response mimetype", response.getMediaType().toString(),
                    is(MediaType.TEXT_PLAIN));
            assertThat("applicant body", response.readEntity(String.class),
                    is("hello world"));
        }
    }


    @Test
    void getMultipleApplicantBodies() {
        final int applicantsSize = 9;
        String baseId = "getApplicantBody";
        List<String> jsonTestData = getJsonData(applicantsSize);
        String metaDataTemplate = "{\"id\":\"%s%d\",\"category\":\"changeApplicantState\",\"mimetype\":\"application/metaData\",\"state\":\"PENDING\",\"body\":\"%s\"}";

        // Create <applicantsSize> number of applicants.
        for (int i = 0; i < applicantsSize; i++) {
            try(Response response = createOrReplaceApplicant(baseId + i, String.format(metaDataTemplate, baseId, i, Base64.toBase64String(jsonTestData.get(i).getBytes())))) {
                assertThat(response.getStatus(), in(List.of(200, 201)));
            }
        }

        // Do the unified query to get them all back in one batch.
        String data = "[" + IntStream.range(0, applicantsSize).mapToObj(i -> "\"" + baseId + i + "\"").collect(Collectors.joining(", ")) + "]";

        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withPathElements(new PathBuilder("/v1/api/applicants/body/bulk")
                        .build())
                .withHeader("Content-Type", "application/json")
                .withData(data, MediaType.APPLICATION_JSON);

        try (Response response = httpClient.execute(httpPost)) {
            assertThat("response status", response.getStatus(),
                    is(200));
            assertThat("response mimetype", response.getMediaType().toString(),
                    is(MediaType.APPLICATION_JSON));
            JsonNode json = jsonbContext.getJsonTree(response.readEntity(String.class));
            if (json.isArray()) {
                for (int i = 0; i < applicantsSize; i++) {
                    assertThat("Json Value", json.get(i).findValue("someId").asText(), is("some-data-" + i));
                }
            } else {
                throw new RuntimeException("No array of jsondata found.");
            }

        } catch (JSONBException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getJsonData(int num) {
        return IntStream.range(0, num).mapToObj(i1 -> String.format("{\"someId\": \"some-data-%d\"}", i1)).collect(Collectors.toList());
    }

    @Test
    void getApplicants_invalidStateFilter() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("state", "NOT_A_KNOWN_STATE")
                .withPathElements(new PathBuilder("/v1/api/applicants").build());

        try (Response response = httpClient.execute(httpGet)) {
            assertThat(response.getStatus(), is(422));
        }
    }

    @Test
    void getApplicants_emptyResult() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("category", "NOT_A_KNOWN_CATEGORY")
                .withQueryParameter("state", "PENDING")
                .withPathElements(new PathBuilder("/v1/api/applicants").build());

        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response state", response.getStatus(),
                    is(200));
            assertThat("response content", response.readEntity(String.class),
                    is("[]"));
        }
    }

    @Test
    void getApplicants() {
        createOrReplaceApplicantsForQuery();

        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("category", "getApplicants")
                .withQueryParameter("state", "PENDING")
                .withPathElements(new PathBuilder("/v1/api/applicants").build());

        final String json;
        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response state", response.getStatus(),
                    is(200));
            json = response.readEntity(String.class);
        }
        assertThat("getApplicants-1 in response", json,
                containsString("\"id\":\"getApplicants-1\""));
        assertThat("getApplicants-2 not in response", json,
                not(containsString("\"id\":\"getApplicants-2\"")));
        assertThat("getApplicants-3 in response", json,
                containsString("\"id\":\"getApplicants-3\""));
        assertThat("body is not contained in response", json,
                not(containsString("\"body\":")));
    }

    @Test
    void getApplicantsByAdditionalInfo_invalidStateFilter() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("state", "NOT_A_KNOWN_STATE")
                .withQueryParameter("agency", "123456")
                .withQueryParameter("user", "192556")
                .withPathElements(new PathBuilder("/v1/api/applicants/additionalInfo").build());

        try (Response response = httpClient.execute(httpGet)) {
            assertThat(response.getStatus(), is(422));
        }
    }

    @Test
    void getApplicantsByAdditionalInfo_emptyResult() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("category", "NOT_A_KNOWN_CATEGORY")
                .withQueryParameter("state", "PENDING")
                .withQueryParameter("agency", "123456")
                .withQueryParameter("user", "192556")
                .withPathElements(new PathBuilder("/v1/api/applicants/additionalInfo").build());

        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response state", response.getStatus(),
                    is(200));
            assertThat("response content", response.readEntity(String.class),
                    is("[]"));
        }
    }

    @Test
    void getApplicantsByAdditionalInfoWithNoFilter() {
        createOrReplaceApplicantsForQuery();

        // Check that a query without filtering on any additional info fields should yield the two pending results
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("category", "getApplicants")
                .withQueryParameter("state", "PENDING")
                .withPathElements(new PathBuilder("/v1/api/applicants/additionalInfo").build());

        String json;
        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response state", response.getStatus(),
                    is(200));
            json = response.readEntity(String.class);
        }
        assertThat("getApplicants-1 in response", json,
                containsString("\"id\":\"getApplicants-1\""));
        assertThat("getApplicants-2 not in response", json,
                not(containsString("\"id\":\"getApplicants-2\"")));
        assertThat("getApplicants-3 in response", json,
                containsString("\"id\":\"getApplicants-3\""));
        assertThat("body is not contained in response", json,
                not(containsString("\"body\":")));
    }

    @Test
    void getApplicantsByAdditionalInfoWithFilter() {
        createOrReplaceApplicantsForQuery();

        // Check that a query with filtering on the additional 'agency' field should yield the single pending result with that agency
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("category", "getApplicants")
                .withQueryParameter("state", "PENDING")
                .withQueryParameter("agency", "761500")
                .withPathElements(new PathBuilder("/v1/api/applicants/additionalInfo").build());

        String json;
        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response state", response.getStatus(),
                    is(200));
            json = response.readEntity(String.class);
        }
        assertThat("getApplicants-1 not in response", json,
                not(containsString("\"id\":\"getApplicants-1\"")));
        assertThat("getApplicants-2 not in response", json,
                not(containsString("\"id\":\"getApplicants-2\"")));
        assertThat("getApplicants-3 in response", json,
                containsString("\"id\":\"getApplicants-3\""));
        assertThat("body is not contained in response", json,
                not(containsString("\"body\":")));
    }

    @Test
    void checkApplicantReturnedByAdditionalInfoQuery() {
        createOrReplaceApplicantsForQuery();

        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withQueryParameter("category", "getApplicants")
                .withQueryParameter("state", "PENDING")
                .withQueryParameter("agency", "761500")
                .withPathElements(new PathBuilder("/v1/api/applicants/additionalInfo").build());

        String json;
        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response state", response.getStatus(),
                    is(200));
            json = response.readEntity(String.class);
        }

        assertThat("id in response", json,
                containsString("\"id\":\"getApplicants-3\""));
        assertThat("category in response", json,
                containsString("\"category\":\"getApplicants\""));
        assertThat("mimetype in response", json,
                containsString("\"mimetype\":\"text/plain\""));
        assertThat("state in response", json,
                containsString("\"state\":\"PENDING\""));
        assertThat("timeOfCreation in response", json,
                containsString("\"timeOfCreation\":"));
        assertThat("timeOfLastModification in response", json,
                containsString("\"timeOfLastModification\":"));
        assertThat("body is not contained in response", json,
                not(containsString("\"body\":")));
        assertThat("additionalInfo in response", json,
                containsString("\"additionalInfo\":{\"agency\":\"761500\"}"));
    }

    @Test
    void getApplicant_notFound() {
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withPathElements(new PathBuilder("/v1/api/applicants/no-such-id").build());

        try (Response response = httpClient.execute(httpGet)) {
            assertThat(response.getStatus(), is(410));
        }
    }

    @Test
    void getApplicant() {
        createOrReplaceApplicantsForQuery();

        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withPathElements(new PathBuilder("/v1/api/applicants/getApplicants-2").build());

        final String json;
        try (Response response = httpClient.execute(httpGet)) {
            assertThat("response state", response.getStatus(),
                    is(200));
            json = response.readEntity(String.class);
        }
        assertThat("getApplicants-1 not in response", json,
                not(containsString("\"id\":\"getApplicants-1\"")));
        assertThat("getApplicants-2 in response", json,
                containsString("\"id\":\"getApplicants-2\""));
        assertThat("getApplicants-3 not in response", json,
                not(containsString("\"id\":\"getApplicants-3\"")));
        assertThat("body is not contained in response", json,
                not(containsString("\"body\":")));
    }


    @Test
    void deleteOutdatedApplicants() {
        // Given: Three applicants. One is recent, and accepted. One is accepted and 200 days old.
        //      One is PENDING and 200 days old.
        final String id1 = "getApplicants-1";
        createOrReplaceApplicant(id1,
                "{\"id\":\"getApplicants-1\",\"category\":\"getApplicants\",\"mimetype\":\"text/plain\",\"state\":\"ACCEPTED\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        final String id2 = "getApplicants-2";
        createOrReplaceApplicant(id2,
                "{\"id\":\"getApplicants-2\",\"category\":\"getApplicants\",\"mimetype\":\"text/plain\",\"state\":\"ACCEPTED\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        final String id3 = "getApplicants-3";
        createOrReplaceApplicant(id3,
                "{\"id\":\"getApplicants-3\",\"category\":\"getApplicants\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        final String id4 = "getApplicants-4";
        createOrReplaceApplicant(id4,
                "{\"id\":\"getApplicants-4\",\"category\":\"getApplicants\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");

        outdateThisApplicant(id2);
        outdateThisApplicant(id3);
        outdateThisApplicant(id4);

        // When a "clean" lobby is performed
        final HttpDelete httpDelete= new HttpDelete(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withPathElements(new PathBuilder("/v1/api/applicants").build());
        httpClient.execute(httpDelete);

        // Then only the one accepted and 200 days old are deleted
        assertThat("The one with status pending is still there", getApplicantWithThisId(id3).getStatus(), is(200));
        assertThat("The one with status accepted, and is recent is still there", getApplicantWithThisId(id1).getStatus(), is(200));
        assertThat("The one with status pending, but 200 days old, is still there", getApplicantWithThisId(id4).getStatus(), is(200));
        assertThat("The one with status accepted, but 200 days old, is no longer there", getApplicantWithThisId(id2).getStatus(), is(410));
    }
    
    static Response createOrReplaceApplicant(String id, String json) {
        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withJsonData(json)
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}")
                        .bind("id", id)
                        .build());
        return httpClient.execute(httpPut);
    }

    private void createOrReplaceApplicantsForQuery() {
        final String id1 = "getApplicants-1";
        createOrReplaceApplicant(id1,
                "{\"id\":\"getApplicants-1\",\"category\":\"getApplicants\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\",\"additionalInfo\":{\"agency\":\"010100\"}}");

        final String id2 = "getApplicants-2";
        createOrReplaceApplicant(id2,
                "{\"id\":\"getApplicants-2\",\"category\":\"getApplicants\",\"mimetype\":\"text/plain\",\"state\":\"ACCEPTED\",\"body\":\"aGVsbG8gd29ybGQ=\",\"additionalInfo\":{\"agency\":\"761500\"}}");

        final String id3 = "getApplicants-3";
        createOrReplaceApplicant(id3,
                "{\"id\":\"getApplicants-3\",\"category\":\"getApplicants\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\",\"additionalInfo\":{\"agency\":\"761500\"}}");
    }

    static void outdateThisApplicant(String id) {
        try (Connection connection = connectToLobbyDB()) {
            PreparedStatement preparedStatement = null;
            try {
                final String updateSQL = "UPDATE applicant SET timeOfLastModification=now()-CAST ('200 days' AS INTERVAL) WHERE id=?";
                preparedStatement = connection.prepareStatement(updateSQL);
                preparedStatement.setString(1,id);
                preparedStatement.executeUpdate();
            } finally {
                if (preparedStatement != null){
                    preparedStatement.close();
                }
            }

        }
        catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static Response getApplicantWithThisId(String id){
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}/body")
                        .bind("id", id)
                        .build());

        return  httpClient.execute(httpGet);
    }

    static HashMap<String, Object> getApplicantById(String id) {
        try (Connection connection = connectToLobbyDB()) {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                final HashMap<String, Object> row = new HashMap<>();
                final String selectSQL = "SELECT * FROM applicant WHERE id = ?";
                preparedStatement = connection.prepareStatement(selectSQL);
                preparedStatement.setString(1, id);
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    final ResultSetMetaData metadata = resultSet.getMetaData();
                    final int columnCount = metadata.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        final String columnName = metadata.getColumnName(i);
                        row.put(columnName, resultSet.getObject(i));
                    }
                    break;
                }
                return row;
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
