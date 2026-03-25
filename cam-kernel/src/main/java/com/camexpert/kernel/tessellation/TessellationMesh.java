package com.camexpert.kernel.tessellation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triangle mesh produced by tessellating a {@link com.camexpert.kernel.brep.BRepModel}.
 *
 * <p>This is the GPU-ready representation of a solid. The vertex buffer stores
 * interleaved position and normal data ([x, y, z, nx, ny, nz] per vertex), and
 * the index buffer stores triangles as groups of three vertex indices.
 *
 * <p>Instances are created by {@link Tessellator} and pushed to the graphics engine
 * (OpenGL / Vulkan) by the rendering subsystem.
 */
public final class TessellationMesh {

    private final List<double[]> vertices;
    private final List<Triangle> triangles;

    /** Creates an empty tessellation mesh. */
    public TessellationMesh() {
        this.vertices = new ArrayList<>();
        this.triangles = new ArrayList<>();
    }

    /**
     * Appends a vertex (position + normal) to the mesh.
     *
     * @param x  position X
     * @param y  position Y
     * @param z  position Z
     * @param nx normal X
     * @param ny normal Y
     * @param nz normal Z
     * @return index of the newly added vertex
     */
    public int addVertex(double x, double y, double z, double nx, double ny, double nz) {
        vertices.add(new double[]{x, y, z, nx, ny, nz});
        return vertices.size() - 1;
    }

    /**
     * Appends a triangle to the mesh.
     *
     * @param triangle the triangle to add
     */
    public void addTriangle(Triangle triangle) {
        triangles.add(triangle);
    }

    /**
     * Returns an unmodifiable view of the vertex list.
     * Each element is a 6-element array [x, y, z, nx, ny, nz].
     *
     * @return vertices
     */
    public List<double[]> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    /**
     * Returns an unmodifiable view of the triangle list.
     *
     * @return triangles
     */
    public List<Triangle> getTriangles() {
        return Collections.unmodifiableList(triangles);
    }

    /** @return number of vertices in this mesh */
    public int getVertexCount() {
        return vertices.size();
    }

    /** @return number of triangles in this mesh */
    public int getTriangleCount() {
        return triangles.size();
    }

    @Override
    public String toString() {
        return "TessellationMesh{vertices=" + vertices.size() + ", triangles=" + triangles.size() + "}";
    }
}
