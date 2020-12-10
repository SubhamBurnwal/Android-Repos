 package subham.simpleapp;

import android.graphics.Point;
import android.support.annotation.NonNull;

 enum MAPTYPE {WALLED, WEAK_WALLED, NO_WALL, ENDLESS}
 enum MODE {PUZZLE, ZEN, EDITOR}
 enum FORM {BIRDIE,SIDEWAYS,NONE}
 enum DIR {UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, LEFT_UP, LEFT_DOWN, RIGHT_UP, RIGHT_DOWN}
 enum STATE_CHANGE_STYLE {BUILDING, DIRECTIONAL, STRICT, RANDOM, NONE}

 //Entity wall, weak_wall, gate, warp_wall, temple, floor, lava, water;      //perma, first layer
 //Entity gear, laser, portal, spring, box;                                  //bound, second layer
 //Entity speedUp, tresPass, freezeTime, freeHint, skipLvl, lifeUp, lifeDown;//tempo, second layer
 //Entity ball, power_up, mecha, path;                                       //items, third layer
 //Entity wind, water, wave, fog, bubble, sparks, sparkle, arrow;            //sfx,   last layer

 enum PERMATYPE {GRASS, FLOOR, LAVA, WATER}
 enum BUILDTYPE {WALL, WEAK_WALL, GATE, WARP_WALL, TEMPLE, NONE}
 enum MECHATYPE {GEAR, LASER, PORTAL, SPRING, BOX, NONE}
 enum ITEMTYPE {HERO, POWER_UP, MECHA, PATH, NONE}
 enum POWERTYPE {SPEED_UP, TRESPASS, FREEZE_TIME, HINT, SKIP_LEVEL, LIFE_UP, LIFE_DOWN, NONE}
 enum PARTICLETYPE {WIND, WATER, WAVE, FIRE, FOG, BUBBLE, SPARKS, SPARKLE, ARROW, NONE}

/*smallest part of a map are _CELL and cell
    _CELL   : to hold all processing functions, default values and required constants
              limited to use within map generators
    cell    : to hold, store, and use the generated level data
              level are initialized during game-play, then TODO: loaded from a path OR generated using pathFinder
              pathFinder finds a solution first, based on the MAPTYPE, then builds rest of the map around it
              solution is a linked list of _CELLS, TODO: or a hanging tree of _CELLS
              solution is embedded in map, and stored in level, along with TODO: a minimized form of solution

              TODO: update cell and _CELL for handling complex preset animations

    $ There are runtime fatal errors with keywords differing in just letter case, so the underscore
*/

class _CELL {
    _CELL(){
        _type = PERMATYPE.FLOOR;
        _form = 1;
        _state = 1;
    }
    _CELL(PERMATYPE cellType, int currentForm, int currentState){
        _type = cellType;
        _form = currentForm;
        _state = currentState;
    }
    _CELL(PERMATYPE cellType, int currentForm, int currentState, BUILDTYPE buildType) {
        _type = cellType;
        _form = currentForm;
        _hasBuild = buildType;
        _state = currentState;
    }
    _CELL(PERMATYPE cellType, int currentForm, int currentState, ITEMTYPE itemType){
        _type = cellType;
        _form = currentForm;
        _hasExtra = itemType;
        _state = currentState;
    }
    _CELL(PERMATYPE cellType, int currentForm, int currentState, POWERTYPE powerType){
        _type = cellType;
        _form = currentForm;
        _hasExtra = ITEMTYPE.POWER_UP;
        _hasPower = powerType;
        _state = currentState;
    }
    public int state(){return _state;}
    public int form(){return _form;}
    //public int state(){return _state[_form];}
    public PERMATYPE type(){return _type;}
    public BUILDTYPE getBuildType(){if(_hasExtra == null) _hasBuild = BUILDTYPE.NONE; return _hasBuild;}
    public ITEMTYPE getItemType(){if(_hasExtra == null) _hasExtra = ITEMTYPE.NONE; return _hasExtra;}
    public POWERTYPE getPowerType(){if(_hasPower == null) _hasPower = POWERTYPE.NONE; return _hasPower;}
    public MECHATYPE getMechaType(){if(_hasMecha == null) _hasMecha = MECHATYPE.NONE; return _hasMecha;}

