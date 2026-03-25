package com.camexpert.kernel.jni;

import com.camexpert.kernel.brep.BRepModel;
import com.camexpert.kernel.tessellation.TessellationMesh;
import com.camexpert.kernel.tessellation.Tessellator;

import java.util.logging.Logger;

/**
 * Java-side gateway to the native geometry kernel (e.g. Parasolid, ACIS).
 *
 * <p><b>Architecture overview</b>
 * <pre>
 *   Java code  →  GeometryKernelBridge  →  JNI  →  native C++ kernel
 * </pre>
 *
 * <p>Each public method delegates to a {@code native} counterpart declared at the
 * bottom of this class. When the native shared library ({@code libcamkernel}) is
 * available on the JVM's library path it is loaded by {@link #loadNativeLibrary()};
 * otherwise the bridge automatically falls back to a pure-Java stub implementation
 * so that the rest of the stack can compile and be tested without a real kernel.
 *
 * <p><b>Usage</b>
 * <pre>{@code
 * GeometryKernelBridge bridge = new GeometryKernelBridge();
 * BRepModel model = bridge.importStep("/path/to/part.step");
 * bridge.healGeometry(model, 0.001);
 * TessellationMesh mesh = bridge.tessellate(model, 0.01);
 * }</pre>
 */
public final class GeometryKernelBridge {

    private static final Logger LOG = Logger.getLogger(GeometryKernelBridge.class.getName());

    /** {@code true} when the native shared library was successfully loaded. */
    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("camkernel");
            loaded = true;
            LOG.info("Native geometry kernel library loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            LOG.warning("Native geometry kernel library not found; using Java stub. " + e.getMessage());
        }
        NATIVE_AVAILABLE = loaded;
    }

    /**
     * Returns {@code true} if the native kernel library is loaded and available.
     *
     * @return {@code true} if native
     */
    public boolean isNativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    /**
     * Imports a STEP file and returns the resulting B-Rep model.
     *
     * <p>Delegates to the native kernel when available; otherwise returns a
     * minimal stub model suitable for testing the downstream pipeline.
     *
     * @param stepFilePath absolute path to the STEP file
     * @return the imported B-Rep model
     * @throws IllegalArgumentException if {@code stepFilePath} is null or empty
     */
    public BRepModel importStep(String stepFilePath) {
        if (stepFilePath == null || stepFilePath.isBlank()) {
            throw new IllegalArgumentException("stepFilePath must not be null or empty");
        }
        if (NATIVE_AVAILABLE) {
            long handle = nativeImportStep(stepFilePath);
            return new BRepModel(handle);
        }
        LOG.fine("Stub: importStep(" + stepFilePath + ")");
        return new BRepModel(0L);
    }

    /**
     * Requests the kernel to tessellate {@code model} into a GPU-renderable triangle mesh.
     *
     * @param model          the B-Rep model to tessellate
     * @param chordTolerance maximum chord deviation (model units, > 0)
     * @return the resulting triangle mesh
     */
    public TessellationMesh tessellate(BRepModel model, double chordTolerance) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        if (NATIVE_AVAILABLE) {
            return nativeTessellate(model.getNativeHandle(), chordTolerance);
        }
        // Java-side fallback tessellator
        return new Tessellator(chordTolerance).tessellate(model);
    }

    /**
     * Heals the geometry of {@code model} so that it becomes a water-tight solid.
     *
     * <p>Stitches microscopic gaps between adjacent surfaces (up to
     * {@code gapTolerance} model units) and sets {@link BRepModel#setWaterTight(boolean)}
     * to {@code true} on success.
     *
     * @param model        the B-Rep model to heal; must not be null or disposed
     * @param gapTolerance maximum gap size to stitch (model units, > 0)
     * @return {@code true} if the model is water-tight after healing
     */
    public boolean healGeometry(BRepModel model, double gapTolerance) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        if (gapTolerance <= 0.0) {
            throw new IllegalArgumentException("gapTolerance must be positive, got: " + gapTolerance);
        }
        boolean success;
        if (NATIVE_AVAILABLE) {
            success = nativeHealGeometry(model.getNativeHandle(), gapTolerance);
        } else {
            // Stub: optimistically mark as healed so downstream code can proceed
            success = true;
        }
        model.setWaterTight(success);
        return success;
    }

    // -------------------------------------------------------------------------
    // Native method declarations – implemented in libcamkernel (C++)
    // -------------------------------------------------------------------------

    private native long nativeImportStep(String stepFilePath);

    private native TessellationMesh nativeTessellate(long nativeHandle, double chordTolerance);

    private native boolean nativeHealGeometry(long nativeHandle, double gapTolerance);

    /**
     * Attempts to load the native geometry kernel shared library.
     * Called reflectively by integration tests that need to verify the loading logic.
     */
    static void loadNativeLibrary() {
        // Intentionally left empty; loading is triggered by the static initializer.
    }
}
