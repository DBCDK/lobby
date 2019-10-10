/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import dk.dbc.lobby.model.ApplicantEntity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class QueryBuilderTest {
    @Test
    void toStringRepresentation() {
        assertThat(new QueryBuilder(ApplicantEntity.GET_APPLICANTS_QUERY).toString(),
                is(ApplicantEntity.GET_APPLICANTS_QUERY));
    }

    @Test
    void toStringRepresentationWithParameters() {
        final QueryBuilder queryBuilder = new QueryBuilder(ApplicantEntity.GET_APPLICANTS_QUERY)
                .and("a.category", "bpf")
                .and("a.state", "PENDING");

        assertThat(queryBuilder.toString(), is(ApplicantEntity.GET_APPLICANTS_QUERY +
                " WHERE a.category = ?1 AND a.state = ?2"));
    }
}