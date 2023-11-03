package dk.dbc.lobby.rest;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

public abstract class AbstractLobbyServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLobbyServiceContainerTest.class);
    private static final Network NETWORK = Network.newNetwork();

    static final DBCPostgreSQLContainer LOBBY_DB = startLobbyDB(NETWORK);
    static final GenericContainer<?> lobbyServiceContainer = startLobby(LOBBY_DB, NETWORK);
    static final String lobbyServiceBaseUrl = "http://" + lobbyServiceContainer.getHost() + ":" + lobbyServiceContainer.getMappedPort(8080);
    static final HttpClient httpClient = HttpClient.create(HttpClient.newClient());


    private static GenericContainer<?> startLobby(DBCPostgreSQLContainer db, Network network) {
        GenericContainer<?> container = new GenericContainer<>("docker-metascrum.artifacts.dbccloud.dk/lobby-service:devel")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("LOBBY_DB_URL", db.getPayaraDockerJdbcUrl())
                .withNetwork(network)
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/status"))
                .withStartupTimeout(Duration.ofMinutes(5));
        container.start();
        return container;
    }

    private static DBCPostgreSQLContainer startLobbyDB(Network network) {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer()
                .withNetwork(network)
                .withReuse(false)
                .withNetworkAliases("lobbydb");
        container.start();
        container.exposeHostPort();
        return container;
    }


    static Connection connectToLobbyDB() {
        try {
            Connection connection = LOBBY_DB.createConnection();
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
