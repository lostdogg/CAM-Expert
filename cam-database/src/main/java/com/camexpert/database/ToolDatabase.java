package com.camexpert.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Manages the embedded H2 database connection for the tool library.
 *
 * <p>The database is stored as a single portable file at the path supplied to
 * the constructor, matching the product requirement for a "portable, single-file
 * asset". An in-memory URL ({@code jdbc:h2:mem:...}) is also supported for tests.
 *
 * <p>Call {@link #initialize()} once on startup to create the schema, then use
 * {@link #getConnection()} in repository classes. Call {@link #close()} on
 * shutdown to flush and release resources.
 */
public final class ToolDatabase {

    private static final Logger LOG = Logger.getLogger(ToolDatabase.class.getName());

    private final String jdbcUrl;
    private Connection connection;

    /**
     * Creates a database manager backed by the supplied JDBC URL.
     *
     * @param jdbcUrl H2 JDBC URL (file-based or in-memory)
     */
    public ToolDatabase(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("jdbcUrl must not be null or blank");
        }
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Convenience factory: creates an in-memory database (useful for tests and demos).
     *
     * @param dbName unique database name within the JVM
     * @return new in-memory ToolDatabase
     */
    public static ToolDatabase inMemory(String dbName) {
        return new ToolDatabase("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
    }

    /**
     * Opens the connection and creates the schema if it does not already exist.
     *
     * @throws SQLException if the database cannot be opened or the schema cannot be created
     */
    public void initialize() throws SQLException {
        connection = DriverManager.getConnection(jdbcUrl, "sa", "");
        createSchema();
        LOG.info("ToolDatabase initialised: " + jdbcUrl);
    }

    /**
     * Returns the active database connection. {@link #initialize()} must have been
     * called first.
     *
     * @return active JDBC connection
     * @throws IllegalStateException if the database has not been initialised
     */
    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("ToolDatabase not initialised; call initialize() first");
        }
        return connection;
    }

    /**
     * Closes the database connection and releases all resources.
     *
     * @throws SQLException if the connection cannot be closed
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            LOG.info("ToolDatabase closed.");
        }
    }

    // -------------------------------------------------------------------------
    // Schema creation
    // -------------------------------------------------------------------------

    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS cutting_geometry ("
                + "  id             BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "  name           VARCHAR(255) NOT NULL,"
                + "  diameter       DOUBLE NOT NULL,"
                + "  flute_length   DOUBLE NOT NULL,"
                + "  corner_radius  DOUBLE NOT NULL DEFAULT 0,"
                + "  num_flutes     INT    NOT NULL,"
                + "  overall_length DOUBLE NOT NULL"
                + ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS holders ("
                + "  id                BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "  name              VARCHAR(255) NOT NULL,"
                + "  holder_type       VARCHAR(100),"
                + "  body_diameter     DOUBLE NOT NULL,"
                + "  body_length       DOUBLE NOT NULL,"
                + "  coupling_diameter DOUBLE NOT NULL"
                + ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS tool_assemblies ("
                + "  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "  assembly_name        VARCHAR(255) NOT NULL,"
                + "  cutting_geometry_id  BIGINT NOT NULL REFERENCES cutting_geometry(id),"
                + "  holder_id            BIGINT NOT NULL REFERENCES holders(id),"
                + "  tool_projection      DOUBLE NOT NULL,"
                + "  notes                VARCHAR(1000)"
                + ")"
            );
        }
    }
}
