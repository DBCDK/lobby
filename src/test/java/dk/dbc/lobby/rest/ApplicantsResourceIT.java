/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import dk.dbc.httpclient.HttpPut;
import dk.dbc.httpclient.PathBuilder;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ApplicantsResourceIT extends AbstractLobbyServiceContainerTest {
    @Test
    void createOrReplaceApplicant_invalidJson() {
        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withData("not json", "application/json")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}")
                        .bind("id", "unknown")
                        .build());

        final Response response = httpClient.execute(httpPut);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    void createOrReplaceApplicant_invalidApplicant() {
        final HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withJsonData("{}")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}")
                        .bind("id", "unknown")
                        .build());

        final Response response = httpClient.execute(httpPut);
        assertThat(response.getStatus(), is(422));
    }

    @Test
    void createOrReplaceApplicant() {
        HttpPut httpPut = new HttpPut(httpClient)
                .withBaseUrl(lobbyServiceBaseUrl)
                .withJsonData(
                        "{\"id\":\"createOrReplaceApplicant\",\"category\":\"createOrReplaceApplicant\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\",\"additionalInfo\":{\"localId\": \"extId\"}}")
                .withPathElements(new PathBuilder("/v1/api/applicants/{id}")
                        .bind("id", "createOrReplaceApplicant")
                        .build());

        Response response = httpClient.execute(httpPut);
        assertThat("create response status", response.getStatus(),
                is(201));
        assertThat("row exists", getApplicantById("createOrReplaceApplicant").get("id"),
                is("createOrReplaceApplicant"));

        httpPut.withJsonData("{\"id\":\"createOrReplaceApplicant\",\"category\":\"createOrReplaceApplicant_replace\",\"mimetype\":\"text/plain\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\"}");
        response = httpClient.execute(httpPut);

        assertThat("replace response status", response.getStatus(),
                is(200));
        assertThat("row updated", getApplicantById("createOrReplaceApplicant").get("category"),
                is("createOrReplaceApplicant_replace"));
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