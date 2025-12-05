package de.codesourcery.robosim;

public final class Link implements ArmPart
{
    /** next joint (towards gripper) */
    public Joint next;
    /** previous joint (towards base of arm) */
    public Joint previous;

    /** length of this link in cm */
    public double length;
    /** stiffness of this link */
    public double stiffness;
    /** cross-section of this link in cm^2 */
    public double crossSection;
    /** density in g/cm3 */
    public double density;
}
