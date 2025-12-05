package de.codesourcery.robosim;

import org.apache.commons.lang3.Validate;

public class RobotArm
{
    public Joint firstJoint;

    public void addPart(Joint joint) {
        Validate.notNull( joint, "joint must not be null" );
        Validate.notNull( joint.next, "joint.next must not be null" );

        if ( firstJoint == null ) {
            firstJoint = joint;
            return;
        }

        Link current = firstJoint.next;
        while (current.next != null) {
            current = current.next.next;
        }
        current.next = joint;
    }
}
