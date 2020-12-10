package kudoku.subham.gameviewforms;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.text.DecimalFormat;
import java.util.Vector;

public class GameView3 extends SurfaceView implements Runnable {

    //game control variables
    private boolean menuHeld, menuPressed;
    private boolean leftDragHeld, rightDragHeld;        //drag controls held or not
    private Point lastLeftDragHeld, lastRightDragHeld;  //last held positions in drag controls
    private Point leftDragSpeed, rightDragSpeed;        //rate of change in held positions in drag controls

    //our game-play elements as objects TODO: loading array of objects from file instead of individual code clutter
    private Vector<Entity> perma, tempo, bound, items, sfx;
    private Entity hero;
    private myLine myPath;
    private level lvl;
    private int curFrame, curTotalFrames, curCheckPoint, nextCheckPoint;
    private boolean curFrameChanged = false;
    //debug props
    private int frameBgColor = Color.BLACK, o2pColor = Color.RED, a2bColor = Color.GRAY, p2tColor = Color.YELLOW, heroBoundColor = Color.GREEN;
    private int dragColor = Color.CYAN;


    //Class constructor
    public GameView3(Context contxt, Point screenResolution) {
        super(contxt);
        context = contxt;

        //initialising tools
        surfaceHolder = getHolder();
        paint = new Paint();
        pointPaint = new Paint();
        MAX = new Point(screenResolution);

        //game control variables
        running = true;
        debugging = true;
        gameRequested = false;
        menuLoaded = false;
        loaded = false;
        gamePaused = false;
        playing = false;
    }

