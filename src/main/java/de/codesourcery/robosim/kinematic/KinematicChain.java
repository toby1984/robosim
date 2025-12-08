package de.codesourcery.robosim.kinematic;

import org.apache.commons.lang3.Validate;

public class KinematicChain
{
    public Joint firstJoint;

    public void addPart(Part part) {
        Validate.notNull( part, "joint must not be null" );

        if ( firstJoint == null ) {
            firstJoint = (Joint) part;
            return;
        }
        Part current = firstJoint;
        while (current.next() != null) {
            current = current.next();
        }
        current.setNext( part );
    }
}
