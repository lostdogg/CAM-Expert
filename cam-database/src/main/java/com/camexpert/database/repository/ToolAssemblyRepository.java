package com.camexpert.database.repository;

import com.camexpert.database.model.ToolAssembly;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ToolAssembly} persistence.
 *
 * <p>Provides CRUD operations against the {@code tool_assemblies} table in the
 * embedded H2 database. The assembly table is the relational link between
 * {@link com.camexpert.database.model.CuttingGeometry} and
 * {@link com.camexpert.database.model.Holder}, augmented with the critical
 * {@code tool_projection} dimension used by the collision-avoidance system.
 */
public final class ToolAssemblyRepository {

    private final Connection connection;

    /**
     * @param connection active JDBC connection; must not be null
     */
    public ToolAssemblyRepository(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
    }

    /**
     * Persists a new {@link ToolAssembly} and sets its generated ID.
     *
     * @param assembly the assembly to save
     * @return the saved assembly with its generated {@code id} set
     * @throws SQLException on database error
     */
    public ToolAssembly save(ToolAssembly assembly) throws SQLException {
        String sql = "INSERT INTO tool_assemblies "
                + "(assembly_name, cutting_geometry_id, holder_id, tool_projection, notes) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, assembly.getAssemblyName());
            ps.setLong(2, assembly.getCuttingGeometryId());
            ps.setLong(3, assembly.getHolderId());
            ps.setDouble(4, assembly.getToolProjection());
            ps.setString(5, assembly.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    assembly.setId(keys.getLong(1));
                }
            }
        }
        return assembly;
    }

    /**
     * Finds a tool assembly by its primary key.
     *
     * @param id primary key
     * @return an {@link Optional} containing the assembly, or empty if not found
     * @throws SQLException on database error
     */
    public Optional<ToolAssembly> findById(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM tool_assemblies WHERE id = ?")) {
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
     * Returns all assemblies that use the given cutting geometry.
     *
     * @param cuttingGeometryId FK to cutting_geometry
     * @return matching assemblies
     * @throws SQLException on database error
     */
    public List<ToolAssembly> findByCuttingGeometryId(long cuttingGeometryId) throws SQLException {
        List<ToolAssembly> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM tool_assemblies WHERE cutting_geometry_id = ? ORDER BY id")) {
            ps.setLong(1, cuttingGeometryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * Returns all tool assemblies.
     *
     * @return all assemblies
     * @throws SQLException on database error
     */
    public List<ToolAssembly> findAll() throws SQLException {
        List<ToolAssembly> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tool_assemblies ORDER BY id")) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    /**
     * Deletes a tool assembly by primary key.
     *
     * @param id primary key
     * @return {@code true} if a record was deleted
     * @throws SQLException on database error
     */
    public boolean deleteById(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM tool_assemblies WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private ToolAssembly mapRow(ResultSet rs) throws SQLException {
        ToolAssembly a = new ToolAssembly();
        a.setId(rs.getLong("id"));
        a.setAssemblyName(rs.getString("assembly_name"));
        a.setCuttingGeometryId(rs.getLong("cutting_geometry_id"));
        a.setHolderId(rs.getLong("holder_id"));
        a.setToolProjection(rs.getDouble("tool_projection"));
        a.setNotes(rs.getString("notes"));
        return a;
    }
}
