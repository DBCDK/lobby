package dk.dbc.lobby.model;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(converter.convertToEntityAttribute(null), is(nullValue()));
    }

    @Test
    void toEntityAttribute() {
        assertThat("ACCEPTED", converter.convertToEntityAttribute("ACCEPTED"),
                is(ApplicantEntity.State.ACCEPTED));
        assertThat("PENDING", converter.convertToEntityAttribute("PENDING"),
                is(ApplicantEntity.State.PENDING));
    }
}
