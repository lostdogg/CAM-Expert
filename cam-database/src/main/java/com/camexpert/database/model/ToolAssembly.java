package com.camexpert.database.model;

/**
 * Represents a tool assembly — the combination of a {@link CuttingGeometry} and a
 * {@link Holder} with a specific tool projection (gauge length).
 *
 * <p>The {@code toolProjection} field records how far the cutter protrudes from
 * the face of the holder. This dimension is critical: combined with the holder and
 * cutter geometries, it determines the exact 3-D collision mesh used by the
 * collision-avoidance algorithms to ensure the holder never crashes into the
 * workpiece or fixtures.
 */
public final class ToolAssembly {

    private Long id;
    private String assemblyName;
    private Long cuttingGeometryId;
    private Long holderId;
    private double toolProjection;
    private String notes;

    /** No-argument constructor required for JDBC result-set mapping. */
    public ToolAssembly() {}

    /**
     * Creates a tool assembly record.
     *
     * @param assemblyName      human-readable assembly name
     * @param cuttingGeometryId FK to {@link CuttingGeometry#getId()}
     * @param holderId          FK to {@link Holder#getId()}
     * @param toolProjection    distance the cutter protrudes from the holder face (mm)
     */
    public ToolAssembly(String assemblyName, long cuttingGeometryId,
                        long holderId, double toolProjection) {
        this.assemblyName = assemblyName;
        this.cuttingGeometryId = cuttingGeometryId;
        this.holderId = holderId;
        this.toolProjection = toolProjection;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssemblyName() { return assemblyName; }
    public void setAssemblyName(String assemblyName) { this.assemblyName = assemblyName; }

    public Long getCuttingGeometryId() { return cuttingGeometryId; }
    public void setCuttingGeometryId(Long cuttingGeometryId) { this.cuttingGeometryId = cuttingGeometryId; }

    public Long getHolderId() { return holderId; }
    public void setHolderId(Long holderId) { this.holderId = holderId; }

    public double getToolProjection() { return toolProjection; }
    public void setToolProjection(double toolProjection) { this.toolProjection = toolProjection; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "ToolAssembly{id=" + id + ", name='" + assemblyName
                + "', cutterId=" + cuttingGeometryId
                + ", holderId=" + holderId
                + ", projection=" + toolProjection + "}";
    }
}
