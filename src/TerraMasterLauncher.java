public class TerraMasterLauncher {

    public static void main(String[] args) {
        JarClassLoader jcl = new JarClassLoader();
        try {
            jcl.invokeMain("TerraMaster", args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    } // main()

} // class TerraMasterLauncher
