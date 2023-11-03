package dk.dbc.lobby.rest;

import org.glassfish.jersey.jackson.JacksonFeature;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class LobbyApplication extends Application {
    private static final Set<Class<?>> classes = new HashSet<>();
    static {
        classes.add(ApplicantsResource.class);
        classes.add(JacksonFeature.class);
        classes.add(Status.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
