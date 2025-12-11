package de.codesourcery.robosim.render;

import java.util.List;
import java.util.function.Supplier;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class MeshRenderer extends ApplicationAdapter
{
    // --- Vertex Data ---
    // Positions: (x, y)
    // The vertices array format is: X, Y, R, G, B, A
    // (This format is defined by the VertexAttributes below)
    private FirstPersonCameraController cameraController;
    private ShaderProgram shaderProgram;

    private static final String VERTEX_SHADER =
        "attribute vec4 a_position; " +
        "attribute vec4 a_color; " +
        "uniform mat4 u_projTrans; " +
        "varying vec4 v_color; " +
        "void main() { " +
        "   v_color = a_color;" +
        "   gl_Position = u_projTrans * a_position; " +
        "}";
    private static final String FRAGMENT_SHADER =
        "varying vec4 v_color; " +
        "void main() { " +
        "   gl_FragColor = v_color; " +
        "}";

    private PerspectiveCamera camera;
    private List<Body> bodies;
    private final Supplier<List<Body>> bodySupplier;

    public MeshRenderer(Supplier<List<Body>> bodySupplier)
    {
        this.bodySupplier = bodySupplier;
    }

    @Override
    public void create()
    {
        this.bodies = bodySupplier.get();
        shaderProgram = new ShaderProgram( VERTEX_SHADER, FRAGMENT_SHADER );
        if ( !shaderProgram.isCompiled() )
        {
            Gdx.app.error( "Shader Error", shaderProgram.getLog() );
            throw new IllegalStateException( "Shader failed to compile." );
        }
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(100f, 100f, 100f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f; // Closer near plane for better viewing of nearby objects
        camera.far = 500f;
        camera.update();

        cameraController = new FirstPersonCameraController(camera);
        // This line tells LibGDX to send all input events (keys, mouse, etc.) to our controller
        Gdx.input.setInputProcessor(cameraController);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void render()
    {
// **1. Update the Camera Controller**
        // This is where the WASD key presses are read and the camera's position is changed.
        cameraController.update(Gdx.graphics.getDeltaTime());

        // 2. Standard Render setup
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);

//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        // 3. Render the Mesh
        shaderProgram.bind();
        // The camera's combined matrix MUST be updated after the controller moves the camera.
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        for ( final Body body : bodies )
        {
            final Mesh mesh = body.getMesh();
            mesh.render( shaderProgram, GL20.GL_TRIANGLES);
        }
    }

    @Override
    public void dispose()
    {
        bodies.forEach( body -> body.getMesh().dispose() );
        shaderProgram.dispose();
    }

}