package dk.dbc.lobby.model;

import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ApplicantEntityTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final String expectedJson =
            "{\"id\":\"42\",\"category\":\"bpf\",\"mimetype\":\"application/xml\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\",\"additionalInfo\":{\"localid\":\"bibID\",\"errors\":[\"err1\",\"err2\"]}}";

    @Test
    void jsonMarshalling() throws JSONBException {
        final ApplicantEntity entity = new ApplicantEntity();
        entity.setId("42");
        entity.setState(ApplicantEntity.State.PENDING);
        entity.setMimetype("application/xml");
        entity.setCategory("bpf");
        entity.setAdditionalInfo("{\"localid\": \"bibID\", \"errors\": [\"err1\", \"err2\"]}");
        entity.setBody("hello world".getBytes(StandardCharsets.UTF_8));
        assertThat(jsonbContext.marshall(entity), is(expectedJson));
    }

    @Test
    void jsonUnmarshalling() throws JSONBException {
        final String json = "{\"id\":\"42\",\"category\":\"bpf\",\"mimetype\":\"application/xml\",\"state\":\"PENDING\",\"body\":\"aGVsbG8gd29ybGQ=\",\"additionalInfo\":{\"localid\":\"bibID\",\"errors\":[\"err1\", \"err2\"]}}";
        final ApplicantEntity unmarshalled = jsonbContext.unmarshall(json, ApplicantEntity.class);
        assertThat(jsonbContext.marshall(unmarshalled), is(expectedJson));
    }
}
