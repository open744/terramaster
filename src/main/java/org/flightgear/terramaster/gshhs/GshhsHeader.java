package org.flightgear.terramaster.gshhs;

public class GshhsHeader {
  int		id;
  private int numPoints;
  private byte		level;
  private boolean	greenwich;
  private int		west;
  private int east;
  private int south;
  private int north;
  int area;
  int areaFull;
  int container;
  int ancestor;
  public int getWest() {
    return west;
  }
  public void setWest(int west) {
    this.west = west;
  }
  public int getNorth() {
    return north;
  }
  public void setNorth(int north) {
    this.north = north;
  }
  public int getSouth() {
    return south;
  }
  public void setSouth(int south) {
    this.south = south;
  }
  public int getEast() {
    return east;
  }
  public void setEast(int east) {
    this.east = east;
  }
  public int getNumPoints() {
    return numPoints;
  }
  public void setNumPoints(int n) {
    this.numPoints = n;
  }
  public boolean isGreenwich() {
    return greenwich;
  }
  public void setGreenwich(boolean greenwich) {
    this.greenwich = greenwich;
  }
  public byte getLevel() {
    return level;
  }
  public void setLevel(byte level) {
    this.level = level;
  }
}
