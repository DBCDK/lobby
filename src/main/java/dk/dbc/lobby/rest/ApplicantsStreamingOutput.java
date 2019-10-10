/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.lobby.model.ApplicantEntity;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

class ApplicantsStreamingOutput implements StreamingOutput {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResultSet<ApplicantEntity> resultSet;

    ApplicantsStreamingOutput(ResultSet<ApplicantEntity> resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        final JsonGenerator generator = OBJECT_MAPPER.getFactory()
                .createGenerator(outputStream, JsonEncoding.UTF8);
        try {
            generator.writeStartArray();
            for (ApplicantEntity applicantEntity : resultSet) {
                generator.writeObject(applicantEntity);
            }
            generator.writeEndArray();
        } finally {
            generator.flush();
            generator.close();
            resultSet.close();
        }
    }
}
