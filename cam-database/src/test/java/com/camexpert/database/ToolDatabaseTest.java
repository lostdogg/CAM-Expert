package com.camexpert.database;

import com.camexpert.database.mesh.CollisionMeshGenerator;
import com.camexpert.database.mesh.CollisionMeshGenerator.CollisionMesh;
import com.camexpert.database.model.CuttingGeometry;
import com.camexpert.database.model.Holder;
import com.camexpert.database.model.ToolAssembly;
import com.camexpert.database.repository.CuttingGeometryRepository;
import com.camexpert.database.repository.HolderRepository;
import com.camexpert.database.repository.ToolAssemblyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ToolDatabaseTest {

    private ToolDatabase db;
    private CuttingGeometryRepository geomRepo;
    private HolderRepository holderRepo;
    private ToolAssemblyRepository assemblyRepo;

    @BeforeEach
    void setUp() throws SQLException {
        db = ToolDatabase.inMemory("test-" + System.nanoTime());
        db.initialize();
        geomRepo    = new CuttingGeometryRepository(db.getConnection());
        holderRepo  = new HolderRepository(db.getConnection());
        assemblyRepo = new ToolAssemblyRepository(db.getConnection());
    }

    @AfterEach
    void tearDown() throws SQLException {
        db.close();
    }

    // ------------------------------------------------------------------
    // ToolDatabase lifecycle
    // ------------------------------------------------------------------

    @Test
    void initialize_createsSchema() throws SQLException {
        // If schema creation failed, repositories would throw on insert.
        CuttingGeometry g = geomRepo.save(
                new CuttingGeometry("Test", 10.0, 25.0, 0.0, 4, 75.0));
        assertNotNull(g.getId());
    }

    @Test
    void getConnection_throwsIfNotInitialised() {
        ToolDatabase uninitialisedDb = new ToolDatabase("jdbc:h2:mem:uninit");
        assertThrows(IllegalStateException.class, uninitialisedDb::getConnection);
    }

    // ------------------------------------------------------------------
    // CuttingGeometry CRUD
    // ------------------------------------------------------------------

    @Test
    void cuttingGeometry_saveAndFindById() throws SQLException {
        CuttingGeometry saved = geomRepo.save(
                new CuttingGeometry("6mm Ball", 6.0, 20.0, 3.0, 2, 60.0));
        assertNotNull(saved.getId());

        Optional<CuttingGeometry> found = geomRepo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("6mm Ball", found.get().getName());
        assertEquals(6.0, found.get().getDiameter(), 1e-9);
        assertEquals(3.0, found.get().getCornerRadius(), 1e-9);
    }

    @Test
    void cuttingGeometry_findAll() throws SQLException {
        geomRepo.save(new CuttingGeometry("A", 8.0, 22.0, 0.0, 4, 72.0));
        geomRepo.save(new CuttingGeometry("B", 12.0, 30.0, 1.0, 3, 80.0));

        List<CuttingGeometry> all = geomRepo.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void cuttingGeometry_deleteById() throws SQLException {
        CuttingGeometry saved = geomRepo.save(
                new CuttingGeometry("Del", 5.0, 15.0, 0.0, 4, 50.0));
        assertTrue(geomRepo.deleteById(saved.getId()));
        assertTrue(geomRepo.findById(saved.getId()).isEmpty());
    }

    @Test
    void cuttingGeometry_findById_returnsEmpty_forMissingId() throws SQLException {
        assertTrue(geomRepo.findById(9999L).isEmpty());
    }

    // ------------------------------------------------------------------
    // Holder CRUD
    // ------------------------------------------------------------------

    @Test
    void holder_saveAndFindById() throws SQLException {
        Holder saved = holderRepo.save(
                new Holder("HSK63 Collet", "HSK63A", 63.0, 90.0, 63.0));
        assertNotNull(saved.getId());

        Optional<Holder> found = holderRepo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("HSK63 Collet", found.get().getName());
        assertEquals("HSK63A", found.get().getHolderType());
    }

    @Test
    void holder_findAll() throws SQLException {
        holderRepo.save(new Holder("H1", "BT40", 63.0, 80.0, 44.45));
        holderRepo.save(new Holder("H2", "BT30", 50.0, 70.0, 31.75));

        assertEquals(2, holderRepo.findAll().size());
    }

    @Test
    void holder_deleteById() throws SQLException {
        Holder saved = holderRepo.save(new Holder("H3", "BT40", 63.0, 80.0, 44.45));
        assertTrue(holderRepo.deleteById(saved.getId()));
        assertTrue(holderRepo.findById(saved.getId()).isEmpty());
    }

    // ------------------------------------------------------------------
    // ToolAssembly CRUD and relational integrity
    // ------------------------------------------------------------------

    @Test
    void toolAssembly_saveAndFindById() throws SQLException {
        CuttingGeometry cutter = geomRepo.save(
                new CuttingGeometry("10mm EM", 10.0, 30.0, 0.0, 4, 80.0));
        Holder holder = holderRepo.save(
                new Holder("BT40 Collet", "BT40", 63.0, 90.0, 44.45));

        ToolAssembly saved = assemblyRepo.save(
                new ToolAssembly("Assembly-1", cutter.getId(), holder.getId(), 45.0));
        assertNotNull(saved.getId());

        Optional<ToolAssembly> found = assemblyRepo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Assembly-1", found.get().getAssemblyName());
        assertEquals(45.0, found.get().getToolProjection(), 1e-9);
    }

    @Test
    void toolAssembly_findByCuttingGeometryId() throws SQLException {
        CuttingGeometry c1 = geomRepo.save(
                new CuttingGeometry("C1", 6.0, 20.0, 0.0, 4, 60.0));
        CuttingGeometry c2 = geomRepo.save(
                new CuttingGeometry("C2", 8.0, 25.0, 0.0, 4, 70.0));
        Holder h = holderRepo.save(
                new Holder("H", "BT40", 63.0, 90.0, 44.45));

        assemblyRepo.save(new ToolAssembly("Asm1", c1.getId(), h.getId(), 40.0));
        assemblyRepo.save(new ToolAssembly("Asm2", c1.getId(), h.getId(), 50.0));
        assemblyRepo.save(new ToolAssembly("Asm3", c2.getId(), h.getId(), 60.0));

        List<ToolAssembly> forC1 = assemblyRepo.findByCuttingGeometryId(c1.getId());
        assertEquals(2, forC1.size());
    }

    @Test
    void toolAssembly_deleteById() throws SQLException {
        CuttingGeometry c = geomRepo.save(
                new CuttingGeometry("Del", 5.0, 15.0, 0.0, 4, 50.0));
        Holder h = holderRepo.save(
                new Holder("DelH", "BT30", 50.0, 70.0, 31.75));
        ToolAssembly a = assemblyRepo.save(
                new ToolAssembly("DelAsm", c.getId(), h.getId(), 30.0));

        assertTrue(assemblyRepo.deleteById(a.getId()));
        assertTrue(assemblyRepo.findById(a.getId()).isEmpty());
    }

    // ------------------------------------------------------------------
    // CollisionMeshGenerator
    // ------------------------------------------------------------------

    @Test
    void collisionMesh_generatesCorrectVertexAndTriangleCount() {
        CollisionMeshGenerator gen = new CollisionMeshGenerator(24);
        CuttingGeometry cutter = new CuttingGeometry("Test", 10.0, 30.0, 0.0, 4, 80.0);
        Holder holder = new Holder("H", "BT40", 63.0, 90.0, 44.45);
        ToolAssembly assembly = new ToolAssembly("Asm", 1L, 1L, 45.0);

        CollisionMesh mesh = gen.generate(assembly, cutter, holder);

        // Profile has 6 points → 5 quad spans → 10 triangles per segment × 24 segments = 240
        assertEquals(24 * 6, mesh.getVertexCount());
        assertEquals(24 * 5 * 2, mesh.getTriangleCount());
    }

    @Test
    void collisionMesh_invalidSegmentsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new CollisionMeshGenerator(2));
    }

    @Test
    void collisionMesh_nullArgumentsThrow() {
        CollisionMeshGenerator gen = new CollisionMeshGenerator();
        CuttingGeometry c = new CuttingGeometry("C", 10.0, 25.0, 0.0, 4, 75.0);
        Holder h = new Holder("H", "BT40", 63.0, 90.0, 44.45);
        ToolAssembly a = new ToolAssembly("A", 1L, 1L, 40.0);

        assertThrows(IllegalArgumentException.class, () -> gen.generate(null, c, h));
        assertThrows(IllegalArgumentException.class, () -> gen.generate(a, null, h));
        assertThrows(IllegalArgumentException.class, () -> gen.generate(a, c, null));
    }
}
