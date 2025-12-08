package de.codesourcery.robosim.kinematic;

import java.awt.Color;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import de.codesourcery.robosim.render.Body;
import de.codesourcery.robosim.render.Mesh;
import de.codesourcery.robosim.render.MeshBuilder;

public class ModelBuilder
{
    public void assignBodies(KinematicChain chain) {

        Part part = chain.firstJoint;
        while ( part != null )
        {
            final Body newBody = switch( part ) {
                case Joint joint ->
                {
                    final Mesh cylinder = MeshBuilder.createCylinder( joint.length, joint.diameter, 32,
                        Color.BLUE.getRGB() );
                    final Matrix4f rot = new Matrix4f().setRotationXYZ( joint.initialRotation.x, joint.initialRotation.y, joint.initialRotation.z );
                    cylinder.transform( rot, rot.invert(new Matrix4f()).transpose() );
                    yield new Body( cylinder );
                }
                case Link link ->
                {
                    final Mesh box = MeshBuilder.createBox( link.width, link.height, link.length, Color.RED.getRGB() );
                    final Matrix4f rot = new Matrix4f().translationRotate( link.initialPosition, new Quaternionf().rotateXYZ( link.initialRotation.x, link.initialRotation.y, link.initialRotation.z ) );
                    box.transform( rot, rot.invert(new Matrix4f()).transpose() );
                    yield new Body( box );
                }
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
