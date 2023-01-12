package dk.dbc.lobby.rest;

import dk.dbc.lobby.model.ApplicantEntity;

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

    private String sqlResultSetMapper;

    QueryBuilder(String baseQuery) {
        this(baseQuery, null);
    }

    QueryBuilder(String baseQuery, String sqlResultSetMapper) {
        this.baseString = baseQuery;
        this.sqlResultSetMapper = sqlResultSetMapper;
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

    QueryBuilder json(String field, String value) {
        if (sqlResultSetMapper == null) {
            throw new IllegalArgumentException("json(...) not allowed on typed query");
        }
        if (field != null && value != null) {
            parameters.add("additionalInfo ->> '" + field + "'");
            values.add(value);
            toStringRepresentation = null;
        }
        return this;
    }

    Query build(EntityManager entityManager) {
        final Query query = sqlResultSetMapper == null
                ? entityManager.createQuery(toString(), ApplicantEntity.class)
                : entityManager.createNativeQuery(toString(), sqlResultSetMapper);
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
