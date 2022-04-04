package subham.dungeondash;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.text.DecimalFormat;
import java.util.Vector;

import static subham.dungeondash.gmtry.getCircleBisector;
import static subham.dungeondash.gmtry.wrtCircle;

public class GameView extends SurfaceView implements Runnable {

    //menu control variables
    private boolean selected;
    volatile boolean loaded;
    private boolean bitMapped;

    //game control variables
    private boolean leftDragHeld, rightDragHeld;        //drag controls held or not
    private Point lastLeftDragHeld, lastRightDragHeld;  //last held positions in drag controls
    private Point leftDragDisp, rightDragDisp;          //rate of change in held positions in drag controls

    //menu props
    private Point btnSize;                              //current btn position, original btn position, original btn size
    private Point lastHeld;

    private String menuName[];
    private int fontSize[], fontColor[];
    private int menuBgColor = Color.WHITE;
    private int menuColorLeft = Color.BLACK;
    private int menuColorRight = Color.BLACK;

    //our game-play elements as objects,         TODO: loading array of objects from file instead of individual code clutter
    private Vector<Entity> perma, build, tempo, bound, items, sfx;
    private Entity hero;
    private myLine myPath;
    private level lvl;
    private int curFrame, curTotalFrames, curCheckPoint, nextCheckPoint;
    private boolean curFrameChanged = false;
    //debug props
    private int frameBgColor = Color.WHITE, o2pColor = Color.RED, a2bColor = Color.GRAY, p2tColor = Color.YELLOW, heroBoundColor = Color.GREEN;
    private int dragColor = 0xaabbddee;
    private Point randCoord;

    //Class constructor
    public GameView(Context context, Point screenResolution) {
        super(context);
        this.context = context;

        //initialising tools
        surfaceHolder = getHolder();
        paint = new Paint();
        pointPaint = new Paint();
        MAX = new Point(screenResolution);

        //initializing bounds
        lowerViewBound = new Point (0,0);                                           //initial lower boundary
        upperViewBound = new Point(lowerViewBound.x+MAX.x, lowerViewBound.y+MAX.y); //initial upper boundary
        curLowerViewBound = new Point(lowerViewBound);
        curUpperViewBound = new Point(upperViewBound);

        defCellSize = new Point(40,40); //Best size per square cell
        fontSize = getResources().getIntArray(R.array.font_size);
        fontColor= getResources().getIntArray(R.array.font_color);

        //menuName = getResources().getStringArray(R.array.center_menu);
        btnSize = new Point(MAX.x, MAX.y/14);
        paint.setTextSize(fontSize[2]);
        lastHeld = new Point(-1000, -1000);

        //game control variables
        running = true;
        debugging = true;
        gameRequested = false;
        loaded = false;
        playing = false;
        bitMapped = false;

        //initializing game controllers,                     TODO: extend Entity for non-bitmap stuff, like controls
        leftDragRadius = new Point(MAX.x/13,MAX.x/13);
        rightDragRadius = new Point(leftDragRadius);
        noControllerZone = new Point(3*MAX.x/7,4*MAX.x/7);

        minLeftDragOrigin = new Point(leftDragRadius.x,leftDragRadius.y);
        maxLeftDragOrigin = new Point(noControllerZone.x-leftDragRadius.x,MAX.y-leftDragRadius.y);
        minRightDragOrigin = new Point(noControllerZone.y+rightDragRadius.x, rightDragRadius.y);
        maxRightDragOrigin = new Point(MAX.x-rightDragRadius.x,MAX.y-rightDragRadius.y);
        
        leftDragOrigin = new Point(minLeftDragOrigin.x,maxLeftDragOrigin.y);
        rightDragOrigin = new Point(maxRightDragOrigin.x,maxRightDragOrigin.y);
        leftDragHeld = false; rightDragHeld = false;
        leftDragDisp = new Point(0,0); rightDragDisp = new Point(0,0);
        lastLeftDragHeld = new Point(leftDragOrigin); lastRightDragHeld = new Point(rightDragOrigin);

        randCoord = new Point(-1,-1);

        //menu map dimensions
        menuMaxViewBound = new Point(3*MAX.x, MAX.y);
    }