    public void setType(PERMATYPE type){_type = type;}
    public void setState(int state){_state = state;}
    public void setForm(int form){_form = form;}
    public void setBuildType(BUILDTYPE buildType){_hasBuild = buildType;}
    public void setItemType(ITEMTYPE itemType){_hasExtra = itemType;}
    public void setPowerType(POWERTYPE powerType){ _hasPower = powerType;}
    public void setMechaType(MECHATYPE mechaType){_hasMecha = mechaType;}
    private PERMATYPE _type;
    private int _form;
    private int _state;
    private BUILDTYPE _hasBuild = BUILDTYPE.NONE;
    private ITEMTYPE _hasExtra = ITEMTYPE.NONE;
    private POWERTYPE _hasPower = POWERTYPE.NONE;
    private MECHATYPE _hasMecha = MECHATYPE.NONE;
}
class cell {
    public int index;
    public int form;
    public int state;
    //public int state[];
    public PERMATYPE type;
    public cell(PERMATYPE cellType) {
        this.type = cellType;
    }
    public BUILDTYPE hasBuild;
    public ITEMTYPE hasExtra;
    public POWERTYPE hasPower;
    public MECHATYPE hasMecha;
    public cell nextCell;    //stores pointers to the next cell in the solution
    public DIR nextDir;
}

class player{
    public player(){
    }
    public player(@NonNull player Player){
        this.curLevel = Player.curLevel;
        this.highscore = Player.highscore;
        this.score = Player.highscore;
    }
    public void reset(){
        this.curLevel = null;
        this.highscore = 0;
        this.score = 0;
    }
    public int score;
    public level curLevel;
    public int highscore;
}

class level {
    public level(Point mapGridSize, Point defaultCellSize, MAPTYPE mapType) {
        MAXCOL = mapGridSize.x;
        MAXROW = mapGridSize.y;
        MAX_CELL = mapGridSize.x*mapGridSize.y;
        this.defaultCellSize = new Point(defaultCellSize);
        this.cellSize = new Point(defaultCellSize);
        _mapType = mapType;
        _map = new _CELL[MAX_CELL];
        for (int i = 0; i < MAX_CELL; i++) {
            _map[i] = new _CELL();
        }
        pathfinder = new pathFinder();
        this.generate();
    }
    public void changeZoom(double xZoom){
        cellSize.x*=xZoom;
        cellSize.y*=xZoom;
    }
    public void resetZoom(){
        cellSize.set(defaultCellSize.x,defaultCellSize.y);
    }
    public Point getCellSize(){
        return cellSize;
    }
    public Point getDefaultCellSize() {
        return defaultCellSize;
    }

