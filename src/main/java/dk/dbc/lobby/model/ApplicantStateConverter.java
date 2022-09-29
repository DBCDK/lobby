/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.model;

import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
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