    @Override
    public void run() {
        Log.d(TAG, "Starting app loop");
        initTimingElements();
        long startTime, timeDiff, framesSkipped;
        //physics phys = new physics();
        //boolean isInLineOfGates = false;

        while (running) {
            startTime = System.currentTimeMillis();
            framesSkipped = 0;
            if(gameRequested) {
                while (!loaded) {
                    Log.d(TAG, "Loading level. MaxDims: "+MAX.toString());
                    //136 30
                    buildLevel(new Point(60, 30), new Point(MAX.x,MAX.y), MAPTYPE.WALLED, 2);
                }
            }
            if(loaded && gameRequested) {
                Log.d(TAG, "Starting game loop");
                playing = true;
                gameRequested = false;
            }

            if (playing) {
                //TODO: transfer black-n-white snippets to its own project
                //if (curFrameChanged) {
                    //frame color changes along black-white gradient
//                    frameBgColor += 0x00999999 / (nextCheckPoint - curFrame + 1);
//                    curFrameChanged = false;
//                }
//                curUpperViewBound.x = curLowerViewBound.x + MAX.x;
            }   //update menu background
            draw();              //draw updated frame
            if(playing) update();//update game entities
            else updateMenus();  //check last swipes, and make method calls to update menus

            timeDiff = System.currentTimeMillis() - startTime;
            sleepTime = FRAME_PERIOD - timeDiff;
            if(sleepTime > 0) control(sleepTime);   //use the remaining time to sleep
            while(sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {// && framesSkipped < MAX_FRAME_SKIPS){
                //we need to catch up
                if(framesSkipped%2==0) {
                    if (playing) update();    //update without rendering
                    else updateMenus();
                }
                sleepTime += FRAME_PERIOD;
                framesSkipped++;
            }
            if(framesSkipped > 0) Log.d(TAG, "Skipped:" + framesSkipped);
            framesSkippedPerStatCycle += framesSkipped; //for statistics
            storeStats();                               //call the routine to store the gathered stats
        }
    }
    private void draw() {
        //INITIALIZING DRAWING VARIABLES,     TODO: Blitt some bitmaps to fasten up the job, EDIT: Done, now improve it
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();
            if (playing) {
                drawGame();                         //DRAWING GAME LEVEL
                drawGameControls();
            }
            else {
                drawMenu();
            }
            if(debugging) {
                drawCursorPos();
                drawStats();
            }
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
    private void drawMap(int i) {
        int curPosX, curPosY, index;
        _CELL temp;

        curPosY = 0;
        for (int y = 0; y < lvl.rows(); y++) {
            curPosX = 0;
            for (int x = 0; x < lvl.cols(); x++) {
                temp = lvl._map[y*lvl.cols()+x];
                switch(i){
                    case 0:
                        index = temp.type().ordinal();
                        if(index< PERMATYPE.values().length) {
                            //bCanvas.drawBitmap(fetchEntity("perma", index).bitmap(temp.state()), curPosX, curPosY , paint);
                            try {
                                bCanvas.drawBitmap(fetchEntity("perma", index).bitmap(), curPosX, curPosY, paint);
                            } catch (Exception e){
                                e.fillInStackTrace();
                            }
                        }
                        break;
                    case 1:
                        index = temp.getBuildType().ordinal();
                        if(temp.getBuildType() != BUILDTYPE.NONE)
                            bCanvas.drawBitmap(build.get(index).bitmap(temp.state()), curPosX, curPosY, paint);
                        break;
                    case 2:
                        index = temp.getMechaType().ordinal();
                        if(temp.getMechaType() != MECHATYPE.NONE)
                            bCanvas.drawBitmap(bound.get(index).bitmap(bound.get(index).hasCurrentState()), curPosX, curPosY , paint);
                        break;
                    case 3:
                        index = temp.getPowerType().ordinal();
                        if(temp.getPowerType() != POWERTYPE.NONE)
                            bCanvas.drawBitmap(tempo.get(index).bitmap(tempo.get(index).hasCurrentState()), curPosX, curPosY , paint);
                        break;
                    case 4:
                        index = temp.getItemType().ordinal();
                        /*we need to summon subtypes randomly in their place, so initially they are mystery boxes,      TODO: Instead of all values in ITEMTYPE having subtypes,*/
                        if(temp.getItemType() == ITEMTYPE.PATH)
                            bCanvas.drawBitmap(items.get(index).bitmap(items.get(index).hasCurrentState()), curPosX, curPosY , paint);
                        //right now all similar objects change states simultaneously,                                                            TODO: need to store more states or bring some randomness, EDIT: Done, now the wildcards
                        break;
                }
                curPosX += lvl.getCellSize().x;
            }
            curPosY += lvl.getCellSize().y;
        }
    }
    private void drawGame(){
        Point gap = new Point(curLowerViewBound);
        canvas.drawColor(frameBgColor);
        if(!bitMapped) {
            MAP = new Bitmap[5];
            for (int i = 0; i < 5; i++) {
                MAP[i] = Bitmap.createBitmap(lvl.getMapSize().x, lvl.getMapSize().y, Bitmap.Config.ARGB_8888);
                //MAP[i - 1].setHasAlpha(true);
                bCanvas = new Canvas(MAP[i]);
                drawMap(i);
                bitMapped = true;
                bCanvas = null;
            }
        }

        Rect rect = new Rect(gap.x,gap.y,curUpperViewBound.x,curUpperViewBound.y);
        Rect rect2 = new Rect(0,0,MAX.x,MAX.y);

        //DRAWING LEVEL : BACKGROUND TILING
        canvas.drawBitmap(MAP[0], rect, rect2, null);
        //DRAWING LEVEL MAP : PERMA LAYER
        canvas.drawBitmap(MAP[1], rect, rect2, null);
        //DRAWING MECHA LAYER
        canvas.drawBitmap(MAP[2], rect, rect2, null);
        //DRAWING POWERUP LAYER
        canvas.drawBitmap(MAP[3], rect, rect2, null);

        //DRAWING ITEM LAYER
        //drawMap(5);

        //DRAWING PLAYER LAYER
        canvas.drawBitmap(hero.bitmap(), hero.pos().x - hero.width()/2 - gap.x,
                hero.pos().y - hero.height()/2 - gap.y, paint);

        if(debugging) drawGameDebugTools(); //DRAWING GRAPHS FOR DEBUGGING
    }
    private void drawMenu(){
        //paint,menu,tip,title,book,debug
        paint.setStrokeWidth(2);
        canvas.drawColor(menuBgColor);

        if(debugging)
        drawMenuDebugTools();

        paint.setColor(fontColor[2]); paint.setTextSize(fontSize[1]); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PLAY", MAX.x/2, MAX.y/2 + btnSize.y/2, paint);
    }
    private void drawGameControls(){
        if(debugging) drawGameControlDebug();
        if(leftDragHeld) {
            pointPaint.setStrokeWidth(4); pointPaint.setColor(dragColor);
            canvas.drawCircle(leftDragOrigin.x, leftDragOrigin.y, leftDragRadius.x, pointPaint);
            pointPaint.setStrokeWidth(4); pointPaint.setColor(Color.RED);
            canvas.drawCircle(lastLeftDragHeld.x, lastLeftDragHeld.y, 15, pointPaint);
        }
        if(rightDragHeld) {
            pointPaint.setStrokeWidth(4); pointPaint.setColor(dragColor);
            canvas.drawCircle(rightDragOrigin.x, rightDragOrigin.y, rightDragRadius.x, pointPaint);
            pointPaint.setStrokeWidth(4); pointPaint.setColor(Color.RED);
            canvas.drawCircle(lastRightDragHeld.x, lastRightDragHeld.y, 15, pointPaint);
        }
    }
    private void drawGameControlDebug(){
        pointPaint.setColor(dragColor);
        //canvas.drawRect(minLeftDragOrigin.x, minLeftDragOrigin.y, maxLeftDragOrigin.x, maxLeftDragOrigin.y, pointPaint);
        //canvas.drawRect(minRightDragOrigin.x, minRightDragOrigin.y, maxRightDragOrigin.x, maxRightDragOrigin.y, pointPaint);
        //canvas.drawRect(noControllerZone.x, 0, noControllerZone.y, MAX.y, pointPaint);

        paint.setColor(fontColor[5]); paint.setTextSize(fontSize[5]); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("lo: " + leftDragOrigin, fontSize[5],fontSize[5]*4+3, paint);
        canvas.drawText("POS " + hero.pos().toString(), fontSize[5],fontSize[5]*6+5, paint);
        canvas.drawText("CLVB " + curLowerViewBound.toString(), fontSize[5],fontSize[5]*7+6, paint);
        canvas.drawText("CUVB " + curUpperViewBound.toString(), fontSize[5],fontSize[5]*8+7, paint);
        canvas.drawText("LDH " + leftDragHeld, fontSize[5],fontSize[5]*9+8, paint);
    }
    private void drawGameDebugTools() {
        Point gap = new Point(curLowerViewBound);
        Paint viewBoundsPaint = new Paint();
        viewBoundsPaint.setColor(heroBoundColor);
        viewBoundsPaint.setStrokeWidth(2);
        int xLeft = hero.pos().x - hero.viewRadius() - gap.x;
        int xRight = hero.pos().x + hero.viewRadius() - gap.x;
        int yUp = hero.pos().y - hero.viewRadius();
        int yDown = hero.pos().y + hero.viewRadius(); //HERO RANGED BORDER
        canvas.drawLine(xLeft, yUp, xLeft, yDown, viewBoundsPaint);         //left
        canvas.drawLine(xRight, yUp, xRight, yDown, viewBoundsPaint);       //right
        canvas.drawLine(xLeft, yUp, xRight, yUp, viewBoundsPaint);          //top
        canvas.drawLine(xLeft, yDown, xRight, yDown, viewBoundsPaint);      //bottom

        pointPaint.setStrokeWidth(2);   //MOTION PATHS

        pointPaint.setColor(o2pColor);  //line from hero origin to hero position
        canvas.drawLine(hero.origin().x - gap.x, hero.origin().y,
                hero.pos().x - gap.x, hero.pos().y, pointPaint);

        pointPaint.setColor(a2bColor);  //line from path origin to path target
        canvas.drawLine(myPath.getA().x - gap.x, myPath.getA().y,
                myPath.getB().x - gap.x, myPath.getB().y, pointPaint);

        pointPaint.setColor(p2tColor);  //line from hero position to hero target, somehow this and the previous line always overlap each other
        canvas.drawLine(hero.pos().x - gap.x, hero.pos().y,
                hero.target().x - gap.x, hero.target().y, pointPaint);

        pointPaint.setColor(0xFFDD5500);
        canvas.drawLine(hero.pos().x-hero.width()/2-gap.x,0,hero.pos().x-hero.width()/2-gap.x,MAX.y,pointPaint);
        canvas.drawLine(0,hero.pos().y-hero.height()/2-gap.y,MAX.x,hero.pos().y-hero.height()/2-gap.y,pointPaint);

    }
    private void drawMenuDebugTools(){
        pointPaint.setStrokeWidth(2);
        pointPaint.setColor(a2bColor);
        canvas.drawRect(0, MAX.y/2-btnSize.y/2, MAX.x, MAX.y/2+btnSize.y/2, pointPaint);
        pointPaint.setColor(Color.RED);
        canvas.drawLine(0, MAX.y/2, MAX.x, MAX.y/2, pointPaint);
        pointPaint.setColor(Color.GREEN);
        canvas.drawPoint(randCoord.x, randCoord.y, pointPaint);
    }
    private void drawCursorPos(){
        pointPaint.setColor(Color.BLUE);
        canvas.drawLine(0, lastHeld.y, MAX.x, lastHeld.y, pointPaint);
        canvas.drawLine(lastHeld.x, 0, lastHeld.x, MAX.y, pointPaint);

        paint.setColor(fontColor[5]); paint.setTextSize(fontSize[5]); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(lastHeld.x + "," + lastHeld.y, fontSize[5],fontSize[5]*5+4, paint);
    }
    private void drawStats(){
        paint.setColor(fontColor[5]); paint.setTextSize(fontSize[5]); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("fps " + df.format(averageFps), fontSize[5], fontSize[5], paint);
        canvas.drawText("fs  " + df.format(framesSkippedPerStatCycle), fontSize[5],fontSize[5]*2+1, paint);
        canvas.drawText("MAX.x  " + MAX.x, fontSize[5],fontSize[5]*3+2, paint);
    }

    private void buildLevel(Point roomSize/*row,column*/, Point mapSize/*width,height*/, MAPTYPE maptype, double initialZoom){
        //, int roomCount, roomLayout.HORIZONTAL_ENDLESS
        //setting up game dimensions and level

        //explicit room size should be optional, otherwise follow a preset, or generated randomly
        //room generation using roomCount(s) should be inside the level class

        lvl = new level(roomSize, defCellSize, maptype);
        lvl.changeZoom(initialZoom);
        upperViewBound = new Point(defCellSize.x*roomSize.x, defCellSize.y*roomSize.y);           //width,height of full map
        maxGridSize = new Point(upperViewBound.x/defCellSize.x, upperViewBound.y/defCellSize.x);    //rows and columns cap for map

        //load all resources
        loadCachingResources();

        //deciding number of maps this level
        curTotalFrames = 3; //total
        curFrame = 1;       //initializing with the first
        curCheckPoint = 1;  //current progress point
        nextCheckPoint = 3; //progress save point
        Log.d(TAG, "Level loaded successfully");
        loaded = true;
    }

    //TODO: For Clickable View Accessibility
    //@Override
    //public boolean performClick(){return true;}

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        int x, y;
        if(playing && loaded) {
            x = (int) motionEvent.getX();
            y = (int) motionEvent.getY();
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    //for (int i = 0; i < motionEvent.getPointerCount() && i < 2; i++) {
                    //x = (int) motionEvent.getX(i);
                    //y = (int) motionEvent.getY(i);
                    if (x < noControllerZone.x) {
                        //its the left control
                        leftDragHeld = true;
                        if (x < minLeftDragOrigin.x) {
                            leftDragOrigin.x = minLeftDragOrigin.x;
                            lastLeftDragHeld.x = x;
                        } else if (x > maxLeftDragOrigin.x) {
                            leftDragOrigin.x = maxLeftDragOrigin.x;
                            lastLeftDragHeld.x = x;
                        } else {
                            leftDragOrigin.x = x;
                            lastLeftDragHeld.x = x;
                        }

                        if (y < minLeftDragOrigin.y) {
                            leftDragOrigin.y = minLeftDragOrigin.y;
                            lastLeftDragHeld.y = y;
                        } else if (y > maxLeftDragOrigin.y) {
                            leftDragOrigin.y = maxLeftDragOrigin.y;
                            lastLeftDragHeld.y = y;
                        } else {
                            leftDragOrigin.y = y;
                            lastLeftDragHeld.y = y;
                        }
                    }
                    else if (x > noControllerZone.y) {
                        //its the right control
                        rightDragHeld = true;

                        if (x < minRightDragOrigin.x) {
                            rightDragOrigin.x = minRightDragOrigin.x;
                            lastRightDragHeld.x = x;
                        } else if (x > maxRightDragOrigin.x) {
                            rightDragOrigin.x = maxRightDragOrigin.x;
                            lastRightDragHeld.x = x;
                        } else {
                            rightDragOrigin.x = x;
                            lastRightDragHeld.x = x;
                        }

                        if (y < minRightDragOrigin.y) {
                            rightDragOrigin.y = minRightDragOrigin.y;
                            lastRightDragHeld.y = y;
                        } else if (y > maxRightDragOrigin.y) {
                            rightDragOrigin.y = maxRightDragOrigin.y;
                            lastRightDragHeld.y = y;
                        } else {
                            rightDragOrigin.y = y;
                            lastRightDragHeld.y = y;
                        }
                    }
                    lastHeld.set(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                //for (int i = 0; i < motionEvent.getPointerCount() && i < 2; i++) {
                    //x = (int) motionEvent.getX(i);
                    //y = (int) motionEvent.getY(i);

                if(x<noControllerZone.x) {
                    if (wrtCircle(x, y, leftDragOrigin, leftDragRadius) < 1) {
                        if (x > leftDragOrigin.x - leftDragRadius.x && x < leftDragOrigin.x + leftDragRadius.x) {
                            leftDragDisp.x = x-leftDragOrigin.x;
                            lastLeftDragHeld.x = x;
                        }
                        if (y > leftDragOrigin.y - leftDragRadius.y && y < leftDragOrigin.y + leftDragRadius.y) {
                            leftDragDisp.y = y-leftDragOrigin.y;
                            lastLeftDragHeld.y = y;
                        }
                    }
                    else {
                        Point p = getCircleBisector(x, y, leftDragOrigin, leftDragRadius);
                        leftDragDisp.x = p.x-leftDragOrigin.x;
                        lastLeftDragHeld.x = p.x;
                        leftDragDisp.y = p.x-leftDragOrigin.y;
                        lastLeftDragHeld.y = p.y;
                    }
                    rightDragHeld = false;

                    //The day I lost my head:                                     TODO: nothing makes sense right now
                    //hero.setTarget(hero.pos());
                    //myPath.init(hero.pos(), hero.target());   //revisited:03-04-2022, Why init in the middle of things!? must be some forgotten debug
                }
                else if (x>noControllerZone.y) {
                    if (wrtCircle(x, y, rightDragOrigin, rightDragRadius) < 1) {
                        if (x > rightDragOrigin.x - rightDragRadius.x && x < rightDragOrigin.x + rightDragRadius.x) {
                            rightDragDisp.x = x-rightDragOrigin.x;
                            lastRightDragHeld.x = x;
                        }
                        if (y > rightDragOrigin.y - rightDragRadius.y && y < rightDragOrigin.y + rightDragRadius.y) {
                            rightDragDisp.y = y-rightDragOrigin.y;
                            lastRightDragHeld.y = y;
                        }
                    }
                    else {
                        Point p = getCircleBisector(x, y, rightDragOrigin, rightDragRadius);
                        rightDragDisp.x = p.x-rightDragOrigin.x;
                        lastRightDragHeld.x = p.x;
                        rightDragDisp.y = p.x-rightDragOrigin.y;
                        lastRightDragHeld.y = p.y;
                    }
                    leftDragHeld = false;
                }
                lastHeld.set(x, y);
                break;
            case MotionEvent.ACTION_UP:
                //for (int i = 0; i < motionEvent.getPointerCount() && i < 2; i++) {
                    //x = (int) motionEvent.getX(i);
                    //y = (int) motionEvent.getY(i);
                if(x<noControllerZone.x) {
                    if (leftDragHeld) {
                //        hero.setTarget(hero.pos());
                        hero.setVelocity(0, 0);
                //        myPath.init(hero.pos(), hero.target());
                        lastLeftDragHeld.set(x, y);
                    }
                }
                else if (x>noControllerZone.y){
                    if (rightDragHeld) {
                        lastRightDragHeld.set(x, y);
                    }
                    lastHeld.set(x, y);
                }
                leftDragHeld = false;
                rightDragHeld = false;
                break;
            }
        }
        else if (playing && !loaded) {

        } else if (!gameRequested) {
            x = (int) motionEvent.getX();
            y = (int) motionEvent.getY();
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:               //on touching the screen
                    if (y > MAX.y / 2 - btnSize.y / 2 && y < MAX.y / 2 + btnSize.y / 2)
                        selected = true;
                    lastHeld.set(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:               //on moving finger across the screen
                    if (selected) {
                        if (y > MAX.y / 2 - btnSize.y / 2 && y < MAX.y / 2 + btnSize.y / 2)
                            selected = true;
                        else
                            selected = false;
                    }
                    lastHeld.set(x, y);
                    break;
                case MotionEvent.ACTION_UP:                 //on lifting the finger up
                    if (selected) {
                        if (y > MAX.y / 2 - btnSize.y / 2 && y < MAX.y / 2 + btnSize.y / 2)
                            gameRequested = true;
                    }
                    lastHeld.set(x, y);
                    break;
            }
        }
        return true;
    }
    private void updateHeroControllerPosition(){

        //TODO: De-hibernation protocol activated, 5:02 AM 04-04-2022
        

        hero.setVelocity(leftDragDisp.x / 24, leftDragDisp.y / 24);
        //setting a target in multiple frames,       Done TODO: when you move, the world moves
        //hero.setTarget(curLowerViewBound.x + leftDragDisp.x, curLowerViewBound.y + leftDragDisp.y);

        if (curLowerViewBound.x + hero.velocity().x >= lowerViewBound.x && curUpperViewBound.x + hero.velocity().x <= upperViewBound.x) {
            hero.setPos(hero.pos().x + hero.velocity().x, hero.pos().y);
            if ((hero.pos().x < curLowerViewBound.x + hero.radius() && hero.velocity().x < 0)
            || (hero.pos().x > curUpperViewBound.x - hero.radius() && hero.velocity().x > 0)) {
                curLowerViewBound.x += hero.velocity().x;
                curUpperViewBound.x += hero.velocity().x;
            }
        }
        if (curLowerViewBound.y + hero.velocity().y >= lowerViewBound.y && curUpperViewBound.y + hero.velocity().y <= upperViewBound.y) {
            hero.setPos(hero.pos().x, hero.pos().y + hero.velocity().y);
            if ((hero.pos().y < curLowerViewBound.y + hero.radius() && hero.velocity().y < 0)
            || (hero.pos().y > curUpperViewBound.y - hero.radius() && hero.velocity().y > 0)) {
                curLowerViewBound.y += hero.velocity().y;
                curUpperViewBound.y += hero.velocity().y;
            }
        }
    }
    private void updateAimControlDirection(){}
    private void update() {
        //update game elements
        if(leftDragHeld) updateHeroControllerPosition();
        if(rightDragHeld) updateAimControlDirection();
        //if (!myPath.isTraced()) {
            //hero.setPos(myPath.getNext(hero.pos()));
        //}
        //hero.setCollided(false);
    }
    private void updateMenus() {

    }


    //game loop variables
    volatile boolean running;
    private boolean playing;
    private boolean debugging;
    private boolean gameRequested;

    //all the boundaries and dimensions
    private Point MAX;
    private Point menuMaxViewBound, maxGridSize;
    final private Point defaultResolution = new Point(16*40, 9*40);
    private Point defCellSize;
    private Point leftDragOrigin, rightDragOrigin, leftDragRadius, rightDragRadius;
    final private Point minLeftDragOrigin, minRightDragOrigin, maxLeftDragOrigin, maxRightDragOrigin;
    final private Point noControllerZone;

    //VIEW BOUNDARIES ENCLOSE THE VISIBLE AREA OF THE MAP (along x-axis here)
    private Point lowerViewBound, curLowerViewBound;
    private Point upperViewBound, curUpperViewBound;



    //DONE WELL, DO NOT TOUCH

    private void storeStats(){
        frameCountPerStatCycle++;
        totalFrameCount++;

        //check actual time
        statusIntervalTime += (System.currentTimeMillis() -statusIntervalTime);
        if(statusIntervalTime >= lastStatusStore + STAT_INTERVAL){
            //calculate the actual fps status check interval
            double actualFps = (double)(frameCountPerStatCycle / (STAT_INTERVAL/1000));

            //stores the latest fps in the array
            fpsStore[(int) statsCount % FPS_HISTORY_NR] = actualFps;

            //increase the number of times statistics was calculated
            statsCount++;

            double totalFps = 0.0;
            //sum up the stored fps values
            for (int i=0; i< FPS_HISTORY_NR;i++){
                totalFps += fpsStore[i];
            }

            //obtain the average
            if(statsCount < FPS_HISTORY_NR) {
                //in case of the first 10 triggers
                averageFps = totalFps / statsCount;
            } else{
                averageFps = totalFps/FPS_HISTORY_NR;
            }
            //saving the number of total frames skipped
            totalFramesSkipped+=framesSkippedPerStatCycle;
            //resetting the counters after a status record
            framesSkippedPerStatCycle = 0;
            statusIntervalTime = 0;
            frameCountPerStatCycle = 0;

            statusIntervalTime = System.currentTimeMillis();
            lastStatusStore = statusIntervalTime;
            Log.d(TAG, "Average FPS:" + df.format(averageFps));
        }
    }
    private void initTimingElements(){
        fpsStore = new double[FPS_HISTORY_NR];
        for(int i=0;i<FPS_HISTORY_NR;i++){
            fpsStore[i]=0.0;
        }
        Log.d(TAG+".initTimingElements()", "Timing elements for stats initialized");
    }
    private void control(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void pause() {
        //when the game is paused
        //setting the variable to false
        running = false;
        playing = false;
        try {
            //stopping the thread
            gameThread.join();
        } catch (InterruptedException e) {
            e.fillInStackTrace();
        }
    }
    public void resume() {
        //when the game is resumed
        //starting the thread again
        running = true;
        playing = false;
        gameThread = new Thread(this);
        gameThread.start();
    }
    private void loadCachingResources(){
        //creating entities (need to make PRESETS later on)
        //Entity grass, floor, lava, water;                                         //perma, first layer
        //Entity wall, weak_wall, gate, warp_wall, temple;                          //build, second layer
        //Entity gear, laser, portal, spring, box;                                  //bound, second layer
        //Entity speedUp, tresPass, freezeTime, freeHint, skipLvl, lifeUp, lifeDown;//tempo, second layer
        //Entity hero, power_up, mecha, path;                                       //items, third layer
        //Entity wind, water, wave, fog, bubble, sparks, sparkle, arrow;            //sfx,   last layer

        perma = new Vector<Entity>(1,1);        //stores cell behaviour functions, or just some fancy background tiles
        build = new Vector<Entity>(1,1);        //stores stores uninteractable obstacles and structures
        bound = new Vector<Entity>(1,1);        //stores behaviours for all non-player movables
        //stores functions to modify player properties TODO: load separately
        tempo = new Vector<Entity>(1,1);
        items = new Vector<Entity>(1,1);        //stores behaviours of interactable obstacles and obtainable items
        //stores functions for animated particles          TODO: load separately
        sfx   = new Vector<Entity>(1,1);

        int pr = 40, tr = 30, br = 35, ir = 30, ar = 10;    //count
        blank = Bitmap.createBitmap(2,2,Bitmap.Config.RGB_565);
        Canvas ctemp = new Canvas(blank);
        ctemp.drawColor(Color.RED); //making RED warning bitmap replacement for non-existent ones

        Entity temp;
//        String permal[] = getResources().getStringArray(R.array.perma);
//        String buildl[] = getResources().getStringArray(R.array.build);
//        String tempol[] = getResources().getStringArray(R.array.tempo);
//        String boundl[] = getResources().getStringArray(R.array.bound);
//        String itemsl[] = getResources().getStringArray(R.array.items);
//        String sfxl[]   = getResources().getStringArray(R.array.sfx);

//        for(int i=0; i<permal.length; i++){
//            temp = new Entity(permal[i],permal);
//        }

        //creating entities
        for(int entityIndex = 1; entityIndex < 33; entityIndex++) {

            temp = new Entity();

            //create game objects,                                         TODO: load from db
            switch(entityIndex) {
                /*perma*/
                case 1:  temp = new Entity("grass",    200, pr, 1, 1);
                    temp.setHasStates(1,1); break;
                case 2:  temp = new Entity("floor",    200, pr, 1, 1);
                    temp.setHasStates(1,1); break;
                case 3:  temp = new Entity("lava",     200, pr, 1, 1);
                    temp.setHasStates(1,1); break;
                case 4:  temp = new Entity("water",    200, pr, 1, 1);
                    temp.setHasStates(1,1); break;
                /*build*/
                case 5:  temp = new Entity("wall",     5000,pr, 1, 1);
                    temp.setHasStates(2,1); break;
                case 6:  temp = new Entity("weak_wall",500, pr, 1, 1);
                    temp.setHasStates(0,0); break;
                case 7:  temp = new Entity("gate",     200, pr, 1, 1);
                    temp.setHasStates(4,1); break;
                case 8:  temp = new Entity("warp",     1000,pr, 1, 1);
                    temp.setHasStates(0,0); break;
                case 9:  temp = new Entity("temple",   1000,pr, 1, 1);
                    temp.setHasStates(1,1); break;
                /*tempo*/
                case 10: temp = new Entity("speedup",  0,   tr, 1, 1);
                    temp.setHasStates(5,1); break;
                case 11: temp = new Entity("trespass", 0,   tr, 1, 1);
                    temp.setHasStates(1,1); break;
                case 12: temp = new Entity("freeze",   0,   tr, 1, 1);
                    temp.setHasStates(1,1); break;
                case 13: temp = new Entity("hint",     0,   tr, 1, 1);
                    temp.setHasStates(8,1); break;
                case 14: temp = new Entity("skip",     0,   tr, 1, 1);
                    temp.setHasStates(10,1); break;
                case 15: temp = new Entity("minuslife",0,   tr, 1, 1);
                    temp.setHasStates(5, 1); break;
                case 16: temp = new Entity("pluslife", 0,   tr, 1, 1);
                    temp.setHasStates(15,1); break;
                /*bound*/
                case 17: temp = new Entity("gear",     1000,br, 1, 1);
                    temp.setHasStates(0, 0); break;
                case 18: temp = new Entity("portal",   5000,br, 1, 1);
                    temp.setHasStates(10,1); break;
                case 19: temp = new Entity("laser",    1000,br, 1, 1);
                    temp.setHasStates(0, 0); break;
                case 20: temp = new Entity("box",      1000,ir, 1, 1);
                    temp.setHasStates(1, 1); break;
                /*items*/
                case 21: temp = new Entity("hero",     0,   ir, 1, 1);
                    temp.setHasStates(8, 1); break;
                case 22: temp = new Entity("path",     0,   ir, 1, 1);
                    temp.setHasStates(2, 1); break;
                case 23: temp = new Entity("power_up", 0,   ir, 1, 1);
                    temp.setHasStates(0, 0); break;
                case 24: temp = new Entity("mecha",    1000,ir, 1, 1);
                    temp.setHasStates(0, 0); break;
                /*sfx*/
                case 25: temp = new Entity("arrow",    0,   ir, 1, 1);
                    temp.setHasStates(1, 1); break;
                case 26: temp = new Entity("wind",     0,   ar, 1, 1);
                    temp.setHasStates(4, 1); break;
                case 27: temp = new Entity("watersfx",    0,   ar, 1, 1);
                    temp.setHasStates(4, 1); break;
                case 28: temp = new Entity("wave",     0,   ar, 1, 1);
                    temp.setHasStates(2, 1); break;
                case 29: temp = new Entity("fog",      0,   ar, 1, 1);
                    temp.setHasStates(0, 0); break;
                case 30: temp = new Entity("bubble",   0,   ar, 1, 1);
                    temp.setHasStates(0, 0); break;
                case 31: temp = new Entity("sparks",   0,   ar, 1, 1);
                    temp.setHasStates(0, 0); break;
                case 32: temp = new Entity("sparkle",  0,   ar, 1, 1);
                    temp.setHasStates(0, 0); break;
            }

            //loading bitmaps dynamically
            String resName;
            int resIndex = 0;
            Bitmap bmp;

            if(temp.hasStates() != 0)
                do {
                    resName = temp.name();
                    if (resIndex != 0) resName += Integer.toString(resIndex + 1);
                    int resId = getResources().getIdentifier(resName, "drawable", getContext().getPackageName());
                    Bitmap tempBmp = BitmapFactory.decodeResource(context.getResources(), resId);
                    if(resId == 0) {
                        Log.e("Could not load!", "resId is 0");
                    }
                    if(tempBmp == null){
                        Log.e("Could not load!", "Bitmap is null");
                    }
                    if(resName == null){
                        Log.e("Could not load!", "Name is null");
                    }
                    try {
                        bmp = Bitmap.createScaledBitmap(tempBmp, lvl.getCellSize().x, lvl.getCellSize().y, true);
                        Log.d(this.getClass().getSimpleName(), "Loaded\t" + resName);
                    } catch (Exception e){
                        e.fillInStackTrace();
                        bmp = Bitmap.createScaledBitmap(blank, lvl.getCellSize().x, lvl.getCellSize().y, true);
                        Log.d(this.getClass().getSimpleName(), "Could not load \t" + resName);
                    }
                    resIndex++;
                } while (temp.addBitmap(bmp) && resIndex < temp.hasStates());
            if (entityIndex<5){
                temp.setAnimationOptions(false, false, 0, STATE_CHANGE_STYLE.DIRECTIONAL);
                perma.add(temp); //push to perma
            }
            else if (entityIndex<10){
                temp.setAnimationOptions(false, false, 0, STATE_CHANGE_STYLE.BUILDING);
                build.add(temp); //push to perma
            }
            else if(entityIndex<17) {
                temp.setAnimationOptions(true, false, temp.hasStates()*10, STATE_CHANGE_STYLE.NONE);
                tempo.add(temp); //push to tempo
            }
            else if(entityIndex<21) bound.add(temp); //push to bound
            else if(entityIndex<25) items.add(temp); //push to items
            else                    sfx.add(temp);   //push to sfx

        }
        cache = new LruCache<>(perma.size());
        cache.put("perma", perma);

        //for now each rotation of a bitmap will occupy separate memory space, to be fixed by
        //TODO: implementing PRESET animations, EDIT: inner classes ready for PRESETS implementation
        //TODO: for objects that require just combination of two or more objects, default values for hasExtra and hasPowerUp should be loaded from here

        //positioning player
        hero = items.get(ITEMTYPE.HERO.ordinal());
        hero.resize(lvl.getCellSize().x/2,lvl.getCellSize().y/2);
        hero.setViewRadius(hero.height());
        hero.setAnimationOptions(false, false, hero.hasStates()*5, STATE_CHANGE_STYLE.DIRECTIONAL);
        hero.setOrigin(lvl.getStart().x*lvl.getCellSize().x + lvl.getCellSize().x/2,lvl.getStart().y*lvl.getCellSize().y + lvl.getCellSize().y/2);
        hero.setPos(hero.origin());
        myPath = new myLine(hero.pos(),hero.target());
    }
    private void cacheEntity(String key, Vector<Entity> entity){
        if(cache.get(key)==null){
            cache.put(key,entity);
        }
    }
    private Entity fetchEntity(String key, int index){
        //get stored object
        return cache.get(key).get(index);
    }

    //the thread, context, and cache
    private Thread gameThread = null;
    private LruCache<String, Vector<Entity>> cache;
    private Context context;

    //stat tools
    private static final String TAG = GameView.class.getSimpleName();
    private final static int MAX_FPS = 60;                      //desired fps
    private final static int MAX_FRAME_SKIPS = 30;              //max number of frames to skip
    private final static int FRAME_PERIOD = 1000/MAX_FPS;       //desired spf
    private final static int STAT_INTERVAL = 1000;              //delay between stat readings
    private final static int FPS_HISTORY_NR = 10;               //max fps history stored
    private DecimalFormat df = new DecimalFormat("0.##"); //setting a shorter df to display
    private long lastStatusStore = 0;               //last status store time
    private long statusIntervalTime = 0l;           //delay between status store
    private long totalFramesSkipped = 0l;           //total frames skipped
    private long framesSkippedPerStatCycle = 0l;    //frames skipped per cycle
    private int frameCountPerStatCycle = 0;         //number of rendered frames in an interval
    private long totalFrameCount = 0l;
    private double fpsStore[];                      //previous fps values
    private long statsCount = 0;                    //times stats taken
    private double averageFps = 0.0;                //the average fps since beginning
    private long sleepTime = 0;

    //our drawing tools
    private Paint paint, pointPaint;
    private SurfaceHolder surfaceHolder;
    private Canvas canvas, bCanvas;
    private Bitmap blank;
    private Bitmap MAP[];

}