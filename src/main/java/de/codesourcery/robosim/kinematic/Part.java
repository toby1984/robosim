package de.codesourcery.robosim.kinematic;

import java.util.ArrayList;
import java.util.List;
import de.codesourcery.robosim.render.Body;

public sealed interface Part permits Joint, Link
{
    Part next();
    void setNext(Part part);

    Part previous();
    void setPrevious(Part part);

    Body body();
    void setBody(Body body);
    String name();

    default List<Body> getAllBodies() {
        final List<Body> result = new ArrayList<>();
        Part current = this;
        do {
            final Body b = body();
            if ( b != null )
            {
                b.visit( result::add );
            }
            current = current.next();
        } while ( current != null );

        return result;
    }
}
