/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

public abstract class AbstractLobbyServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLobbyServiceContainerTest.class);

    static final EmbeddedPostgres pg = pgStart();

    static {
        Testcontainers.exposeHostPorts(pg.getPort());
    }

    static final GenericContainer lobbyServiceContainer;
    static final String lobbyServiceBaseUrl;

    static {
        lobbyServiceContainer = new GenericContainer("docker-io.dbc.dk/lobby-service:devel")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("LOBBY_DB_URL", String.format("postgres:@host.testcontainers.internal:%s/postgres",
                        pg.getPort()))
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/status"))
                .withStartupTimeout(Duration.ofMinutes(5));
        lobbyServiceContainer.start();
        lobbyServiceBaseUrl = "http://" + lobbyServiceContainer.getContainerIpAddress() +
                ":" + lobbyServiceContainer.getMappedPort(8080);
    }

    private static EmbeddedPostgres pgStart() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
