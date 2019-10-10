/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds JPA query using positional parameter bindings.
 * <p>
 * Caveat - currently only supports the equals '=' operator.
 * </p>
 * <p>
 * This class is not thread-safe.
 * </p>
 */
class QueryBuilder {
    private final String baseString;
    private final List<String> parameters = new ArrayList<>();
    private final List<Object> values = new ArrayList<>();

    private String toStringRepresentation;

    QueryBuilder(String baseQuery) {
        this.baseString = baseQuery;
        toStringRepresentation = baseString;
    }

    QueryBuilder and(String parameter, Object value) {
        if (value != null) {
            parameters.add(parameter);
            values.add(value);
            toStringRepresentation = null;
        }
        return this;
    }

    Query build(EntityManager entityManager, String resultMapping) {
        final Query query = entityManager.createNativeQuery(toString(), resultMapping);
        for (int i = 0; i < values.size(); i++) {
            query.setParameter(i+1, values.get(i));
        }
        return query;
    }

    @Override
    public String toString() {
        if (toStringRepresentation == null) {
            final StringBuilder queryString = new StringBuilder(baseString);
            for (int i = 0; i < parameters.size(); i++) {
                final String parameter = parameters.get(i);
                if (queryString.toString().equals(baseString)) {
                    queryString.append(" WHERE ").append(parameter).append(" = ?").append(i + 1);
                } else {
                    queryString.append(" AND ").append(parameter).append(" = ?").append(i + 1);
                }
            }
            toStringRepresentation = queryString.toString();
        }
        return toStringRepresentation;
    }
}
