/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import dk.dbc.lobby.model.ApplicantEntity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("/v1/api/applicants")
public class ApplicantsResource {
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
}
