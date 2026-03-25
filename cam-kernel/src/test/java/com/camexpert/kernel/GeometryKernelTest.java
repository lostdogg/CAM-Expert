package com.camexpert.kernel;

import com.camexpert.kernel.brep.BRepEdge;
import com.camexpert.kernel.brep.BRepFace;
import com.camexpert.kernel.brep.BRepModel;
import com.camexpert.kernel.healing.GeometryHealer;
import com.camexpert.kernel.healing.GeometryHealer.HealingReport;
import com.camexpert.kernel.jni.GeometryKernelBridge;
import com.camexpert.kernel.tessellation.TessellationMesh;
import com.camexpert.kernel.tessellation.Tessellator;
import com.camexpert.kernel.tessellation.Triangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeometryKernelTest {

    private GeometryKernelBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new GeometryKernelBridge();
    }

    // ------------------------------------------------------------------
    // BRepModel tests
    // ------------------------------------------------------------------

    @Test
    void bRepModel_initiallyNotWaterTight() {
        BRepModel model = new BRepModel(0L);
        assertFalse(model.isWaterTight());
        assertFalse(model.isDisposed());
    }

    @Test
    void bRepModel_addAndRetrieveFaces() {
        BRepModel model = new BRepModel(0L);
        BRepFace face = new BRepFace();
        model.addFace(face);
        assertEquals(1, model.getFaces().size());
    }

    @Test
    void bRepModel_dispose_preventsUse() {
        BRepModel model = new BRepModel(0L);
        model.dispose();
        assertTrue(model.isDisposed());
        assertThrows(IllegalStateException.class, model::getFaces);
    }

    @Test
    void bRepFace_normalDirection_defaultAndSet() {
        BRepFace face = new BRepFace();
        double[] defaultNormal = face.getNormalDirection();
        assertArrayEquals(new double[]{0.0, 0.0, 1.0}, defaultNormal, 1e-9);

        face.setNormalDirection(new double[]{1.0, 0.0, 0.0});
        assertArrayEquals(new double[]{1.0, 0.0, 0.0}, face.getNormalDirection(), 1e-9);
    }

    @Test
    void bRepEdge_length_calculatedCorrectly() {
        BRepEdge edge = new BRepEdge(new double[]{0, 0, 0}, new double[]{3, 4, 0});
        assertEquals(5.0, edge.length(), 1e-9);
    }

    @Test
    void bRepEdge_invalidVertexThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new BRepEdge(null, new double[]{0, 0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> new BRepEdge(new double[]{0, 0}, new double[]{0, 0, 0}));
    }

    // ------------------------------------------------------------------
    // Tessellator tests
    // ------------------------------------------------------------------

    @Test
    void tessellator_invalidToleranceThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Tessellator(0.0));
        assertThrows(IllegalArgumentException.class, () -> new Tessellator(-1.0));
    }

    @Test
    void tessellator_emptyModelProducesEmptyMesh() {
        BRepModel model = new BRepModel(0L);
        Tessellator t = new Tessellator(0.01);
        TessellationMesh mesh = t.tessellate(model);
        assertEquals(0, mesh.getVertexCount());
        assertEquals(0, mesh.getTriangleCount());
    }

    @Test
    void tessellator_oneFaceProducesTwoTriangles() {
        BRepModel model = new BRepModel(0L);
        model.addFace(new BRepFace());
        Tessellator t = new Tessellator(0.01);
        TessellationMesh mesh = t.tessellate(model);
        assertEquals(4, mesh.getVertexCount());
        assertEquals(2, mesh.getTriangleCount());
    }

    @Test
    void tessellator_nullModelThrows() {
        Tessellator t = new Tessellator(0.01);
        assertThrows(IllegalArgumentException.class, () -> t.tessellate(null));
    }

    @Test
    void tessellator_disposedModelThrows() {
        BRepModel model = new BRepModel(0L);
        model.dispose();
        Tessellator t = new Tessellator(0.01);
        assertThrows(IllegalStateException.class, () -> t.tessellate(model));
    }

    @Test
    void tessellationMesh_vertexAndTriangleStorage() {
        TessellationMesh mesh = new TessellationMesh();
        int idx = mesh.addVertex(1, 2, 3, 0, 0, 1);
        assertEquals(0, idx);
        mesh.addTriangle(new Triangle(0, 1, 2));
        assertEquals(1, mesh.getVertexCount());
        assertEquals(1, mesh.getTriangleCount());
    }

    // ------------------------------------------------------------------
    // GeometryKernelBridge tests
    // ------------------------------------------------------------------

    @Test
    void bridge_importStep_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> bridge.importStep(null));
        assertThrows(IllegalArgumentException.class, () -> bridge.importStep("   "));
    }

    @Test
    void bridge_importStep_stubReturnsBRepModel() {
        BRepModel model = bridge.importStep("/tmp/test.step");
        assertNotNull(model);
        assertFalse(model.isDisposed());
    }

    @Test
    void bridge_tessellate_nullModelThrows() {
        assertThrows(IllegalArgumentException.class, () -> bridge.tessellate(null, 0.01));
    }

    @Test
    void bridge_healGeometry_stubMarksModelWaterTight() {
        BRepModel model = bridge.importStep("/tmp/test.step");
        boolean result = bridge.healGeometry(model, 0.001);
        assertTrue(result);
        assertTrue(model.isWaterTight());
    }

    @Test
    void bridge_healGeometry_invalidToleranceThrows() {
        BRepModel model = bridge.importStep("/tmp/test.step");
        assertThrows(IllegalArgumentException.class, () -> bridge.healGeometry(model, 0.0));
    }

    // ------------------------------------------------------------------
    // GeometryHealer (healing pipeline) tests
    // ------------------------------------------------------------------

    @Test
    void healer_nullBridgeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new GeometryHealer(null, 0.001));
    }

    @Test
    void healer_invalidToleranceThrows() {
        assertThrows(IllegalArgumentException.class, () -> new GeometryHealer(bridge, 0.0));
    }

    @Test
    void healer_nullModelThrows() {
        GeometryHealer healer = new GeometryHealer(bridge, 0.001);
        assertThrows(IllegalArgumentException.class, () -> healer.heal(null));
    }

    @Test
    void healer_disposedModelThrows() {
        GeometryHealer healer = new GeometryHealer(bridge, 0.001);
        BRepModel model = new BRepModel(0L);
        model.dispose();
        assertThrows(IllegalStateException.class, () -> healer.heal(model));
    }

    @Test
    void healer_modelWithNoGapsIsWaterTight() {
        GeometryHealer healer = new GeometryHealer(bridge, 0.001);
        BRepModel model = bridge.importStep("/tmp/test.step");
        HealingReport report = healer.heal(model);
        assertEquals(0, report.getDetectedGaps());
        assertTrue(report.isWaterTight());
    }

    @Test
    void healer_detectsGapBetweenAdjacentFaces() {
        GeometryHealer healer = new GeometryHealer(bridge, 0.01);
        BRepModel model = new BRepModel(0L);

        // Face 1 with edge ending at (1, 0, 0)
        BRepFace face1 = new BRepFace();
        face1.addEdge(new BRepEdge(new double[]{0, 0, 0}, new double[]{1, 0, 0}));
        model.addFace(face1);

        // Face 2 with edge starting at (1.005, 0, 0) — 0.005 gap, within tolerance
        BRepFace face2 = new BRepFace();
        face2.addEdge(new BRepEdge(new double[]{1.005, 0, 0}, new double[]{2, 0, 0}));
        model.addFace(face2);

        HealingReport report = healer.heal(model);
        assertEquals(1, report.getDetectedGaps());
        assertEquals(1, report.getStitchedGaps());
        assertTrue(report.isWaterTight());
    }
}
