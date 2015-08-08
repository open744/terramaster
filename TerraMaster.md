## Installing ##

Grab the latest zip file from the 'Downloads' link.

Unzip all files into a folder.

## Running ##

You need to have Java 6 installed on your system.

Start TerraMaster using a shortcut that uses the commandline:
> java -jar terramaster.jar

## Using ##

TerraMaster starts off in 'whole-earth' view. Left-click anywhere to zoom in on that location in 'globe' view. Click the 'globe' icon to toggle between these two views.

Click the 'gears' icon to set the folder where your downloaded (terrasync'd) scenery is found.

Tile colour codes:
> | green | both Terrain and Objects data exist |
|:------|:------------------------------------|
> | amber | only Terrain data exist             |
> | red   | current tile selection              |
> | blue  | tiles queued for sync               |

Navigate around the globe using the right mouse button.

Zoom using the scroll wheel.

Select 1x1 degree tiles using the left button. Hold down the Control key to add to your current selection. Use click-and-drag (or Control-click-and-drag) to select a matrix of tiles.

Click 'Sync' to start scenery download from the server. If you already have a scenery tile, it will be updated with the latest revision. The tiles are sync'd in the order you selected them.

(Hint: For a matrix, the tiles are sync'd in order of increasing distance from the first-clicked tile. Eg if you click then drag north-east, the south-west-most tile is sync'd first, then the three adjacent tiles, and so on, until the north-east-most tile. When sync'ing scenery prior to a long flight, click on the origin airport then drag to the destination.)

## Discuss ##

Come to the [FG forum thread](http://flightgear.org/forums/viewtopic.php?f=5&t=12050) for help.