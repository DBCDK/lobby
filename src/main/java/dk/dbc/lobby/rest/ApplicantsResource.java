package dk.dbc.lobby.rest;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.lobby.model.ApplicantBodyEntity;
import dk.dbc.lobby.model.ApplicantEntity;
import dk.dbc.lobby.model.ApplicantStateList;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless
@Path("/v1/api/applicants")
public class ApplicantsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicantsResource.class);

    @PersistenceContext(unitName = "lobbyPU")
    private EntityManager entityManager;

    private static final String ILLEGAL_STATE_VALUE = "Illegal state value ";

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
        LOGGER.info("Purge {} aged {}.", state, age);
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
     * <p>
     * Specific behavior depends on state.
     * Types:
     *      - ACCEPTED : Cleaned after 6 months
     *
     * @return a HTTP 200 OK response
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @DELETE
    public Response clean() {
        final EnumMap<ApplicantEntity.State, String> purgeRules = new EnumMap<>(ApplicantEntity.State.class);
        purgeRules.put(ApplicantEntity.State.ACCEPTED, "4 weeks");

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

        try {
            final ApplicantEntity.State state = ApplicantEntity.State.valueOf(stateStr);
            changeApplicantState(id, state);
        } catch(NotFoundException e) {
            return Response.status(410).entity(e.getMessage()).build();
        } catch(IllegalArgumentException e) {
            return Response.status(422).entity(ILLEGAL_STATE_VALUE + stateStr).build();
        }
        return Response.ok().build();
    }

    /**
     * Changes state of applicant resources with ID specified by a list
     * @param applicantStateList List of applicant ids and the state they should be set to
     * @return HTTP 200 Ok response when matching applicants has their state replaced. There can be 0 matching applicants
     */
    @PUT
    @Path("/state")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeApplicantsState(ApplicantStateList applicantStateList) {
        for (String id : applicantStateList.getIds()) {
            try {
                changeApplicantState(id, applicantStateList.getState());
            } catch(NotFoundException e) {
                LOGGER.warn("Application {} not found. Error is silently ignored.", id);
            }
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

        //noinspection DuplicatedCode
        Object state = null;
        try {
            if (stateStr != null) {
                state = ApplicantEntity.State.valueOf(stateStr);
            }
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity(ILLEGAL_STATE_VALUE + stateStr).build();
        }

        final QueryBuilder queryBuilder = new QueryBuilder(ApplicantEntity.GET_APPLICANTS_QUERY)
                .and("applicant.category", category)
                .and("applicant.state", state);
        LOGGER.debug("GET /applicants query {}", queryBuilder);

        @SuppressWarnings("unchecked")
        final List<ApplicantEntity> resultSet = queryBuilder.build(entityManager).getResultList();

        return getStreamingResponse(resultSet);
    }

    /**
     * Returns list of applicants (not including body content) matched by optional filters.
     * Filters may be applied also to fields in the additionalInfo object not having
     * the same name as the protected names 'category' and 'state' by passing them
     * in as extra query parameters like this: agency=123456 or user=192556
     * @param category category filter
     * @param stateStr state filter
     * @param uriInfo request information
     * @return an HTTP 200 Ok response streaming applicants as JSON array,
     *         an HTTP 422 Unprocessable Entity response when the state parameter can not
     *                    be converted into a legal state value.
     */
    @GET
    @Path("/additionalInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Response getApplicantsByAdditionalInfo(
            @QueryParam("category") String category,
            @QueryParam("state") String stateStr, @Context UriInfo uriInfo) {

        //noinspection DuplicatedCode
        Object state = null;
        try {
            if (stateStr != null) {
                state = ApplicantEntity.State.valueOf(stateStr);
            }
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity(ILLEGAL_STATE_VALUE + stateStr).build();
        }

        Map<String, String> additionalFilters = getAdditionalFilters(uriInfo);
        QueryBuilder queryBuilder = new QueryBuilder(
                ApplicantEntity.GET_APPLICANTS_BY_ADDITIONAL_INFO_QUERY,
                ApplicantEntity.GET_APPLICANTS_BY_ADDITIONAL_INFO_SQL_RESULT_SET_MAPPER)
                .and("applicant.category", category);
        if (stateStr != null && !stateStr.isEmpty()) {
            queryBuilder.and("CAST(applicant.state AS TEXT)", String.format("%s", state));
        }
        for (Map.Entry<String, String> entry : additionalFilters.entrySet()) {
            queryBuilder = queryBuilder.json(entry.getKey(), entry.getValue());
        }
        LOGGER.info("GET /applicants/additionalFilter query {}", queryBuilder);

        @SuppressWarnings("unchecked")
        final List<ApplicantEntity> resultSet = queryBuilder.build(entityManager).getResultList();
        return getStreamingResponse(resultSet);
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

    /**
     *
     * @param ids list of ids. Json. Like [ "id1", "id2", "id3" ].
     * @return a HTTP 200 ok response streaming applicant bodies.
     * Returns bodies for the ones that can be found. No warnings for the
     * ones that can not.
     * Silently returns empty list, when none of the ids can be found.
     */
    @POST
    @Path("/body/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicantBodyBulk(List<String> ids) {
        LOGGER.info("Get body for ids: {}.", ids);
        TypedQuery<ApplicantBodyEntity> query = entityManager
                .createNamedQuery(ApplicantEntity.GET_BULK_APPLICANT_BODIES,
                        ApplicantBodyEntity.class)
                .setParameter("ids", ids);
        List<ApplicantBodyEntity> applicantBodyEntities = query.getResultList();
        StreamingOutput streamingOutput = outputStream -> {
            Iterator<ApplicantBodyEntity> iterator = applicantBodyEntities.iterator();
            outputStream.write("[".getBytes(StandardCharsets.UTF_8));
            while (iterator.hasNext()) {
                outputStream.write(iterator.next().getBody());
                if (iterator.hasNext()) {
                    outputStream.write(", ".getBytes(StandardCharsets.UTF_8));
                }
            }
            outputStream.write("]".getBytes(StandardCharsets.UTF_8));
        };
        LOGGER.info("Succesfully delivered {} bodies.", applicantBodyEntities.size());
        return Response.ok().entity(streamingOutput).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicant(@PathParam("id") String id) {
        final ApplicantEntity applicantEntity = entityManager.find(ApplicantEntity.class, id);
        if (applicantEntity == null) {
            return Response.status(410).entity("Applicant not found").build();
        }

        return Response.ok().entity(applicantEntity).build();
    }

    private Response getStreamingResponse(List<ApplicantEntity> resultSet) {
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

    private Map<String, String> getAdditionalFilters(UriInfo uriInfo) {
        if( uriInfo == null || uriInfo.getQueryParameters() == null) {
            return Map.of();
        }

        List<String> additionalParameterNames = new ArrayList<>(uriInfo.getQueryParameters().keySet());
        additionalParameterNames.removeIf(k -> Set.of("state", "category").contains(k));

        Map<String, String> additionalParameters = new HashMap<>();
        for (String name : additionalParameterNames.stream().distinct().collect(Collectors.toList())) {
            additionalParameters.put(name, uriInfo.getQueryParameters().getFirst(name));
        }

        return additionalParameters;
    }

    private void changeApplicantState(String id, ApplicantEntity.State state) throws NotFoundException{
        final ApplicantEntity applicantEntity = entityManager.find(ApplicantEntity.class, id);
        if (applicantEntity == null) {
            throw new NotFoundException("Applicant " + id + " not found");
        }
        applicantEntity.setState(state);
    }
}
