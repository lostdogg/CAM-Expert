package com.camexpert.kernel.brep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Boundary Representation (B-Rep) solid model.
 *
 * <p>A B-Rep model is the authoritative mathematical definition of a 3-D part.
 * The geometry kernel stores NURBS surfaces, arcs, and spline edges in native C++
 * memory; this Java class holds only the metadata and a native handle so the JNI
 * bridge can locate the underlying data without copying it across the JNI boundary.
 *
 * <p>Instances are created by {@link com.camexpert.kernel.jni.GeometryKernelBridge}
 * and must be disposed via {@link #dispose()} when no longer needed to free the
 * native memory.
 */
public final class BRepModel {

    private final String modelId;
    private final long nativeHandle;
    private final List<BRepFace> faces;
    private boolean waterTight;
    private boolean disposed;

    /**
     * Creates a new B-Rep model with the supplied native handle.
     *
     * @param nativeHandle opaque pointer to the kernel-side solid (0 = null/stub)
     */
    public BRepModel(long nativeHandle) {
        this.modelId = UUID.randomUUID().toString();
        this.nativeHandle = nativeHandle;
        this.faces = new ArrayList<>();
        this.waterTight = false;
        this.disposed = false;
    }

    /**
     * Adds a face to this model's topology.
     *
     * @param face the face to add
     */
    public void addFace(BRepFace face) {
        checkNotDisposed();
        faces.add(face);
    }

    /**
     * Returns an unmodifiable view of the faces in this model.
     *
     * @return faces
     */
    public List<BRepFace> getFaces() {
        checkNotDisposed();
        return Collections.unmodifiableList(faces);
    }

    /** @return unique identifier for this model instance */
    public String getModelId() {
        return modelId;
    }

    /** @return opaque native pointer used by the JNI bridge */
    public long getNativeHandle() {
        checkNotDisposed();
        return nativeHandle;
    }

    /**
     * Returns {@code true} if all gaps between adjacent faces have been healed and
     * the solid forms a closed, water-tight shell.
     *
     * @return {@code true} if water-tight
     */
    public boolean isWaterTight() {
        return waterTight;
    }

    /**
     * Marks the model as water-tight (or not). Called by the geometry healing pipeline
     * after successful gap stitching.
     *
     * @param waterTight {@code true} if the model is now water-tight
     */
    public void setWaterTight(boolean waterTight) {
        this.waterTight = waterTight;
    }

    /**
     * Releases native resources. After calling this method the object must not be used.
     */
    public void dispose() {
        disposed = true;
    }

    /** @return {@code true} if {@link #dispose()} has been called */
    public boolean isDisposed() {
        return disposed;
    }

    private void checkNotDisposed() {
        if (disposed) {
            throw new IllegalStateException("BRepModel has already been disposed: " + modelId);
        }
    }

    @Override
    public String toString() {
        return "BRepModel{id=" + modelId + ", faces=" + faces.size()
                + ", waterTight=" + waterTight + ", disposed=" + disposed + "}";
    }
}
