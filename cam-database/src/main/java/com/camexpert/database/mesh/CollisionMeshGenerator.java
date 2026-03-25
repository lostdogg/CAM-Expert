package com.camexpert.database.mesh;

import com.camexpert.database.model.CuttingGeometry;
import com.camexpert.database.model.Holder;
import com.camexpert.database.model.ToolAssembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates a 3-D collision mesh for a complete tool assembly.
 *
 * <p>The collision mesh is used by the CAM engine's collision-avoidance algorithms
 * to detect and prevent the tool holder from crashing into the workpiece or fixture.
 *
 * <p><b>Algorithm (2-D profile revolved around the tool axis)</b>
 * <ol>
 *   <li>Build a 2-D axial profile made up of rectangular sections representing
 *       the cutter flutes and the holder body.</li>
 *   <li>Revolve the profile around the Z axis using {@code segments} angular
 *       divisions to produce a tessellated solid of revolution.</li>
 * </ol>
 *
 * <p>The resulting mesh is an approximation suitable for real-time collision checks.
 * A finer {@code segments} value produces a more accurate but more expensive mesh.
 */
public final class CollisionMeshGenerator {

    /** Default number of angular segments used when revolving the profile. */
    public static final int DEFAULT_SEGMENTS = 24;

    private final int segments;

    /**
     * Creates a generator with the default segment count ({@value #DEFAULT_SEGMENTS}).
     */
    public CollisionMeshGenerator() {
        this(DEFAULT_SEGMENTS);
    }

    /**
     * Creates a generator with a custom segment count.
     *
     * @param segments number of angular divisions for the solid of revolution (must be >= 3)
     */
    public CollisionMeshGenerator(int segments) {
        if (segments < 3) {
            throw new IllegalArgumentException("segments must be >= 3, got: " + segments);
        }
        this.segments = segments;
    }

    /**
     * Generates the collision mesh for the supplied tool assembly.
     *
     * @param assembly the tool assembly; must not be null
     * @param cutter   the cutting geometry used in this assembly; must not be null
     * @param holder   the holder used in this assembly; must not be null
     * @return a {@link CollisionMesh} containing the revolved triangle mesh
     */
    public CollisionMesh generate(ToolAssembly assembly, CuttingGeometry cutter, Holder holder) {
        if (assembly == null) throw new IllegalArgumentException("assembly must not be null");
        if (cutter == null)   throw new IllegalArgumentException("cutter must not be null");
        if (holder == null)   throw new IllegalArgumentException("holder must not be null");

        List<ProfilePoint> profile = buildProfile(assembly, cutter, holder);
        return revolveProfile(profile);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the 2-D axial profile (radius, z) for the assembly.
     *
     * <pre>
     *  Z
     *  ^
     *  |    [holder body]
     *  |    +-------+
     *  |    |       |   radius = holder.bodyDiameter / 2
     *  |----+       +---+   (shoulder)
     *       |           |   radius = cutter.diameter / 2
     *       +-----------+
     *  0    (tool tip)
     * </pre>
     */
    private List<ProfilePoint> buildProfile(ToolAssembly assembly,
                                            CuttingGeometry cutter, Holder holder) {
        List<ProfilePoint> pts = new ArrayList<>();

        double cutterRadius = cutter.getDiameter() / 2.0;
        double holderRadius = holder.getBodyDiameter() / 2.0;
        double projection   = assembly.getToolProjection();
        double holderLength = holder.getBodyLength();

        // Tool tip at Z = 0
        pts.add(new ProfilePoint(0.0, 0.0));
        // Base of cutter flute section
        pts.add(new ProfilePoint(cutterRadius, 0.0));
        // Top of cutter / bottom of holder (z = projection)
        pts.add(new ProfilePoint(cutterRadius, projection));
        // Holder shoulder step
        pts.add(new ProfilePoint(holderRadius, projection));
        // Top of holder body
        pts.add(new ProfilePoint(holderRadius, projection + holderLength));
        // Close profile on axis
        pts.add(new ProfilePoint(0.0, projection + holderLength));

        return pts;
    }

    /**
     * Revolves the 2-D profile around the Z axis to produce a triangle mesh.
     *
     * @param profile ordered list of (radius, z) points
     * @return the resulting collision mesh
     */
    private CollisionMesh revolveProfile(List<ProfilePoint> profile) {
        List<double[]> vertices = new ArrayList<>();
        List<int[]> triangles  = new ArrayList<>();

        double angleStep = 2.0 * Math.PI / segments;

        // Build vertex grid: [segment][profileIndex]
        int[][] grid = new int[segments][profile.size()];
        for (int s = 0; s < segments; s++) {
            double angle = s * angleStep;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            for (int p = 0; p < profile.size(); p++) {
                ProfilePoint pt = profile.get(p);
                double x = pt.radius * cos;
                double y = pt.radius * sin;
                double z = pt.z;
                vertices.add(new double[]{x, y, z});
                grid[s][p] = vertices.size() - 1;
            }
        }

        // Connect quads between adjacent segments
        for (int s = 0; s < segments; s++) {
            int sNext = (s + 1) % segments;
            for (int p = 0; p < profile.size() - 1; p++) {
                int v00 = grid[s][p];
                int v10 = grid[sNext][p];
                int v11 = grid[sNext][p + 1];
                int v01 = grid[s][p + 1];
                triangles.add(new int[]{v00, v10, v11});
                triangles.add(new int[]{v00, v11, v01});
            }
        }

        return new CollisionMesh(vertices, triangles);
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private static final class ProfilePoint {
        final double radius;
        final double z;

        ProfilePoint(double radius, double z) {
            this.radius = radius;
            this.z = z;
        }
    }

    // -------------------------------------------------------------------------
    // Public result type
    // -------------------------------------------------------------------------

    /**
     * Triangle mesh representing the 3-D envelope of a tool assembly.
     *
     * <p>Vertices are stored as {@code double[3]} arrays (x, y, z).
     * Triangles are stored as {@code int[3]} vertex-index arrays.
     */
    public static final class CollisionMesh {

        private final List<double[]> vertices;
        private final List<int[]> triangles;

        CollisionMesh(List<double[]> vertices, List<int[]> triangles) {
            this.vertices  = Collections.unmodifiableList(vertices);
            this.triangles = Collections.unmodifiableList(triangles);
        }

        /** @return unmodifiable list of vertex positions [x, y, z] */
        public List<double[]> getVertices() { return vertices; }

        /** @return unmodifiable list of triangle index triples [v0, v1, v2] */
        public List<int[]> getTriangles() { return triangles; }

        /** @return number of vertices */
        public int getVertexCount() { return vertices.size(); }

        /** @return number of triangles */
        public int getTriangleCount() { return triangles.size(); }

        @Override
        public String toString() {
            return "CollisionMesh{vertices=" + vertices.size() + ", triangles=" + triangles.size() + "}";
        }
    }
}
