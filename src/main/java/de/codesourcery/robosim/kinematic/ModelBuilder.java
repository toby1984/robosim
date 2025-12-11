package de.codesourcery.robosim.kinematic;

import com.badlogic.gdx.graphics.Color;
import de.codesourcery.robosim.RendererTest;
import de.codesourcery.robosim.render.Body;

public class ModelBuilder
{
    public void assignBodies(KinematicChain chain) {

        Part part = chain.firstJoint;
        while ( part != null )
        {
            final Body newBody = switch( part ) {
                case Joint joint -> RendererTest.createCylinder( "", joint.length, joint.diameter, Color.BLUE );
                case Link link ->
                    RendererTest.createBox( "", link.width, link.height, link.length, Color.RED ) ;
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
