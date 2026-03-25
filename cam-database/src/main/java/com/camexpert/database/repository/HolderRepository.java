package com.camexpert.database.repository;

import com.camexpert.database.model.Holder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Holder} persistence.
 *
 * <p>Provides CRUD operations against the {@code holders} table in the embedded H2 database.
 */
public final class HolderRepository {

    private final Connection connection;

    /**
     * @param connection active JDBC connection; must not be null
     */
    public HolderRepository(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
    }

    /**
     * Persists a new {@link Holder} and sets its generated ID.
     *
     * @param holder the holder to save
     * @return the saved holder with its generated {@code id} set
     * @throws SQLException on database error
     */
    public Holder save(Holder holder) throws SQLException {
        String sql = "INSERT INTO holders "
                + "(name, holder_type, body_diameter, body_length, coupling_diameter) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, holder.getName());
            ps.setString(2, holder.getHolderType());
            ps.setDouble(3, holder.getBodyDiameter());
            ps.setDouble(4, holder.getBodyLength());
            ps.setDouble(5, holder.getCouplingDiameter());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    holder.setId(keys.getLong(1));
                }
            }
        }
        return holder;
    }

    /**
     * Finds a holder by its primary key.
     *
     * @param id primary key
     * @return an {@link Optional} containing the holder, or empty if not found
     * @throws SQLException on database error
     */
    public Optional<Holder> findById(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM holders WHERE id = ?")) {
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
     * Returns all holders in the database.
     *
     * @return list of all holders
     * @throws SQLException on database error
     */
    public List<Holder> findAll() throws SQLException {
        List<Holder> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM holders ORDER BY id")) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    /**
     * Deletes a holder by primary key.
     *
     * @param id primary key
     * @return {@code true} if a record was deleted
     * @throws SQLException on database error
     */
    public boolean deleteById(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM holders WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Holder mapRow(ResultSet rs) throws SQLException {
        Holder h = new Holder();
        h.setId(rs.getLong("id"));
        h.setName(rs.getString("name"));
        h.setHolderType(rs.getString("holder_type"));
        h.setBodyDiameter(rs.getDouble("body_diameter"));
        h.setBodyLength(rs.getDouble("body_length"));
        h.setCouplingDiameter(rs.getDouble("coupling_diameter"));
        return h;
    }
}
