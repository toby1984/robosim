package de.codesourcery.robosim.kinematic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.badlogic.gdx.math.Vector3;
import de.codesourcery.robosim.render.Body;

public sealed interface Part permits Joint, Link
{
    Part next();
    void setNext(Part part);

    Part previous();
    void setPrevious(Part part);

    default void visitChain(BiConsumer<Part,Part> visitor) {
        Part previous = previous();
        Part current = this;
        do
        {
            visitor.accept( previous, current );
            previous = current;
            current = current.next();
        } while ( current != null );
    }

    Body body();
    void setBody(Body body);
    String name();

    default List<Body> getAllBodies() {
        final List<Body> result = new ArrayList<>();
        final Body b = body();
        if ( b != null )
        {
            b.visit( result::add );
        }
        return result;
    }
}