    //map stuff
    public int rows(){return MAXROW;}                             //returns map dimensions
    public int cols(){return MAXCOL;}
    final private Point defaultCellSize;                          //typical cell size at zoom level 1
    private Point cellSize;                                       //modifiable typical cell size
    public PERMATYPE get(int row, int col) {
        return _map[row * MAXCOL + col].type();
    }                   //returns cells on the map
    public PERMATYPE get(int index){return _map[index].type();}    //using index
    private void generate() {
        int row, col;

        //deciding default BORDER using mapType
        BUILDTYPE borderType;
        switch (_mapType) {
            case WEAK_WALLED:
                borderType = BUILDTYPE.WEAK_WALL;
            case NO_WALL:
                borderType = BUILDTYPE.NONE;
            case ENDLESS:
                borderType = BUILDTYPE.WARP_WALL;
            case WALLED:
            default:
                borderType = BUILDTYPE.WALL;
        }

        //placing BORDERs
        for (int y = 0; y < MAXROW; y++) {
            for (int x = 0; x < MAXCOL; x++) {
                int temp = y*MAXCOL+x;
                _map[temp].setType(PERMATYPE.GRASS);
                _map[temp].setBuildType(borderType);
                //corner walls
                if(x+y == 0) _map[temp].setState(8);                    //top-left
                else if(x+y == MAXCOL+MAXROW-2) _map[temp].setState(4); //bottom-right
                else if(x*y == 0) {
                    if (y == MAXROW - 1) _map[temp].setState(6);        //bottom-left
                    else if (x == MAXCOL - 1) _map[temp].setState(2);   //top-right
                }

                if(x>0 && x<MAXCOL-1){
                    if(y==0) _map[temp].setState(1);                    //top
                    if(y==MAXROW-1) _map[temp].setState(5);             //bottom
                }
                else if(y>0 && y<MAXROW-1){
                    if(x==0) _map[temp].setState(7);                    //left
                    if(x==MAXCOL-1) _map[temp].setState(3);             //right
                }
                if (y > 0 && y < MAXROW - 1)
                    if (x == 0 && x < MAXCOL - 1)
                        x = MAXCOL - 2;
            }
        }

        //initialize blank solution having ENTRY and EXIT
        pathfinder.init(MAXROW, MAXCOL, _map);

        //generate a solution using pathFinder
        if (pathfinder.generate()) {
            solution = pathfinder.fetch();
            //imbed solution in map
            cell icell = solution;
            _entry = new Point(icell.index%MAXCOL,icell.index/MAXCOL);
            for (; icell.nextCell != null; icell = icell.nextCell) {
                _map[icell.index].setType(icell.type);
                _map[icell.index].setItemType(icell.hasExtra);
                if(icell.hasExtra == ITEMTYPE.POWER_UP)
                    _map[icell.index].setPowerType(icell.hasPower);
                _map[icell.index].setState(icell.state);
            }
            _exit = new Point(icell.index%MAXCOL,icell.index/MAXCOL);
        }

        //NOTES:
        //we could create a function which compares whether two different paths use obstacles that cancel out the other
        //we isolate and try to move obstacles so that both paths can exist simultaneously
        //we implement a path::add function to add both paths into one path that we display on the map
        //the same process can be repeated for adding more and more solutions to a map
        //for the hints, we find which path the user might be using and that being simple we display that solution
        //or a small part of it
    }                  //to generate the map
    private void decorate(){
        int row, col;
        //decorating BORDERs
        for (int y = 0; y < MAXROW; y++) {
            for (int x = 0; x < MAXCOL; x++) {
                int k = y*MAXCOL+x;
                switch(_map[k].type()){
                    case WATER: case LAVA:
                    for(int i=0; i<Entity.DIRECTION.values().length-1;i++) {
                        switch(getInDirection(new Point(y, x), Entity.DIRECTION.values()[i]).type()) {
                        case FLOOR: case GRASS:
                            _map[k].setForm(FORM.BIRDIE.ordinal());
                            _map[k].setState(Entity.DIRECTION.values()[i].ordinal());
                           break;
                        }
                    }
                    break;
                }
            }
        }
    }
    public _CELL _map[];                                          //the generated map
    private int MAXROW, MAXCOL, MAX_CELL;                         //quantized dimensions
    public Point getMapSize(){
        return new Point(this.MAXCOL*cellSize.x,this.MAXROW*cellSize.y);
    }                                                             //return dimensions
    private MAPTYPE _mapType;
    //solution of the level
    private pathFinder pathfinder;                                //utility for generator
    private cell solution;                                        //solved path
    private Point _entry, _exit;
    public Point getStart(){
        return _entry;
    }
    public Point getExit(){
        return _exit;
    }
    public _CELL getInDirection(Point p, Entity.DIRECTION direction){
        switch(direction) {
            case U: return _map[(p.y-1) * MAXCOL + p.x];
            case D: return _map[(p.y+1) * MAXCOL + p.x];
            case L: return _map[p.y * MAXCOL + (p.x-1)];
            case R: return _map[p.y * MAXCOL + (p.x+1)];
            case UL: return _map[(p.y-1) * MAXCOL + (p.x-1)];
            case UR: return _map[(p.y-1) * MAXCOL + (p.x+1)];
            case DL: return _map[(p.y+1) * MAXCOL + (p.x-1)];
            case DR: return _map[(p.y+1) * MAXCOL + (p.x+1)];
        }
        return null;
    }
}
class pathFinder {
    public pathFinder() {
        _path = null;
    }
    public pathFinder(int maxRow, int maxCol, _CELL map[]) {
        init(maxRow, maxCol, map);
    }
    public void init(int maxRow, int maxCol, _CELL map[]) {
        MAXROW = maxRow;
        MAXCOL = maxCol;
        _map = map;
        //initializing random number generator
        //clear old solution data
        if (_path != null) _path = null;

        //deciding ENTRY and EXIT
        //_ENTRY = (int) (random() % MAXROW);
        //_map[_ENTRY * MAXCOL] = PERMATYPE.ENTRY;
        //_EXIT = (int) (random() % MAXROW);
        //_map[_EXIT * MAXCOL + MAXCOL] = PERMATYPE.EXIT;

        //start solution from ENTRY point
        _path = new cell(PERMATYPE.FLOOR);
        _path.state = 1;
        _path.hasBuild = BUILDTYPE.GATE;
        //_path.index = _ENTRY;
    }
    public boolean generate() {
        if (_path == null) return false;
        int ID = 0;
        cell currentCell = _path;
        currentCell.index = MAXCOL*MAXROW/2;

        for(int i=0; i<3; i++) {
            cell newCell = null;
            if(i==0) {
                //set info of new cell
                newCell = new cell(PERMATYPE.GRASS);
                newCell.state = 1;
                newCell.index = MAXCOL*MAXROW/2;
                newCell.hasBuild = BUILDTYPE.GATE;
                currentCell.nextDir = DIR.RIGHT;
            } else if(i==1) {
                newCell = new cell(PERMATYPE.FLOOR);
                newCell.hasExtra = ITEMTYPE.MECHA;
                newCell.hasMecha = MECHATYPE.BOX;
                newCell.index = MAXCOL*MAXROW/2 + MAXCOL/2;
                currentCell.nextDir = DIR.LEFT_DOWN;
            } else if(i==2){
                newCell = new cell(PERMATYPE.FLOOR);
                newCell.state = 3;
                _EXIT = newCell.index = MAXCOL*(MAXROW/2+1)-1;
                newCell.hasBuild = BUILDTYPE.GATE;
                newCell.hasExtra = ITEMTYPE.POWER_UP;
                newCell.hasPower = POWERTYPE.SPEED_UP;
                currentCell.nextDir = DIR.LEFT;
            } /*else if(i==3) {
                newCell = new cell(PERMATYPE.GRASS);
                newCell.index = 55;
                newCell.hasBuild = BUILDTYPE.WALL;
                currentCell.nextDir = DIR.RIGHT;
            }*/
            // set target of new cell
            currentCell.nextCell = newCell;
            currentCell = currentCell.nextCell;
        }
        currentCell.nextCell = null;
        currentCell.nextDir = null;
        //do {
        //    currentCell = makeCell(PERMATYPE.WALL, ID);
        //    ID++;
        //} while (!leadsToType(currentCell, EXIT));
        return true;
    }
    public cell fetch() {
        return _path;
    }
    private cell _path;                  //our final solution
    private _CELL[] _map;                //the map requiring solution
    private int MAXROW, MAXCOL, _ENTRY, _EXIT;  //doesnt need entry and exit now, now that we have a path
}
public class elements {
//TODO eliminate pseudo class
}