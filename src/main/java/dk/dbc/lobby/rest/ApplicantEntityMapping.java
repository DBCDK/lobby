/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import dk.dbc.jsonb.JsonNodeConverter;
import dk.dbc.lobby.model.ApplicantEntity;
import dk.dbc.lobby.model.ApplicantStateConverter;
import org.postgresql.util.PGobject;

import javax.persistence.PersistenceException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class ApplicantEntityMapping implements Function<ResultSet, ApplicantEntity> {
    private static final ApplicantStateConverter APPLICANT_STATE_CONVERTER = new ApplicantStateConverter();
    private static final JsonNodeConverter JSON_NODE_CONVERTER = new JsonNodeConverter();

    @Override
    public ApplicantEntity apply(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                final ApplicantEntity applicantEntity = new ApplicantEntity();
                applicantEntity.setId(
                        resultSet.getString("ID"));
                applicantEntity.setCategory(
                        resultSet.getString("CATEGORY"));
                applicantEntity.setMimetype(
                        resultSet.getString("MIMETYPE"));
                applicantEntity.setState(APPLICANT_STATE_CONVERTER.convertToEntityAttribute(
                        resultSet.getString("STATE")));
                applicantEntity.setTimeOfCreation(
                        resultSet.getTimestamp("TIMEOFCREATION"));
                applicantEntity.setTimeOfLastModification(
                        resultSet.getTimestamp("TIMEOFLASTMODIFICATION"));
                applicantEntity.setAdditionalInfo(JSON_NODE_CONVERTER.convertToEntityAttribute(
                        (PGobject) resultSet.getObject("ADDITIONALINFO")));
                return applicantEntity;
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
        return null;
    }
}
