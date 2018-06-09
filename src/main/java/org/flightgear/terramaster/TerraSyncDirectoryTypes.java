package org.flightgear.terramaster;
import java.io.File;

/**
 * The subdirectories that TerraMaster supports in the directory. 
 * @author keith.paterson
 *
 */

public enum TerraSyncDirectoryTypes {

  TERRAIN("Terrain", 0), OBJECTS("Objects", 1), MODELS("Models",
      2), AIRPORTS("Airports", 3), BUILDINGS("Buildings", 4);

  String dirname = null;
  
  TerraSyncDirectoryTypes(String name, int index) {
    this.dirname = name + File.separator;
  }

}
