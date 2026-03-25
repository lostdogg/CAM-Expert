package com.camexpert.database.model;

/**
 * Represents a tool holder (chuck) used in a tool assembly.
 *
 * <p>A holder is the physical collet or hydraulic chuck that grips the tool
 * shank. Its geometry contributes to the collision mesh computed for the
 * complete assembly.
 */
public final class Holder {

    private Long id;
    private String name;
    private String holderType;
    private double bodyDiameter;
    private double bodyLength;
    private double couplingDiameter;

    /** No-argument constructor required for JDBC result-set mapping. */
    public Holder() {}

    /**
     * Creates a holder record.
     *
     * @param name             human-readable holder name
     * @param holderType       holder standard/type (e.g. "BT40", "HSK63A", "Collet ER32")
     * @param bodyDiameter     maximum outer diameter of the holder body (mm)
     * @param bodyLength       axial length of the holder body (mm)
     * @param couplingDiameter spindle coupling diameter (mm)
     */
    public Holder(String name, String holderType, double bodyDiameter,
                  double bodyLength, double couplingDiameter) {
        this.name = name;
        this.holderType = holderType;
        this.bodyDiameter = bodyDiameter;
        this.bodyLength = bodyLength;
        this.couplingDiameter = couplingDiameter;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHolderType() { return holderType; }
    public void setHolderType(String holderType) { this.holderType = holderType; }

    public double getBodyDiameter() { return bodyDiameter; }
    public void setBodyDiameter(double bodyDiameter) { this.bodyDiameter = bodyDiameter; }

    public double getBodyLength() { return bodyLength; }
    public void setBodyLength(double bodyLength) { this.bodyLength = bodyLength; }

    public double getCouplingDiameter() { return couplingDiameter; }
    public void setCouplingDiameter(double couplingDiameter) { this.couplingDiameter = couplingDiameter; }

    @Override
    public String toString() {
        return "Holder{id=" + id + ", name='" + name + "', type='" + holderType
                + "', dia=" + bodyDiameter + "}";
    }
}
