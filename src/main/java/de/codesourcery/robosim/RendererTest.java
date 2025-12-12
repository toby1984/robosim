package de.codesourcery.robosim;

import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import de.codesourcery.robosim.kinematic.Joint;
import de.codesourcery.robosim.kinematic.KinematicChain;
import de.codesourcery.robosim.kinematic.Link;
import de.codesourcery.robosim.kinematic.ModelBuilder;
import de.codesourcery.robosim.render.Body;
import de.codesourcery.robosim.render.MeshRenderer;

public class RendererTest
{
    private static final List<Body> topLevelBodies = new ArrayList<>();
    private static final List<Body> bodiesToRender = new ArrayList<>();

    static void main()
    {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("RoboSim");

        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS( 60 );

        config.setWindowedMode(800, 600);
        new Lwjgl3Application(new MeshRenderer( () -> {
            setupBodies();
            return bodiesToRender;
        }), config);
    }

    private static void setupKinematicsChain() {

        final KinematicChain chain = new KinematicChain();

        final float linkLen = 50;

        final Joint base = chain.addPart( new Joint( "Base", 10, 100 ) );
        base.installOrientation.set( 0, 0, 90 );
        chain.addPart( new Link(  "Link #1" , linkLen, 20 ) );
        chain.addPart( new Joint( "Shoulder", 100, 10 ) );
        chain.addPart( new Link(  "Link #2" , linkLen, 20 ) );
        chain.addPart( new Joint( "Elbow"   , 100, 10 ) );
        chain.addPart( new Link(  "Link #3" , linkLen, 20) );
        chain.addPart( new Joint( "Wrist #1", 100, 10 ) );
        chain.addPart( new Link(  "Link #4" , linkLen, 20 ) );
        chain.addPart( new Joint( "Wrist #2", 100, 10 ) );
        chain.addPart( new Link(  "Gripper" , linkLen, 20 ) );

        new ModelBuilder().assignBodies( chain );

        chain.firstJoint.body.setRotation( 0,0,90 );

        topLevelBodies.add( chain.firstJoint.body() );
        System.out.println( "Top-level bodies: " + topLevelBodies.size() );
        bodiesToRender.addAll( chain.firstJoint.getAllBodies() );
        System.out.println("Bodies to render: "+bodiesToRender.size());
    }

    private static void setupParentChild() {
        final Body b1 = createBox( "b1", 100, 1, 100, com.badlogic.gdx.graphics.Color.RED );
        // final Body b2 = createBox( "b1", 10, 10, 10, com.badlogic.gdx.graphics.Color.BLUE );
        // final Body b2 = createCylinder( "b2",100, 10, com.badlogic.gdx.graphics.Color.BLUE );
        final Body b2 = createCylinder( "b3", 100, 10, com.badlogic.gdx.graphics.Color.BLUE );
//        final Body b3 = createCylinder( "b3", 100, 10, com.badlogic.gdx.graphics.Color.BLUE );
        b1.addChild( b2 );
        // b2.addChild( b3 );
        b2.setPosition( 0,50,0 );
//        b3.setRotation( 0,0, 90 );
//        b3.setPosition( 50,0,0 );
        topLevelBodies.addAll( List.of( b1 ) );
        // bodiesToRender.addAll( java.util.List.of( b1, b2, b3 ) );
        bodiesToRender.addAll( java.util.List.of( b1, b2) );
    }

    public static Body createCylinder(String name, float length, float diameter, com.badlogic.gdx.graphics.Color color) {
        return createCylinder( name, length, diameter, color, new Matrix4() );
    }

    public static Body createCylinder(String name, float length, float diameter, com.badlogic.gdx.graphics.Color color, Matrix4 transform)
    {
        final VertexAttributes attributes = new VertexAttributes(
            new VertexAttribute( VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
            new VertexAttribute( VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE ),
            new VertexAttribute( VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE )
        );

        com.badlogic.gdx.graphics.g3d.utils.MeshBuilder builder =
            new com.badlogic.gdx.graphics.g3d.utils.MeshBuilder();
        builder.begin(attributes, GL20.GL_TRIANGLES);
        builder.setColor(color );

        final Matrix4 initialRot = new Matrix4().setToRotation( new Vector3( 0, 0, 1 ), 90 );
        initialRot.mul( transform );

        builder.setVertexTransformationEnabled( true );
        builder.setVertexTransform( initialRot );

        builder.cylinder(diameter, length, diameter, 64 );

        return new Body( builder.end() , name );
    }

    public static Body createBox(String name, float length, float width, com.badlogic.gdx.graphics.Color color) {
        //noinspection SuspiciousNameCombination
        return createBox( name, length, width, width, color );
    }

    public static Body createBox(String name, float width, float height, float depth, com.badlogic.gdx.graphics.Color color)
    {
        final VertexAttributes attributes = new VertexAttributes(
            new VertexAttribute( VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
            new VertexAttribute( VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE ),
            new VertexAttribute( VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE )
        );

        com.badlogic.gdx.graphics.g3d.utils.MeshBuilder builder =
            new com.badlogic.gdx.graphics.g3d.utils.MeshBuilder();
        builder.begin(attributes, GL20.GL_TRIANGLES);

        builder.setColor( color );
        builder.box(width, height, depth);

        return new Body( builder.end() , name );
    }

    private static void setupBodies() {

//        final Body b1 = createCylinder("b1", 100,10,com.badlogic.gdx.graphics.Color.RED );
//        final Body b1 = createBox("b1", 100,20, com.badlogic.gdx.graphics.Color.RED );
//        topLevelBodies.add( b1 );
//        bodiesToRender.add( b1 );

         setupKinematicsChain();
//         setupParentChild();
    }
}