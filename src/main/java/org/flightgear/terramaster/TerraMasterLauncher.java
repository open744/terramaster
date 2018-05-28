package org.flightgear.terramaster;
public class TerraMasterLauncher {

    public static void main(String[] args) {
        JarClassLoader jcl = new JarClassLoader();
        try {
            jcl.invokeMain("org.flightgear.terramaster.TerraMaster", args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    } // main()

} // class TerraMasterLauncher
