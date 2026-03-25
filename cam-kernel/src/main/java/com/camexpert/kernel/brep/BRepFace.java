package com.camexpert.kernel.brep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A single NURBS surface face within a {@link BRepModel}.
 *
 * <p>Each face has an orientation (the outward normal direction) and an ordered
 * list of {@link BRepEdge} instances that form its closed boundary loop.
 */
public final class BRepFace {

    private final String faceId;
    private final List<BRepEdge> edges;
    private double[] normalDirection;

    /** Creates a new face with an auto-generated identifier. */
    public BRepFace() {
        this.faceId = UUID.randomUUID().toString();
        this.edges = new ArrayList<>();
        this.normalDirection = new double[]{0.0, 0.0, 1.0};
    }

    /**
     * Adds an edge to this face's boundary loop.
     *
     * @param edge the boundary edge to add
     */
    public void addEdge(BRepEdge edge) {
        edges.add(edge);
    }

    /**
     * Returns the boundary edges of this face in order.
     *
     * @return unmodifiable list of boundary edges
     */
    public List<BRepEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /** @return unique identifier for this face */
    public String getFaceId() {
        return faceId;
    }

    /**
     * Sets the outward normal direction of this face (unit vector [x, y, z]).
     *
     * @param normal length-3 array; must not be null
     */
    public void setNormalDirection(double[] normal) {
        if (normal == null || normal.length != 3) {
            throw new IllegalArgumentException("Normal must be a 3-element array");
        }
        this.normalDirection = normal.clone();
    }

    /**
     * Returns a copy of the outward normal direction vector [x, y, z].
     *
     * @return normal direction
     */
    public double[] getNormalDirection() {
        return normalDirection.clone();
    }

    @Override
    public String toString() {
        return "BRepFace{id=" + faceId + ", edges=" + edges.size() + "}";
    }
}
