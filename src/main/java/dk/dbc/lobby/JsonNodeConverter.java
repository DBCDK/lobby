package dk.dbc.lobby;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(JsonNode node) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(JSONB_CONTEXT.marshall(node));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public JsonNode convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        if (pgObject != null) {
            try {
                return JSONB_CONTEXT.getJsonTree(pgObject.getValue());
            } catch (JSONBException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}
