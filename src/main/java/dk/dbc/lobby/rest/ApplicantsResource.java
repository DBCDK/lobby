/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.lobby.model.ApplicantEntity;
import dk.dbc.lobby.model.ApplicantStateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;

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
            // force database errors to be caught before implicit commit on method exit
            entityManager.flush();
            return Response.status(status).build();
        } catch (PersistenceException e) {
            return Response.status(422).entity(e.getMessage()).build();
        }
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
                state = new ApplicantStateConverter().convertToDatabaseColumn(
                        ApplicantEntity.State.valueOf(stateStr));
            }
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity("Illegal state value " + stateStr).build();
        }

        final QueryBuilder queryBuilder = new QueryBuilder(ApplicantEntity.GET_APPLICANTS_QUERY)
                .and("a.category", category)
                .and("a.state", state);
        LOGGER.debug("GET /applicants query {}", queryBuilder.toString());

        final List resultSet = queryBuilder.build(entityManager, ApplicantEntity.WITHOUT_BODY)
                .getResultList();

        final StreamingOutput applicantsStreamingOutput = outputStream -> {
            final JsonGenerator generator = new ObjectMapper().getFactory()
                    .createGenerator(outputStream, JsonEncoding.UTF8);
            try {
                generator.writeStartArray();
                for (Object applicantEntity : resultSet) {
                    // Even though the native query does not retrieve the body
                    // the entitymanager might still choose to serve the entities
                    // from the cache, where the body might already be fetched.
                    ((ApplicantEntity) applicantEntity).setBody(null);
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

        final StreamingOutput streamingOutput = outputStream -> outputStream.write(applicantEntity.getBody());
        return Response.ok().type(applicantEntity.getMimetype()).entity(streamingOutput).build();
    }
}
