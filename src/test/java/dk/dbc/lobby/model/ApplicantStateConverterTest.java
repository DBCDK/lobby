/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.model;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicantStateConverterTest {
    private final ApplicantStateConverter converter = new ApplicantStateConverter();

    @Test
    void convertToDatabaseColumn_statusArgIsNull_returnsNullValuedDatabaseObject() {
        final Object pgObject = converter.convertToDatabaseColumn(null);
        assertThat("PGobject", pgObject, is(notNullValue()));
        assertThat("PGobject type", ((PGobject) pgObject).getType(), is("applicant_state"));
        assertThat("PGobject value", ((PGobject) pgObject).getValue(), is(nullValue()));
    }

    @Test
    void convertToDatabaseColumn() {
        final Object pgObject = converter.convertToDatabaseColumn(ApplicantEntity.State.PENDING);
        assertThat("PGobject", pgObject, is(notNullValue()));
        assertThat("PGobject type", ((PGobject) pgObject).getType(), is("applicant_state"));
        assertThat("PGobject value", ((PGobject) pgObject).getValue(), is(ApplicantEntity.State.PENDING.name()));
    }

    @Test
    void toEntityAttribute_dbValueArgIsNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(null));
    }

    @Test
    void toEntityAttribute() {
        assertThat("ACCEPTED", converter.convertToEntityAttribute("ACCEPTED"),
                is(ApplicantEntity.State.ACCEPTED));
        assertThat("PENDING", converter.convertToEntityAttribute("PENDING"),
                is(ApplicantEntity.State.PENDING));
    }
}