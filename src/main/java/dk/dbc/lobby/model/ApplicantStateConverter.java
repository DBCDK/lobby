package dk.dbc.lobby.model;

import org.postgresql.util.PGobject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.SQLException;

@Converter
public class ApplicantStateConverter implements AttributeConverter<ApplicantEntity.State, Object> {
    @Override
    public Object convertToDatabaseColumn(ApplicantEntity.State state) {
        String statusValue = null;
        if (state != null) {
            statusValue = state.name();
        }

        final PGobject pgObject = new PGobject();
        pgObject.setType("applicant_state");
        try {
            pgObject.setValue(statusValue);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public ApplicantEntity.State convertToEntityAttribute(Object dbValue) {
        if (dbValue == null) {
            return null;
        }
        return ApplicantEntity.State.valueOf((String) dbValue);
    }
}
