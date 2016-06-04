CLASSPATH=-classpath LIB/svnkit-1.8.10.jar:LIB/javaproj-1.0.6.jar::.

all: TerraMasterLauncher.class TerraMaster.class MapPoly.class MapFrame.class GshhsHeader.class TileData.class Svn.class TileName.class FGMap.class Airport.class WebWorker.class
	jar cfm terramaster.jar manifest *.class gshhs_l.b wdb_borders_l.b

TerraMasterLauncher.class: TerraMasterLauncher.java
	javac ${CLASSPATH} TerraMasterLauncher.java

TerraMaster.class: TerraMaster.java
	javac ${CLASSPATH} TerraMaster.java

GshhsHeader.class: GshhsHeader.java
	javac ${CLASSPATH} GshhsHeader.java

MapPoly.class: MapPoly.java
	javac ${CLASSPATH} MapPoly.java

MapFrame.class: MapFrame.java
	javac ${CLASSPATH} MapFrame.java

TileData.class: TileData.java
	javac ${CLASSPATH} TileData.java

TileName.class: TileName.java
	javac ${CLASSPATH} TileName.java

Svn.class: Svn.java
	javac ${CLASSPATH} Svn.java

FGMap.class: FGMap.java
	javac ${CLASSPATH} FGMap.java

Airport.class: Airport.java
	javac ${CLASSPATH} Airport.java

WebWorker.class: WebWorker.java
	javac ${CLASSPATH} WebWorker.java
