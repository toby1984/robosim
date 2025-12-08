package de.codesourcery.robosim.kinematic;

import java.awt.Color;
import de.codesourcery.robosim.render.Body;
import de.codesourcery.robosim.render.MeshBuilder;

public class ModelBuilder
{
    public void assignBodies(KinematicChain chain) {

        Part part = chain.firstJoint;
        while ( part != null )
        {
            final Body newBody = switch( part ) {
                case Joint joint -> new Body( MeshBuilder.createCylinder( joint.length, joint.diameter, 32,
                    Color.BLUE.getRGB() ) );
                case Link link ->
                    new Body( MeshBuilder.createBox( link.width, link.height, link.length, Color.RED.getRGB() ) );
            };
            part.setBody( newBody );
            if ( part.previous() != null )
            {
                part.previous().body().addChild( newBody );
            }
            part = part.next();
        }
    }
}
