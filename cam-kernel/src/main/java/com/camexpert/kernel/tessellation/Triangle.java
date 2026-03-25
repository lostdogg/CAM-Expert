package com.camexpert.kernel.tessellation;

/**
 * A triangle in a tessellated mesh, defined by three vertex indices that index
 * into the vertex buffer of a {@link TessellationMesh}.
 *
 * <p>Indices are wound counter-clockwise when viewed from outside the solid,
 * following the OpenGL convention.
 */
public final class Triangle {

    private final int v0;
    private final int v1;
    private final int v2;

    /**
     * Creates a triangle from three vertex indices.
     *
     * @param v0 index of the first vertex
     * @param v1 index of the second vertex
     * @param v2 index of the third vertex
     */
    public Triangle(int v0, int v1, int v2) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
    }

    /** @return first vertex index */
    public int getV0() { return v0; }

    /** @return second vertex index */
    public int getV1() { return v1; }

    /** @return third vertex index */
    public int getV2() { return v2; }

    @Override
    public String toString() {
        return "Triangle{" + v0 + ", " + v1 + ", " + v2 + "}";
    }
}
