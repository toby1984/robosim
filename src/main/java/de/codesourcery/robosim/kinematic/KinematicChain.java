package de.codesourcery.robosim.kinematic;

import java.util.Random;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import de.codesourcery.robosim.Utils;

public class KinematicChain
{
    public Joint firstJoint;

    public void assignRandomAngles(Random rnd) {
        forEachJoint( j -> {
            final float maxAngle = "Base".equals( j.name ) ? 360 : 80;
            final float angleInDeg = maxAngle * rnd.nextFloat();
            j.motor.setDesiredAngle( Utils.degToRad( angleInDeg ) );
            j.updateRotationFromMotor();
        } );
    }

    public void forEachJoint(Consumer<Joint> visitor) {

        Part current = firstJoint;
        while ( current != null ) {
            if ( current instanceof Joint j) {
                visitor.accept(j);
            }
            current = current.next();
        }
    }

    public <T extends Part> T addPart(T part) {
        Validate.notNull( part, "joint must not be null" );

        if ( firstJoint == null ) {
            firstJoint = (Joint) part;
            return part;
        }
        Part current = firstJoint;
        while (current.next() != null) {
            current = current.next();
        }
        current.setNext( part );
        return part;
    }
}
