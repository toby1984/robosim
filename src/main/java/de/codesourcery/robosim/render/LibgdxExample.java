package de.codesourcery.robosim.render;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class LibgdxExample extends ApplicationAdapter
{
    private com.badlogic.gdx.graphics.Mesh triangleMesh;
    private ShaderProgram shaderProgram;

    // --- Vertex Data ---
    // Positions: (x, y)
    // The vertices array format is: X, Y, R, G, B, A
    // (This format is defined by the VertexAttributes below)
    private final float packedRed = new Color(1.0f, 0.0f, 0.0f, 1.0f).toFloatBits();

    private final float[] vertices = new float[]{
        // Position         // Color (Red)
        -0.5f, -0.5f, packedRed, // Bottom-left
        0.5f, -0.5f, packedRed, // Bottom-right
        0.0f, 0.5f, packedRed // Top-center
    };

    // LibGDX uses its own naming convention for attributes
    private static final String VERTEX_SHADER =
        """
            attribute vec4 a_position;
            attribute vec4 a_color;
            varying vec4 v_color;
            void main() {
                v_color = a_color;
                gl_Position = a_position;
            }""";

    private static final String FRAGMENT_SHADER =
        """
            varying vec4 v_color;
            void main() {
                gl_FragColor = v_color;
            }""";

    @Override
    public void create()
    {
        // 1. Create the Shader Program
        shaderProgram = new ShaderProgram( VERTEX_SHADER, FRAGMENT_SHADER );
        if ( !shaderProgram.isCompiled() )
        {
            Gdx.app.error( "Shader Error", shaderProgram.getLog() );
            throw new IllegalStateException( "Shaders failed to compile." );
        }

        // 2. Create the Mesh (VAO/VBO combined, handled by LibGDX)
        // Defines the structure of the vertex data in the array:
        // 2 floats for Position, 4 floats for Color.
        VertexAttributes attributes = new VertexAttributes(
            new VertexAttribute( VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE ),
            VertexAttribute.ColorPacked()
        );

        triangleMesh = new com.badlogic.gdx.graphics.Mesh(
            true,               // Static (data won't change often)
            3,                  // Max number of vertices
            0,                  // Max number of indices (not using indices here)
            attributes
        );

        // Upload the vertex data to the GPU buffer
        triangleMesh.setVertices( vertices );
    }

    @Override
    public void render()
    {
        // Clear the screen to a dark grey color
        Gdx.gl.glClearColor( 0.2f, 0.2f, 0.2f, 1.0f );
        Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

        // 1. Tell OpenGL to use our custom shader program
        shaderProgram.begin();

        // 2. Render the mesh
        // Argument 1: Primitive type (GL_TRIANGLES is 4)
        // Argument 2: Start vertex index
        // Argument 3: Number of vertices to draw
        triangleMesh.render( shaderProgram, GL20.GL_TRIANGLES, 0, 3 );

        // 3. Stop using the shader program
        shaderProgram.end();
    }

    @Override
    public void dispose()
    {
        // Release resources to prevent memory leaks
        triangleMesh.dispose();
        shaderProgram.dispose();
    }
}
