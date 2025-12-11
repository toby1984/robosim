package de.codesourcery.robosim;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import de.codesourcery.robosim.kinematic.Joint;
import de.codesourcery.robosim.kinematic.KinematicChain;
import de.codesourcery.robosim.kinematic.Link;
import de.codesourcery.robosim.kinematic.ModelBuilder;
import de.codesourcery.robosim.render.Body;
import de.codesourcery.robosim.render.MeshBuilder;
import de.codesourcery.robosim.render.MeshRenderer;

public class RendererTest
{
    private static final List<Body> topLevelBodies = new ArrayList<>();
    private static final List<Body> bodiesToRender = new ArrayList<>();

    static void main()
    {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("RoboSim");
        config.setWindowedMode(800, 600);
        new Lwjgl3Application(new MeshRenderer( () -> {
            setupBodies();
            return bodiesToRender;
        }), config);
    }

    private static void setupKinematicsChain() {

        final KinematicChain chain = new KinematicChain();

        final Joint j1 = new Joint( "Base" );
        j1.length = 10;
        j1.diameter = 100;
        chain.addPart( j1 );

        final Link l1 = new Link( "link #1" );
        l1.length = 20;
        l1.width = 25;
        l1.height = 100;
        chain.addPart( l1 );

        final Joint j2 = new Joint( "Shoulder" );
        j2.length = 100;
        j2.diameter = 10;
        chain.addPart( j2 );

        new ModelBuilder().assignBodies( chain );

        j1.body().setRotation( 0,0, (float) (Math.PI / 2) );
        l1.body().setPosition( 0,0,50);
        l1.body().setRotation( 0,0, (float) (Math.PI / 2) );
        j2.body().setRotation( 0,0,0);

        topLevelBodies.add( chain.firstJoint.body() );
        System.out.println( "Top-level bodies: " + topLevelBodies.size() );
        bodiesToRender.addAll( chain.firstJoint.getAllBodies() );
        System.out.println("Bodies to render: "+bodiesToRender.size());
    }

    private static void setupParentChild() {
        final Body b1 = new Body( MeshBuilder.createBox( 100, 2, 100, Color.RED.getRGB() ) , "b1" );
        final Body b2 = new Body( MeshBuilder.createCylinder( 100, 10, 10, Color.BLUE.getRGB() ) , "b2" );
        final Body b3 = new Body( MeshBuilder.createCylinder( 100, 10, 10, Color.BLUE.getRGB() ) , "b3" );
        b1.addChild( b2 );
        b2.addChild( b3 );
        b2.setRotation( 0,0, (float) (Math.PI / 2) );
        b2.setPosition( 0,52,0 );
        b3.setRotation( 0,0, (float) (Math.PI / 2) );
        b3.setPosition( 50,0,0 );
        topLevelBodies.addAll( List.of( b1 ) );
        bodiesToRender.addAll( java.util.List.of( b1, b2, b3 ) );
    }

    private static void setupBodies() {

        final VertexAttributes attributes = new VertexAttributes(
            new VertexAttribute( VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
            new VertexAttribute( VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE ),
            new VertexAttribute( VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE )
        );

        com.badlogic.gdx.graphics.g3d.utils.MeshBuilder builder =
            new com.badlogic.gdx.graphics.g3d.utils.MeshBuilder();
        builder.begin(attributes, GL20.GL_TRIANGLES);

        // **BoxShapeBuilder Usage**
        // build() method takes the dimensions (width, height, depth) for a centered box.
        // We also pass the color attribute here to make the whole box red.
        builder.setColor( com.badlogic.gdx.graphics.Color.RED );
        builder.box(50f, 50f, 50f);

        final Body b1 = new Body( builder.end() , "b1" );
        topLevelBodies.add( b1 );
        bodiesToRender.add( b1 );

        // setupKinematicsChain();
        // setupParentChild();
    }
}