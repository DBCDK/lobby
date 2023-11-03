package dk.dbc.lobby.rest;

import dk.dbc.serviceutils.ServiceStatus;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Path;

@Stateless
@Path("/")
public class Status implements ServiceStatus {}
