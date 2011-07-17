CLASSPATH=-classpath javaproj-1.0.6.jar:svnkit.jar:.

all: TerraMaster.class MapPoly.class MapFrame.class GshhsHeader.class TileData.class Svn.class TileName.class
	jar cfm terramaster.jar manifest *.class

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