    @Override
    public void run() {
        Log.d(TAG, "Starting app loop");
        initTimingElements();
        long startTime, timeDiff, framesSkipped;
        //physics phys = new physics();
        //boolean isInLineOfGates = false;

        if(!menuLoaded){
            loadUI();
        }

        if(gameRequested){
            if(!loaded) {
                Log.d(TAG, "Loading level");
                buildLevel(new Point(12, 12));
                Log.d(TAG, "Level loaded successfully");
            }
            else {
                Log.d(TAG, "Starting game loop");
                playing = true;
            }
        }

        while (running) {
            startTime = System.currentTimeMillis();
            framesSkipped = 0;

            if (playing) {
                if (curFrameChanged) {
                    //frame color changes along black-white gradient
                    frameBgColor += 0x00999999 / (nextCheckPoint - curFrame + 1);
                    curFrameChanged = false;
                }
                curUpperViewBound.x = curLowerViewBound.x + MAX.x;
            }   //update game background
            draw();              //draw updated frame
            if(playing) update();//update game entities
            else updateMenus();  //check last swipes, and make method calls to update menus

            timeDiff = System.currentTimeMillis() - startTime;
            sleepTime = FRAME_PERIOD - timeDiff;
            if(sleepTime > 0) control(sleepTime);   //use the remaining time to sleep
            while(sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS){
                //we need to catch up
                if(playing) update();    //update without rendering
                else updateMenus();
                sleepTime += FRAME_PERIOD;
                framesSkipped++;
            }
            if(framesSkipped > 0) Log.d(TAG, "Skipped:" + framesSkipped);
            framesSkippedPerStatCycle += framesSkipped; //for statistics
            storeStats();                               //call the routine to store the gathered stats
        }
    }
    private void draw() {
        //INITIALIZING DRAWING VARIABLES, TODO: Blitt some bitmaps to fasten up the job
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();
            if (playing && loaded) {
                drawGame();                         //DRAWING GAME LEVEL
                if(gamePaused) {
                    drawMenu();
                    if(debugging) drawDebugTools();
                }
                else if (debugging) drawGameDebugTools(); //DRAWING GRAPHS FOR DEBUGGING
            }
            else if(menuLoaded){
                drawMenu();
                if(debugging) drawDebugTools();
            }
            else canvas.drawColor(Color.BLACK);

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
    private void drawMap(int i) {
        int curPosX, curPosY, index;
        _CELL temp;

        for (int y = 0; y < lvl.rows(); y++) {
            curPosY = 0;
            for (int x = 0; x < lvl.cols(); x++) {
                curPosX = 0;
                temp = lvl._map[y*lvl.cols()+x];
                switch(i){
                    case 1:
                        canvas.drawBitmap(fetchEntity("perma", PERMATYPE.GRASS.ordinal()).bitmap(), curPosX, curPosY, paint);
                    break;
                    case 2:
                        index = temp.type().ordinal();
                        if(index< PERMATYPE.values().length) {
                            if(temp.type() == PERMATYPE.WALL) canvas.drawBitmap(fetchEntity("perma", index).bitmap(temp.state()), curPosX, curPosY, paint);
                            else
                                canvas.drawBitmap(fetchEntity("perma", index).bitmap(), curPosX, curPosY, paint);
                        }
                    break;
                    case 3:
                        index = temp.getMechaType().ordinal();
                        if(temp.getMechaType() != MECHATYPE.NONE)
                            canvas.drawBitmap(bound.get(index).bitmap(bound.get(index).hasCurrentState()), curPosX, curPosY, paint);
                    break;
                    case 4:
                        index = temp.getPowerType().ordinal();
                        if(temp.getPowerType() != POWERTYPE.NONE)
                            canvas.drawBitmap(tempo.get(index).bitmap(tempo.get(index).hasCurrentState()), curPosX, curPosY, paint);
                    break;
                    case 5:
                        index = temp.getItemType().ordinal();
                        //we need to summon subtypes randomly in their place, so initially they are mystery boxes,
                        // TODO: Instead of all values in ITEMTYPE having subtypes,
                        if(temp.getItemType() == ITEMTYPE.PATH)
                            canvas.drawBitmap(items.get(index).bitmap(items.get(index).hasCurrentState()), curPosX, curPosY, paint);
                        //right now all similar objects change states simultaneously, TODO: need to store more states or bring some randomness
                    break;
                }
                curPosX += defCellSize.x;
            }
            curPosY+=defCellSize.y;
        }
    }
    private void drawGame(){
        canvas.drawColor(frameBgColor);
        //DRAWING LEVEL : BACKGROUND
        drawMap(1);
        //DRAWING LEVEL MAP : PERMA LAYER
        drawMap(2);
        //DRAWING MECHA LAYER
        drawMap(3);
        //DRAWING POWERUP LAYER
        drawMap(4);
        //DRAWING ITEM LAYER
        //drawMap(5);

        //DRAWING PLAYER LAYER
        canvas.drawBitmap(hero.bitmap(), hero.pos().x - hero.width()/2 - curLowerViewBound.x,
                hero.pos().y - hero.height()/2 - curLowerViewBound.y, paint);
    }
    private void drawMenu(){
        paint.setStrokeWidth(2);
        canvas.drawColor(frameBgColor);
        paint.setColor(fontColor[2]); paint.setTextSize(fontSize[1]); paint.setTextAlign(Paint.Align.RIGHT);
        if(menuHeld) paint.setFakeBoldText(true);
        canvas.drawText("play", MAX.x/2, MAX.y/2+btnHeight*btnPadding + fontSize[1], paint);
        if(menuHeld) paint.setFakeBoldText(false);
    }
    private void drawGameDebugTools() {
        Paint viewBoundsPaint = new Paint();
        viewBoundsPaint.setColor(heroBoundColor);
        viewBoundsPaint.setStrokeWidth(2);
        int xLeft = hero.pos().x - curLowerViewBound.x - hero.viewRadius();
        int xRight = hero.pos().x - curLowerViewBound.x + hero.viewRadius();
        int yUp = hero.pos().y - curLowerViewBound.y - hero.viewRadius();
        int yDown = hero.pos().y - curLowerViewBound.y + hero.viewRadius(); //HERO RANGED BORDER
        canvas.drawLine(xLeft, yUp, xLeft, yDown, viewBoundsPaint);         //left
        canvas.drawLine(xRight, yUp, xRight, yDown, viewBoundsPaint);       //right
        canvas.drawLine(xLeft, yUp, xRight, yUp, viewBoundsPaint);          //top
        canvas.drawLine(xLeft, yDown, xRight, yDown, viewBoundsPaint);      //bottom

        pointPaint.setStrokeWidth(2);   //MOTION PATHS

        pointPaint.setColor(o2pColor);  //line from hero origin to hero position
        canvas.drawLine(hero.origin().x - curLowerViewBound.x, hero.origin().y - curLowerViewBound.y,
                hero.pos().x - curLowerViewBound.x, hero.pos().y - curLowerViewBound.y, pointPaint);

        pointPaint.setColor(a2bColor);  //line from path origin to path target
        canvas.drawLine(myPath.getA().x - curLowerViewBound.x, myPath.getA().y - curLowerViewBound.y,
                myPath.getB().x - curLowerViewBound.x, myPath.getB().y - curLowerViewBound.y, pointPaint);

        pointPaint.setColor(p2tColor);  //line from hero position to hero target, somehow this and the previous line always overlap each other
        canvas.drawLine(hero.pos().x - curLowerViewBound.x, hero.pos().y - curLowerViewBound.y,
                hero.target().x - curLowerViewBound.x, hero.target().y - curLowerViewBound.y, pointPaint);

        pointPaint.setStrokeWidth(4); pointPaint.setColor(dragColor);
        canvas.drawCircle(leftDragOrigin.x, leftDragOrigin.y, leftDragRadius.x, pointPaint);
        canvas.drawCircle(rightDragOrigin.x, rightDragOrigin.y, rightDragRadius.x, pointPaint);

    }
    private void drawDebugTools(){
        paint.setColor(fontColor[5]); paint.setTextSize(fontSize[5]); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("fps " + df.format(averageFps), fontSize[5], fontSize[5], paint);
        canvas.drawText("fs  " + df.format(framesSkippedPerStatCycle), fontSize[5],fontSize[5]*2+1, paint);
        canvas.drawText("MAX.x  " + MAX.x, fontSize[5],fontSize[5]*3+2, paint);
        paint.setColor(Color.RED);
        canvas.drawText(lastHeldPos.x + "," + lastHeldPos.y, fontSize[5],fontSize[5]*4+3, paint);
        pointPaint.setColor(fontColor[5]);
        canvas.drawLine(MAX.x/2,0,MAX.x/2,MAX.y,pointPaint);
        canvas.drawLine(0,MAX.y/2,MAX.x,MAX.y/2,pointPaint);
        paint.setColor(Color.RED);
        canvas.drawLine(0,MAX.y/2+btnPadding, MAX.x, MAX.y/2+btnPadding, pointPaint);
        canvas.drawLine(0,MAX.y/2+2*btnPadding+btnHeight, MAX.x, MAX.y/2+2*btnPadding+btnHeight, pointPaint);
        pointPaint.setColor(fontColor[5]);
        canvas.drawRect(0, MAX.y/2+btnPadding + btnPadding/2, MAX.x, MAX.y/2+btnPadding+btnHeight - btnPadding/2, pointPaint);
        pointPaint.setColor(Color.BLUE);
        canvas.drawLine(0,lastHeldPos.y,MAX.x,lastHeldPos.y,pointPaint);
        canvas.drawLine(lastHeldPos.x,0,lastHeldPos.x,MAX.y,pointPaint);
    }

    private void buildLevel(Point roomSize){
        //, int roomCount, roomLayout.HORIZONTAL_ENDLESS

        //setting up game dimensions and level
        mapGridSize = new Point(roomSize.x,roomSize.y);              //Rows and columns on screen

        //explicit room size should be optional, otherwise follow a preset, or generated randomly
        //room generation using roomCount(s) should be inside the level class

        maxViewBound = new Point(3*MAX.x, MAX.y);       //Boundary Dimensions of actual map
        mapDimension = new Point(MAX.x/mapGridSize.x, MAX.y/mapGridSize.y);   //initial level size
        defCellSize = new Point(MAX.y/mapGridSize.x, MAX.y/mapGridSize.y);    //best fit size of a square cell
        maxGridSize = new Point(MAX.x/defCellSize.x, MAX.y/defCellSize.y);    //rows and columns cap for screen

        lvl = new level(mapGridSize, maxViewBound, MAPTYPE.WALLED);

        //load all resources
        loadCachingResources();

        //deciding number of maps this level
        curTotalFrames = 3; //total
        curFrame = 1;       //initializing with the first
        curCheckPoint = 1;  //current progress point
        nextCheckPoint = 3; //progress save point

        loaded = true;
    }
    private void loadUI(){
        //initializing bounds
        lowerViewBound = new Point (0,0);                                           //initial lower boundary
        upperViewBound = new Point(lowerViewBound.x+MAX.x, lowerViewBound.y+MAX.y); //initial upper boundary
        curLowerViewBound = new Point(lowerViewBound);
        curUpperViewBound = new Point(upperViewBound);

        //menu props    //font props are in the order: paint,menu,tip,title,book,debug
        fontSize = getResources().getIntArray(R.array.font_size);
        fontColor= getResources().getIntArray(R.array.font_color);
        //if(MAX.x > MAX.y) btnHeight = MAX.y/8;
        //else btnHeight = MAX.y/13;

        btnPadding = fontSize[2];
        btnHeight = fontSize[2];

        //initializing game controllers
        leftDragRadius = new Point(MAX.x/12,MAX.x/12);
        rightDragRadius = new Point(leftDragRadius);
        leftDragOrigin = new Point((int)(1.5*leftDragRadius.x),MAX.y-(int)(1.5*leftDragRadius.y));
        rightDragOrigin = new Point(MAX.x-(int)(1.5*rightDragRadius.x),MAX.y-(int)(1.5*rightDragRadius.y));
        leftDragHeld = false; rightDragHeld = false;
        leftDragSpeed = new Point(0,0); rightDragSpeed = new Point(0,0);
        lastLeftDragHeld = leftDragOrigin; lastRightDragHeld = rightDragOrigin;
        lastHeldPos = new Point(0,0);

        //menu map dimensions
        maxViewBound = new Point(3*MAX.x, MAX.y);
        mapDimension = new Point(MAX.x/12, MAX.y/12);
        menuLoaded = true;

        resetUI();
    }
    private void resetUI(){
        //default UI state
        menuHeld = false;
        menuPressed = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        int x, y;
        if(playing && loaded && !gamePaused) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) motionEvent.getX();
                    y = (int) motionEvent.getY();
                    if(x > leftDragOrigin.x-leftDragRadius.x && x < leftDragOrigin.x+leftDragRadius.x
                            && y > leftDragOrigin.y-leftDragRadius.y && y < leftDragOrigin.y+leftDragRadius.y){
                        //its the left control
                        leftDragHeld = true;
                        lastLeftDragHeld.set(x, y);
                    }
                    if(x > rightDragOrigin.x-rightDragRadius.x && x < rightDragOrigin.x+rightDragRadius.x
                            && y > rightDragOrigin.y-rightDragRadius.y && y < rightDragOrigin.y+rightDragRadius.y){
                        //its the right control
                        rightDragHeld = true;
                        lastRightDragHeld.set(x, y);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    x = (int) motionEvent.getX();
                    y = (int) motionEvent.getY();

                    if(x > leftDragOrigin.x-leftDragRadius.x && x < leftDragOrigin.x+leftDragRadius.x
                            && y > leftDragOrigin.y-leftDragRadius.y && y < leftDragOrigin.y+leftDragRadius.y){
                        leftDragSpeed.set(lastLeftDragHeld.x-x,lastLeftDragHeld.y-y);

                        //setting a target in multiple frames along 2 axes
                        hero.setTarget(curLowerViewBound.x+leftDragSpeed.x, curLowerViewBound.y+leftDragSpeed.y);
                        //TODO: nothing makes sense right now
                        hero.setVelocity(leftDragSpeed.x/2, leftDragSpeed.y/2);
                        myPath.init(hero.pos(), hero.target());

                    }
                    else leftDragHeld = false;
                    if(x > rightDragOrigin.x-rightDragRadius.x && x < rightDragOrigin.x+rightDragRadius.x
                            && y > rightDragOrigin.y-rightDragRadius.y && y < rightDragOrigin.y+rightDragRadius.y){
                        rightDragSpeed.set(lastRightDragHeld.x-x,lastRightDragHeld.y-y);
                    }
                    else rightDragHeld = false;

                    break;
                case MotionEvent.ACTION_UP:
                    if(leftDragHeld) {
                        hero.setTarget(hero.pos());
                        myPath.init(hero.pos(), hero.target());
                        leftDragHeld = false;
                    }
                    if(rightDragHeld) rightDragHeld = false;
            }
        }
        else if(playing && !loaded && !gamePaused){

        }
        else if(menuLoaded || gamePaused){
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) motionEvent.getX();
                    y = (int) motionEvent.getY();
                    lastHeldPos.set(x, y);
                    menuPressed=findHorzMenuHit(y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    x = (int) motionEvent.getX();
                    y = (int) motionEvent.getY();
                    lastHeldPos.set(x, y);
                    menuPressed=findHorzMenuHit(y);
                    break;
                case MotionEvent.ACTION_UP:
                    x = (int) motionEvent.getX();
                    y = (int) motionEvent.getY();
                    lastHeldPos.set(x, y);
                    if(menuPressed && findHorzMenuHit(y)) {
                        menuHeld = true;
                    }
                    else menuPressed = false;
                    break;
            }
        }
        return true;
    }
    private void update() {
        //update game elements
        if (!myPath.isTraced()) {
            //TODO: when you move, the world moves
            hero.setPos(myPath.getNext(hero.pos()));
        }
        hero.setCollided(false);
    }
    private void updateMenus() {
    }

    //DONE WELL, DO NOT TOUCH

    private boolean findHorzMenuHit(int y){
        return (y>=MAX.y/2+btnHeight*btnPadding && y<=MAX.y/2+btnHeight*btnPadding+fontSize[1]);
    }
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
        gameRequested = false;
        resetUI();
        gameThread = new Thread(this);
        gameThread.start();
    }
    private void loadCachingResources(){
        //creating entities (need to make PRESETS later on)
        //Entity wall, weak_wall, gate, warp_wall, temple, blank;                   //perma, first layer
        //Entity gear, laser, portal, spring, box;                                  //bound, second layer
        //Entity speedUp, tresPass, freezeTime, freeHint, skipLvl, lifeUp, lifeDown;//tempo, second layer
        //Entity hero, power_up, mecha, path;                                       //items, third layer
        //Entity wind, water, wave, fog, bubble, sparks, sparkle, arrow;            //sfx,   last layer

        perma = new Vector<Entity>(1,1);        //stores cell behaviour functions
        bound = new Vector<Entity>(1,1);        //stores behaviours for all non-player movables
        items = new Vector<Entity>(1,1);        //stores behaviours of obstacles and obtainable items
        tempo = new Vector<Entity>(1,1);        //stores functions to modify player properties TODO: load separately
        tempo = new Vector<Entity>(1,1);        //stores functions to modify player properties TODO: load separately
        sfx   = new Vector<Entity>(1,1);        //stores functions for animated particles TODO: load separately

        int pr = 40, tr = 30, br = 35, ir = 30, ar = 10;    //count

        //creating entities
        for(int entityIndex = 0; entityIndex < 30; entityIndex++) {

            Entity temp = new Entity();

            //create game objects TODO: load from db
            switch(entityIndex) {
                /*perma*/   case 0:  temp = new Entity("wall",     5000,pr, 2, 1); break;
                case 1:  temp = new Entity("weak_wall",500, pr, 0, 0); break;
                case 2:  temp = new Entity("gate",     200, pr, 4, 1); break;
                case 3:  temp = new Entity("warp",     1000,pr, 0, 0); break;
                case 4:  temp = new Entity("temple",   1000,pr, 1, 1); break;
                case 5:  temp = new Entity("grass",    200, pr, 1, 1); break;
                case 6:  temp = new Entity("blank",    200, pr, 1, 1); break;
                /*tempo*/   case 7:  temp = new Entity("speedup",  0,   tr, 5, 1); break;
                case 8:  temp = new Entity("trespass", 0,   tr, 1, 1); break;
                case 9:  temp = new Entity("freeze",   0,   tr, 1, 1); break;
                case 10: temp = new Entity("hint",     0,   tr, 8, 1); break;
                case 11: temp = new Entity("skip",     0,   tr, 10,1); break;
                case 12: temp = new Entity("minuslife",0,   tr, 5, 1); break;
                case 13: temp = new Entity("pluslife", 0,   tr, 15,1); break;
                /*bound*/   case 14: temp = new Entity("gear",     1000,br, 0, 0); break;
                case 15: temp = new Entity("portal",   5000,br, 10,1); break;
                case 16: temp = new Entity("laser",    1000,br, 0, 0); break;
                case 17: temp = new Entity("box",      1000,ir, 1, 1); break;
                /*items*/   case 18: temp = new Entity("hero",     0,   ir, 8, 1); break;
                case 19: temp = new Entity("path",     0,   ir, 2, 1); break;
                case 20: temp = new Entity("power_up", 0,   ir, 0, 0); break;
                case 21: temp = new Entity("mecha",    1000,ir, 0, 0); break;
                /*sfx*/     case 22: temp = new Entity("arrow",    0,   ir, 1, 1); break;
                case 23: temp = new Entity("wind",     0,   ar, 4, 1); break;
                case 24: temp = new Entity("water",    0,   ar, 4, 1); break;
                case 25: temp = new Entity("wave",     0,   ar, 2, 1); break;
                case 26: temp = new Entity("fog",      0,   ar, 0, 0); break;
                case 27: temp = new Entity("bubble",   0,   ar, 0, 0); break;
                case 28: temp = new Entity("sparks",   0,   ar, 0, 0); break;
                case 29: temp = new Entity("sparkle",  0,   ar, 0, 0); break;
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
                        Log.d("FATAL!", "resId is 0");
                    }
                    if(tempBmp == null){
                        Log.d("FATAL!", "Bitmap is null");
                    }
                    if(resName == null){
                        Log.d("FATAL!", "Name is null");
                    }
                    bmp = Bitmap.createScaledBitmap(tempBmp, defCellSize.x, defCellSize.y, true);
                    resIndex++;
                } while (temp.addBitmap(bmp) && resIndex < temp.hasStates());
            if (entityIndex<7){
                temp.setAnimationOptions(false, false, 0, STATE_CHANGE_STYLE.BUILDING);
                perma.add(temp); //push to perma
            }
            else if(entityIndex<14) {
                temp.setAnimationOptions(true, false, temp.hasStates()*10, STATE_CHANGE_STYLE.NONE);
                tempo.add(temp); //push to tempo
            }
            else if(entityIndex<18) bound.add(temp); //push to bound
            else if(entityIndex<23) items.add(temp); //push to items
            else                    sfx.add(temp);   //push to sfx

        }
        cache = new LruCache<>(perma.size());
        cache.put("perma", perma);

        //for now each rotation of a bitmap will occupy separate memory space, to be fixed by
        //TODO: PRESETS implementation, which utilises 'nature' of animation
        //TODO: for objects that require just combination of two or more objects, default values for hasExtra and hasPowerUp should be referred from here

        //positioning player
        hero = items.get(ITEMTYPE.BALL.ordinal());
        hero.resize(defCellSize.x/2,defCellSize.y/2);
        hero.setViewRadius(hero.height());
        hero.setAnimationOptions(false, false, hero.hasStates()*5, STATE_CHANGE_STYLE.DIRECTIONAL);
        hero.setOrigin(lvl.getStart().x*defCellSize.x + defCellSize.x/2,lvl.getStart().y*defCellSize.y + defCellSize.y/2);
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
    private static final String TAG = GameView3.class.getSimpleName();
    private final static int MAX_FPS = 60;                      //desired fps
    private final static int MAX_FRAME_SKIPS = 20;              //max number of frames to skip
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
    private Canvas canvas;
    
    //enums
    private enum menu_area {center,left,guide,sound,in_game}

    //menu props
    private int btnHeight, btnPadding;
    private Point lastHeldPos;

    private int fontSize[], fontColor[];
    private int menuBgColor = Color.WHITE;

    //game loop variables
    volatile boolean running;
    private boolean playing, debugging, gamePaused;
    private boolean menuLoaded, gameRequested, loaded;

    //all the boundaries and dimensions
    private Point MAX;
    private Point maxViewBound, mapDimension, mapGridSize, maxGridSize;
    final private Point defaultResolution = new Point(16*40, 9*40);
    private Point defCellSize;
    private Point leftDragOrigin, rightDragOrigin, leftDragRadius, rightDragRadius;

    //VIEW BOUNDARIES ENCLOSE THE VISIBLE AREA OF THE MAP (along x-axis here)
    private Point lowerViewBound, curLowerViewBound;
    private Point upperViewBound, curUpperViewBound;
}