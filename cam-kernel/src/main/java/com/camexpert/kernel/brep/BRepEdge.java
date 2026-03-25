package com.camexpert.kernel.brep;

import java.util.UUID;

/**
 * A single topological edge within a {@link BRepFace}.
 *
 * <p>An edge is a bounded curve (line, arc, or spline segment) connecting two
 * vertices. Adjacent faces share edges, and a gap between two edges on adjacent
 * faces is exactly what the geometry healing algorithm stitches closed.
 */
public final class BRepEdge {

    private final String edgeId;
    private final double[] startVertex;
    private final double[] endVertex;

    /**
     * Creates a new edge with the given start and end vertices.
     *
     * @param startVertex 3-D start point [x, y, z]
     * @param endVertex   3-D end point [x, y, z]
     */
    public BRepEdge(double[] startVertex, double[] endVertex) {
        if (startVertex == null || startVertex.length != 3) {
            throw new IllegalArgumentException("startVertex must be a 3-element array");
        }
        if (endVertex == null || endVertex.length != 3) {
            throw new IllegalArgumentException("endVertex must be a 3-element array");
        }
        this.edgeId = UUID.randomUUID().toString();
        this.startVertex = startVertex.clone();
        this.endVertex = endVertex.clone();
    }

    /** @return unique edge identifier */
    public String getEdgeId() {
        return edgeId;
    }

    /** @return copy of start vertex [x, y, z] */
    public double[] getStartVertex() {
        return startVertex.clone();
    }

    /** @return copy of end vertex [x, y, z] */
    public double[] getEndVertex() {
        return endVertex.clone();
    }

    /**
     * Returns the Euclidean distance between start and end vertices (edge length).
     *
     * @return edge length
     */
    public double length() {
        double dx = endVertex[0] - startVertex[0];
        double dy = endVertex[1] - startVertex[1];
        double dz = endVertex[2] - startVertex[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return "BRepEdge{id=" + edgeId
                + ", start=[" + startVertex[0] + "," + startVertex[1] + "," + startVertex[2] + "]"
                + ", end=[" + endVertex[0] + "," + endVertex[1] + "," + endVertex[2] + "]}";
    }
}
