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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class MeshRenderer extends ApplicationAdapter
{
    // Light properties
    private static final Vector3 lightPosition = new Vector3(0f, 100f, 100f);
    private static final Vector3 lightColor = new Vector3(1f, 1f, 1f);
    private static final Vector3 ambientColor = new Vector3(0.1f, 0.1f, 0.1f);

    private FirstPersonCameraController cameraController;
    private ShaderProgram shader;

    // <-- NEW: Normal vector input
    // <-- NEW: World matrix (for normal transform)
    // <-- NEW: Light position
    // <-- No 'flat' keyword, allow interpolation (Gouraud)
    // Transform position and normal into world space
    // Calculate light vector (from vertex to light)
    // Calculate the diffuse component (Lambertian model)
    // Combine ambient and diffuse lighting
    // Set vertex color (Base Red color * calculated light)
    // Final position projection
    private static final String VERTEX_SHADER =
        """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec3 a_normal;
            
            uniform mat4 u_projTrans;
            uniform mat4 u_meshTrans;
            uniform mat4 u_worldTrans;
            uniform vec3 u_lightPosition;
            uniform vec3 u_lightColor;
            uniform vec3 u_ambientColor;
            varying vec4 v_color;
            void main() {
              vec3 positionWorld = (u_worldTrans * u_meshTrans * a_position).xyz;
              vec3 normalWorld = normalize((u_worldTrans * u_meshTrans * vec4(a_normal, 0.0)).xyz);
              vec3 lightVector = normalize(u_lightPosition - positionWorld);
              float diffuseIntensity = max(dot(normalWorld, lightVector), 0.0);
              vec3 finalLight = u_ambientColor + u_lightColor * diffuseIntensity;
              v_color = a_color * vec4(finalLight, 1.0);
              gl_Position = u_projTrans * u_meshTrans * a_position;
            }""";

    private static final String FRAGMENT_SHADER =
        """
            varying vec4 v_color;
            void main() {
              gl_FragColor = v_color;
            }""";

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
        shader = new ShaderProgram( VERTEX_SHADER, FRAGMENT_SHADER );
        if ( !shader.isCompiled() )
        {
            Gdx.app.error( "Shader Error", shader.getLog() );
            throw new IllegalStateException( "Shader failed to compile." );
        }
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(100f, 100f, 100f);
        camera.lookAt(0f, 0f, 0f);
        camera.near = 0.1f; // Closer near plane for better viewing of nearby objects
        camera.far = 500f;
        camera.update();

        cameraController = new FirstPersonCameraController(camera);
        cameraController.setVelocity( 50 );
        cameraController.setDegreesPerPixel( 1 );

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
//        bodies.getFirst().incAngleZ( 1 );

        cameraController.update(Gdx.graphics.getDeltaTime());

        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        shader.bind();
        shader.setUniformMatrix("u_projTrans", camera.combined);
        shader.setUniformMatrix("u_worldTrans", camera.view);
        shader.setUniformf("u_lightPosition", lightPosition);
        shader.setUniformf("u_lightColor", lightColor);
        shader.setUniformf("u_ambientColor", ambientColor);
        for ( final Body body : bodies )
        {
            shader.setUniformMatrix("u_meshTrans", body.getAbsoluteMatrix());
            body.getMesh().render( shader, GL20.GL_TRIANGLES);
        }
    }

    @Override
    public void dispose()
    {
        bodies.forEach( body -> body.getMesh().dispose() );
        shader.dispose();
    }

}