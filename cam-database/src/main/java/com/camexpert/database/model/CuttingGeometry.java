package com.camexpert.database.model;

/**
 * Represents the cutting geometry of a milling cutter.
 *
 * <p>Stores the geometric parameters that define the physical cutting portion of
 * a tool: flute length, outer diameter, corner radius, and flute count. These
 * values are used by toolpath algorithms to compute accurate offsets and by the
 * collision-avoidance system to build the tool's collision mesh.
 */
public final class CuttingGeometry {

    private Long id;
    private String name;
    private double diameter;
    private double fluteLength;
    private double cornerRadius;
    private int numberOfFlutes;
    private double overallLength;

    /** No-argument constructor required for JDBC result-set mapping. */
    public CuttingGeometry() {}

    /**
     * Creates a cutting geometry record.
     *
     * @param name           human-readable tool name
     * @param diameter       cutter outer diameter (mm)
     * @param fluteLength    length of the fluted cutting section (mm)
     * @param cornerRadius   tip/corner radius; 0 for flat end-mills (mm)
     * @param numberOfFlutes number of flutes (cutting edges)
     * @param overallLength  overall tool length from tip to shank shoulder (mm)
     */
    public CuttingGeometry(String name, double diameter, double fluteLength,
                           double cornerRadius, int numberOfFlutes, double overallLength) {
        this.name = name;
        this.diameter = diameter;
        this.fluteLength = fluteLength;
        this.cornerRadius = cornerRadius;
        this.numberOfFlutes = numberOfFlutes;
        this.overallLength = overallLength;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getDiameter() { return diameter; }
    public void setDiameter(double diameter) { this.diameter = diameter; }

    public double getFluteLength() { return fluteLength; }
    public void setFluteLength(double fluteLength) { this.fluteLength = fluteLength; }

    public double getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(double cornerRadius) { this.cornerRadius = cornerRadius; }

    public int getNumberOfFlutes() { return numberOfFlutes; }
    public void setNumberOfFlutes(int numberOfFlutes) { this.numberOfFlutes = numberOfFlutes; }

    public double getOverallLength() { return overallLength; }
    public void setOverallLength(double overallLength) { this.overallLength = overallLength; }

    @Override
    public String toString() {
        return "CuttingGeometry{id=" + id + ", name='" + name + "', dia=" + diameter
                + ", fluteLen=" + fluteLength + ", cornerR=" + cornerRadius
                + ", flutes=" + numberOfFlutes + "}";
    }
}
