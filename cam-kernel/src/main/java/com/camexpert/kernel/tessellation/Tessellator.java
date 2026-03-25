package com.camexpert.kernel.tessellation;

import com.camexpert.kernel.brep.BRepFace;
import com.camexpert.kernel.brep.BRepModel;

/**
 * Converts a {@link BRepModel} into a GPU-renderable {@link TessellationMesh}.
 *
 * <p>In production the heavy lifting is delegated to the native geometry kernel
 * via the JNI bridge. This Java-side implementation provides a pure-Java fallback
 * that approximates each face with a simple quad split into two triangles, allowing
 * the rendering pipeline and tests to run without native libraries on the classpath.
 *
 * <p>The {@code chordTolerance} parameter controls the maximum deviation (in model
 * units) between the true NURBS surface and the linearised triangle mesh. Smaller
 * values produce finer meshes and higher GPU load.
 */
public final class Tessellator {

    private final double chordTolerance;

    /**
     * Creates a tessellator with the specified chord tolerance.
     *
     * @param chordTolerance maximum deviation from true surface geometry (model units)
     * @throws IllegalArgumentException if {@code chordTolerance <= 0}
     */
    public Tessellator(double chordTolerance) {
        if (chordTolerance <= 0.0) {
            throw new IllegalArgumentException("chordTolerance must be positive, got: " + chordTolerance);
        }
        this.chordTolerance = chordTolerance;
    }

    /**
     * Tessellates the supplied B-Rep model into a triangle mesh.
     *
     * <p>This pure-Java implementation generates a minimal representative mesh:
     * each face contributes two triangles from a planar quad. The native kernel
     * would replace this with adaptive subdivision of NURBS surfaces.
     *
     * @param model the B-Rep model to tessellate; must not be null or disposed
     * @return a new {@link TessellationMesh} containing all faces
     * @throws IllegalArgumentException if {@code model} is null
     * @throws IllegalStateException    if {@code model} has been disposed
     */
    public TessellationMesh tessellate(BRepModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        if (model.isDisposed()) {
            throw new IllegalStateException("Cannot tessellate a disposed BRepModel");
        }

        TessellationMesh mesh = new TessellationMesh();

        for (BRepFace face : model.getFaces()) {
            double[] n = face.getNormalDirection();
            tessellateFace(mesh, face, n);
        }

        return mesh;
    }

    /** @return the chord tolerance used by this tessellator */
    public double getChordTolerance() {
        return chordTolerance;
    }

    private void tessellateFace(TessellationMesh mesh, BRepFace face, double[] n) {
        // Stub: produce a unit-quad placeholder centred on the origin.
        // The native kernel replaces this with adaptive NURBS subdivision.
        int v0 = mesh.addVertex(-0.5, -0.5, 0, n[0], n[1], n[2]);
        int v1 = mesh.addVertex( 0.5, -0.5, 0, n[0], n[1], n[2]);
        int v2 = mesh.addVertex( 0.5,  0.5, 0, n[0], n[1], n[2]);
        int v3 = mesh.addVertex(-0.5,  0.5, 0, n[0], n[1], n[2]);
        mesh.addTriangle(new Triangle(v0, v1, v2));
        mesh.addTriangle(new Triangle(v0, v2, v3));
    }
}
