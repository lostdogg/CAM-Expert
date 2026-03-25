package com.camexpert.database.repository;

import com.camexpert.database.model.CuttingGeometry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CuttingGeometry} persistence.
 *
 * <p>Provides CRUD operations against the {@code cutting_geometry} table in the
 * embedded H2 database.
 */
public final class CuttingGeometryRepository {

    private final Connection connection;

    /**
     * @param connection active JDBC connection; must not be null
     */
    public CuttingGeometryRepository(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
    }

    /**
     * Persists a new {@link CuttingGeometry} and sets its generated ID.
     *
     * @param geom the geometry to save
     * @return the saved geometry with its generated {@code id} set
     * @throws SQLException on database error
     */
    public CuttingGeometry save(CuttingGeometry geom) throws SQLException {
        String sql = "INSERT INTO cutting_geometry "
                + "(name, diameter, flute_length, corner_radius, num_flutes, overall_length) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, geom.getName());
            ps.setDouble(2, geom.getDiameter());
            ps.setDouble(3, geom.getFluteLength());
            ps.setDouble(4, geom.getCornerRadius());
            ps.setInt(5, geom.getNumberOfFlutes());
            ps.setDouble(6, geom.getOverallLength());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    geom.setId(keys.getLong(1));
                }
            }
        }
        return geom;
    }

    /**
     * Finds a cutting geometry by its primary key.
     *
     * @param id primary key
     * @return an {@link Optional} containing the geometry, or empty if not found
     * @throws SQLException on database error
     */
    public Optional<CuttingGeometry> findById(long id) throws SQLException {
        String sql = "SELECT * FROM cutting_geometry WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all cutting geometries in the database.
     *
     * @return list of all cutting geometries
     * @throws SQLException on database error
     */
    public List<CuttingGeometry> findAll() throws SQLException {
        List<CuttingGeometry> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM cutting_geometry ORDER BY id")) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    /**
     * Deletes a cutting geometry by primary key.
     *
     * @param id primary key of the record to delete
     * @return {@code true} if a record was deleted
     * @throws SQLException on database error
     */
    public boolean deleteById(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM cutting_geometry WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private CuttingGeometry mapRow(ResultSet rs) throws SQLException {
        CuttingGeometry g = new CuttingGeometry();
        g.setId(rs.getLong("id"));
        g.setName(rs.getString("name"));
        g.setDiameter(rs.getDouble("diameter"));
        g.setFluteLength(rs.getDouble("flute_length"));
        g.setCornerRadius(rs.getDouble("corner_radius"));
        g.setNumberOfFlutes(rs.getInt("num_flutes"));
        g.setOverallLength(rs.getDouble("overall_length"));
        return g;
    }
}
