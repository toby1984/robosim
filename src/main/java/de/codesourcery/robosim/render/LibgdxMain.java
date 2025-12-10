package de.codesourcery.robosim.render;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class LibgdxMain {
    public static void main (String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setForegroundFPS(60);
        config.setTitle("LibGDX Red Triangle");
        config.setWindowedMode(800, 600);

        new Lwjgl3Application(new LibgdxExample(), config);
    }
}
