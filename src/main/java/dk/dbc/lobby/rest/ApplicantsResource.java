/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.lobby.model.ApplicantBodyEntity;
import dk.dbc.lobby.model.ApplicantEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
@Path("/v1/api/applicants")
public class ApplicantsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicantsResource.class);

    @PersistenceContext(unitName = "lobbyPU")
    private EntityManager entityManager;

    /**
     * Creates applicant resource with ID specified by the path
     * or completely replaces and existing applicant
     * @param id applicant ID
     * @param applicantEntity applicant entity
     * @return a HTTP 200 Ok response when an existing applicant is replaced,
     *         a HTTP 201 Created response when a new applicant is created,
     *         a HTTP 400 Bad Request response when the request has malformed syntax,
     *         a HTTP 422 Unprocessable Entity response when the syntax of the request
     *                    entity is correct, but the server was unable to process the
     *                    entity due to invalid data.
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrReplaceApplicant(@PathParam("id") String id, ApplicantEntity applicantEntity) {
        try {
            final byte[] body = applicantEntity.getBody();
            applicantEntity.setBody(null);

            // ensure ID from path matches ID in entity
            applicantEntity.setId(id);
            
            final int status;
            if (entityManager.find(ApplicantEntity.class, id) == null) {
                entityManager.persist(applicantEntity);
                status = 201;
            } else {
                entityManager.merge(applicantEntity);
                status = 200;
            }
            entityManager.flush();

            ApplicantBodyEntity applicantBodyEntity = entityManager.find(ApplicantBodyEntity.class, id);
            if (applicantBodyEntity == null) {
                applicantBodyEntity = new ApplicantBodyEntity();
                applicantBodyEntity.setId(id);
                applicantBodyEntity.setBody(body);
                entityManager.persist(applicantBodyEntity);
            } else {
                applicantBodyEntity.setBody(body);
            }
            entityManager.flush();

            return Response.status(status).build();
        } catch (PersistenceException e) {
            return Response.status(422).entity(e.getMessage()).build();
        }
    }

    private void purgeApplicantsByStateAndAge(ApplicantEntity.State state, String age) {
        LOGGER.info("Purge {} aged {}.", state.toString(), age);
        final TypedQuery<ApplicantEntity> query = entityManager
                .createNamedQuery(ApplicantEntity.GET_OUTDATED_APPLICANTS,
                        ApplicantEntity.class)
                .setParameter(1, state.toString())
                .setParameter(2, age);
        final List<ApplicantEntity> applicantEntityList = query.getResultList();
        applicantEntityList.forEach(ael -> entityManager.remove(ael));
        LOGGER.info("Succesfully deleted {} files.", applicantEntityList.size());
    }

    /**
     * Performs a clean operation on lobby database
     *
     * Specific behavior depends on state.
     * Types:
     *      - ACCEPTED : Cleaned after 6 months
     *
     * @return a HTTP 200 OK response
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @DELETE
    public Response clean() {
        final Map<ApplicantEntity.State, String> purgeRules = new HashMap<ApplicantEntity.State, String>() {{
            put(ApplicantEntity.State.ACCEPTED, "4 weeks");
        }};
        try {
            purgeRules.forEach((state,age) -> {
                LOGGER.info("Deleting applicants with {} older than {}", state, age);
                purgeApplicantsByStateAndAge(state, age);
            });
        }
        catch (Exception e) {
            LOGGER.error("Deleting failed:{}", e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    /**
     * Changes state of applicant resource with ID specified by the path
     * @param id applicant ID
     * @param stateStr new applicant state, must be from the set {ACCEPTED, PENDING}
     * @return a HTTP 200 Ok response when applicant has its state replaced,
     *         a HTTP 410 Gone response when an applicant with the given ID can not be found,
     *         a HTTP 422 Unprocessable Entity response when the request entity can not
     *                    be converted into a legal state value.
     */
    @PUT
    @Path("/{id}/state")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response changeApplicantState(@PathParam("id") String id, String stateStr) {
        final ApplicantEntity applicantEntity = entityManager.find(ApplicantEntity.class, id);
        if (applicantEntity == null) {
            return Response.status(410).entity("Applicant not found").build();
        }
        try {
            final ApplicantEntity.State state = ApplicantEntity.State.valueOf(stateStr);
            applicantEntity.setState(state);
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity("Illegal state value " + stateStr).build();
        }
        return Response.ok().build();
    }

    /**
     * Returns list of applicants (not including body content) matched by optional filters
     * @param category category filter
     * @param stateStr state filter
     * @return a HTTP 200 Ok response streaming applicants as JSON array,
     *         a HTTP 422 Unprocessable Entity response when the state parameter can not
     *                    be converted into a legal state value.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Response getApplicants(
            @QueryParam("category") String category,
            @QueryParam("state") String stateStr) {

        Object state = null;
        try {
            if (stateStr != null) {
                state = ApplicantEntity.State.valueOf(stateStr);
            }
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity("Illegal state value " + stateStr).build();
        }

        final QueryBuilder queryBuilder = new QueryBuilder(ApplicantEntity.GET_APPLICANTS_QUERY)
                .and("applicant.category", category)
                .and("applicant.state", state);
        LOGGER.debug("GET /applicants query {}", queryBuilder.toString());

        final List resultSet = queryBuilder.build(entityManager).getResultList();

        final StreamingOutput applicantsStreamingOutput = outputStream -> {
            final JsonGenerator generator = new ObjectMapper().getFactory()
                    .createGenerator(outputStream, JsonEncoding.UTF8);
            try {
                generator.writeStartArray();
                for (Object applicantEntity : resultSet) {
                    generator.writeObject(applicantEntity);
                }
                generator.writeEndArray();
            } finally {
                generator.flush();
                generator.close();
            }
        };

        return Response.ok().entity(applicantsStreamingOutput).build();
    }

    /**
     * Returns the BLOB content for the applicant resource with ID specified by the path.
     * @param id applicant ID
     * @return a HTTP 200 Ok response streaming applicant body,
     *         a HTTP 410 Gone response when an applicant with the given ID can not be found.
     */
    @GET
    @Path("/{id}/body")
    public Response getApplicantBody(@PathParam("id") String id) {
        final ApplicantEntity applicantEntity = entityManager.find(ApplicantEntity.class, id);
        if (applicantEntity == null) {
            return Response.status(410).entity("Applicant not found").build();
        }
        final ApplicantBodyEntity applicantBodyEntity = entityManager.find(ApplicantBodyEntity.class, id);
        if (applicantBodyEntity == null) {
            return Response.status(410).entity("Applicant body not found").build();
        }

        final StreamingOutput streamingOutput = outputStream -> outputStream.write(applicantBodyEntity.getBody());
        return Response.ok().type(applicantEntity.getMimetype()).entity(streamingOutput).build();
    }
}
