package dk.dbc.lobby.rest;

import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
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
