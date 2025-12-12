package de.codesourcery.robosim.kinematic;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.codesourcery.robosim.RendererTest;
import de.codesourcery.robosim.Utils;
import de.codesourcery.robosim.render.Body;

public class ModelBuilder
{
    public void assignBodies(KinematicChain chain) {

        Part part = chain.firstJoint;
        while ( part != null )
        {
            final Body newBody = switch( part ) {
                case Joint joint ->
                {
                    final Matrix4 transform;
                    if ( joint.installOrientation.isZero() )
                    {
                        transform = new Matrix4();
                    }
                    else
                    {
                        transform = Utils.createRotationMatrix( joint.installOrientation );
                    }
                    yield RendererTest.createCylinder( "", joint.length(), joint.diameter(), Color.BLUE, transform );
                }
                case Link link -> RendererTest.createBox( "", link.length(), link.depth(), Color.RED );
            };
            part.setBody( newBody );
            if ( part.previous() != null )
            {
                part.previous().body().addChild( newBody );
                final BoundingBox prevBB = part.previous().body().getInitialBoundingBox();
                final BoundingBox thisBB = newBody.getInitialBoundingBox();
                newBody.setPosition( 0, prevBB.getHeight()/2 + thisBB.getHeight()/2, 0 );
            }
            part = part.next();
        }
        if ( chain.firstJoint != null )
        {
            chain.firstJoint.body.recalculate();
        }
    }
}
