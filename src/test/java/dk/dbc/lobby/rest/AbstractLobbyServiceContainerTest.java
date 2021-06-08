/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.lobby.rest;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

public abstract class AbstractLobbyServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLobbyServiceContainerTest.class);

    static final DBCPostgreSQLContainer dbcPostgreSQLContainer;
    static final GenericContainer lobbyServiceContainer;
    static final String lobbyServiceBaseUrl;
    static final HttpClient httpClient;

    static {
        dbcPostgreSQLContainer = new DBCPostgreSQLContainer();
        dbcPostgreSQLContainer.start();
        dbcPostgreSQLContainer.exposeHostPort();

        lobbyServiceContainer = new GenericContainer("docker-io.dbc.dk/lobby-service:devel")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("LOBBY_DB_URL", dbcPostgreSQLContainer.getPayaraDockerJdbcUrl())
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/status"))
                .withStartupTimeout(Duration.ofMinutes(5));
        lobbyServiceContainer.start();
        lobbyServiceBaseUrl = "http://" + lobbyServiceContainer.getContainerIpAddress() +
                ":" + lobbyServiceContainer.getMappedPort(8080);
        httpClient = HttpClient.create(HttpClient.newClient());
    }

    static Connection connectToLobbyDB() {
        try {
            final Connection connection = dbcPostgreSQLContainer.createConnection();
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
