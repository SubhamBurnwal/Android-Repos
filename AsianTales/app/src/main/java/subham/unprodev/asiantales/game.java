package subham.unprodev.asiantales;

import android.content.Context;
import android.graphics.*;
import android.support.annotation.Nullable;
import java.util.Random;
import java.util.Vector;

class shape{
    private int pivot;
    private Point _pivotPoint;
    private Path _path;
    private Point min;
    private Point max;
    private Vector<Point> shape;
    private Vector<Point> targetShape;
    private int _fillColor, _strokeColor;
    private int _strokeWidth;
    private int _rotationMode; // 0: around corners, 1: around pivot
    private boolean _isAnimated;
    private Vector<Integer> _bitmapID;
    private boolean _isBitmapAnimated = false;
    private boolean _readyToAnimate = false;
    private int _bitmapState;
    private Vector<Integer> lastChange; //TODO: will evolve to support several animations
    private Vector<Point> _updateRate;
    private Point _xUpdateRate;
    private Point _yUpdateRate;

    //TODO: add functions for working with states

    shape(){
        shape = new Vector<>();
        pivot = 1;
        _pivotPoint = new Point(0,0);
        _isAnimated = false;
        _fillColor = 0xFFFFFFFF;
        _strokeColor = 0xFF000000;
        _strokeWidth = 2;
        _isBitmapAnimated = false;
        _bitmapID = new Vector<>();
        _bitmapState=0;
    }
    public void add(Point next){
        shape.add(next);
        updateMinMax(next);
    }
    public boolean updatePath(){
        if(!hitTarget()) {
            //approachTargetSize();
            //initPath();
            //return true;
        }
        if(!hitTargetApprox()){
            approachTargetCorners();
            initPath();
            return true;
        }
        else return false;
    }
    public void initPath() {
        //TODO: learn matrix //_path should update w.r.t pivot and _pivotPoint
        //TODO: learn to reuse paths
        _path = new Path();
        for (int i = 0; i < shape.size(); i++) {
            if (i == 0) _path.moveTo(shape.get(0).x, shape.get(0).y);
            else _path.lineTo(shape.get(i).x, shape.get(i).y);
        }
        _path.close();
    }
    public boolean hitTarget(){
        return(shape == targetShape);
    }
    public boolean hitTargetApprox(int index){
        int errx = shape.get(lastChange.get(index)).x - targetShape.get(lastChange.get(index)).x;
        int erry = shape.get(lastChange.get(index)).y - targetShape.get(lastChange.get(index)).y;
        if(!(errx < 5 && errx > -5 && erry < 5 && erry > -5)) return false;
        return true;
    }
    public boolean hitTargetApprox(){
        for(int i=0;i<lastChange.size();i++){
            if(!hitTargetApprox(i)) return false;
        }
        return true;
    }
    private void updateMinMax(Point next){
        if(min == null) {
            min = new Point(next);
            max = new Point(next);
        }
        else if(next.x<min.x) min.x = next.x;
        else if(next.x>max.x) max.x = next.x;
        if(next.y<min.y) min.y = next.y;
        else if(next.y>max.y) max.y = next.y;
    }
    public void setPivot(int index/*0 is center, default: 1*/){
        //TODO: non-corner pivots?
        pivot = index;
    }
    public void setFreePivotPoint(Point p){
        _pivotPoint = p;
    }
    public void setPoint(int index, int x, int y){
        if (isAnimated()) {
            _readyToAnimate = false;
            targetShape.set(index, new Point(x, y));
            boolean setNewChange = true;
            for(int i=0;i<lastChange.size();i++){
                if(lastChange.get(i) == index) {
                    setNewChange = false;
                    break;
                }
            }
            if(setNewChange) lastChange.add(index);
            calcUpdateRatios();
            _readyToAnimate=true;
        }
        else shape.set(index, new Point(x,y));
        updateMinMax(new Point(x,y));
    }
    private double mod(float a, float b){
        return Math.sqrt(a*a - b*b);
    }
    public void setRotation(float angle){
    }
    public void setRotation(float angle, int pivotID){
    }
    public void setOffsetFromOrigin(int dx, int dy){
        if(isAnimated()) {
            for (int i = 0; i < sides(); i++) {
                targetShape.add(new Point(shape.get(i).x + dx, shape.get(i).y + dy));
            }
        }
        else
            for(int i=0; i<sides(); i++){
                shape.get(i).x+=dx;
                shape.get(i).y+=dy;
            }
    }
    public void setOffsetFromGiven(int dx, int dy, Point origin){//change this asap
        calcUpdateRatios(dx,dy,origin);//???!
        if (origin.x > getPivot().x) dx=-dx;
        if (origin.y > getPivot().y) dy=-dy;
        if (isAnimated()) {
            for (int i = 0; i < sides(); i++) {
                targetShape.add(new Point(shape.get(i).x + dx, shape.get(i).y + dy));
            }
        }
        else
            for(int i=0; i<sides(); i++){
                shape.get(i).x+=dx;
                shape.get(i).y+=dy;
            }
    }
    public void setSizeFromGiven(int dx, int dy, Point origin){//change this asap
        calcUpdateRatios(dx,dy,origin);//???!
        setFreePivotPoint(origin);
        if (origin.x > getFreePivotPoint().x) dx=-dx;
        if (origin.y > getFreePivotPoint().y) dy=-dy;
        if (isAnimated()) {
            for (int i = 0; i < sides(); i++) {
                targetShape.add(new Point(shape.get(i).x + dx, shape.get(i).y + dy));
            }
        }
        else
            for(int i=0; i<sides(); i++){
                shape.get(i).x+=dx;
                shape.get(i).y+=dy;
            }
    }
    public void setUpdateRatios(int left_dx, int right_dx, int up_dy, int down_dy){
        _xUpdateRate = new Point(left_dx,right_dx);
        _yUpdateRate = new Point(up_dy,down_dy);
    }
    private void calcUpdateRatios(int dx, int dy, Point origin){
        //find farthest x from given pivot
        //find farthest y from given pivot
        _xUpdateRate = new Point((origin.x-min.x)/(max.x-min.x)*dx
                ,(max.x-origin.x)/(max.x-min.x)*dx);
        _yUpdateRate = new Point((origin.y-min.y)/(max.y-min.y)*dy
                ,(max.y-origin.y)/(max.y-min.y)*dy);
    }
    private void calcUpdateRatios(){
        try {
            for (int i = 0; i < lastChange.size(); i++) {
                int hcf = approxHCF(targetShape.get(lastChange.get(i)).x - shape.get(lastChange.get(i)).x, targetShape.get(lastChange.get(i)).y - shape.get(lastChange.get(i)).y);
                _updateRate.add(new Point(hcf, hcf));
            }
        }catch(Exception e){
            return;
        }
    }
    private void calcUniformUpdateRatios() {    //not the right thing to do
        int hcf=0;
        for (int i = 0; i < lastChange.size(); i++){
            hcf += approxHCF(targetShape.get(lastChange.get(i)).x - shape.get(lastChange.get(i)).x, targetShape.get(lastChange.get(i)).y - shape.get(lastChange.get(i)).y);
        }
        setUniformUpdateRatios(hcf/sides(),hcf/sides());
    }
    private void setUpdateRatio(int index, int dx, int dy){
        _updateRate.set(index,new Point(dx,dy));
    }
    private void setUniformUpdateRatios(int dx, int dy){
        if(_updateRate.size() != 0) _updateRate.setSize(0);
        for(int i=0;i<sides();i++) _updateRate.add(i,new Point(dx,dy));
    }
    private int approxHCF(int a, int b){    //TODO: better replace this asap
        int max = 3, best=1;
        do {
            for (int i = 1; i < (a > b ? b : a); i++) {
                if (a % i < max && b % i < max) {
                    best = i;
                }
            }
            max++;
        }while(best==1);
        return best;
    }
    private boolean approachTargetSize(){
        int dx,dy;
        if(!hitTarget())
        for(int i=0; i<shape.size();i++) {
            if (shape.get(i).x > getFreePivotPoint().x) dx=_xUpdateRate.y;
            else dx=_xUpdateRate.x;
            if (shape.get(i).y > getFreePivotPoint().y) dy=_xUpdateRate.x;
            else dy=_xUpdateRate.y;
            shape.set(i, new Point(shape.get(i).x+dx,shape.get(i).y+dy));
        }
        return hitTarget();
    }
    private boolean approachTargetCorner(int index){    //index of an element in the list of changed points, not the whole shape
        if(!hitTargetApprox(index))
            shape.set(lastChange.get(index), new Point(shape.get(lastChange.get(index)).x+_updateRate.get(index).x,shape.get(lastChange.get(index)).y+_updateRate.get(index).y));
        return hitTargetApprox(index);
    }
    private boolean approachTargetCorners(){
        for(int i=0;i<lastChange.size();i++) {
            if(approachTargetCorner(i)) {
                eliminateTargets(i);
                i--;
            }
        }
        return hitTargetApprox();
    }
    private boolean eliminateTargets(int index){
        if(index < lastChange.size()) {
            for(int i=index;i<lastChange.size()-1;i++){
                _updateRate.set(i, _updateRate.get(i+1));
                _updateRate.setSize(_updateRate.size()-1);
                lastChange.set(i, lastChange.get(i+1));
                lastChange.setSize(lastChange.size()-1);
            }
            return true;
        }
        return false;
    }
    public void setRotationMode(int rotationMode/*0: around corners, 1: around pivot*/){
        _rotationMode = rotationMode;
    }
    public void setIsAnimated(boolean isAnimated){
        if(targetShape == null){
            targetShape = new Vector<>();
            _xUpdateRate = new Point(0,0);
            _yUpdateRate = new Point(0,0);
            _updateRate = new Vector<>();
            lastChange = new Vector<>();
            for(int i=0; i<sides(); i++) {
                targetShape.add(new Point(shape.get(i)));
            }
        }
        _isAnimated = isAnimated;
    }
    public void setPalette(int fillColor, int strokeColor){
        _fillColor=fillColor;
        _strokeColor=strokeColor;
    }
    public void setStrokeWidth(int strokeWidth){
        _strokeWidth = strokeWidth;
    }
    public void addBitmapID(int bitmapID){_bitmapID.add(bitmapID);}
    public void setBitmapState(int stateID){
        _bitmapState = stateID;
    }

    public Point get(int index){
        if(index >= 0 && index < shape.size())
        return shape.get(index);
        return null;
    }
    public Point getPivot() {
        return shape.get(pivot);
    }
    public Point getFreePivotPoint(){ return _pivotPoint; }
    public Path getPath() {
        return _path;
    }
    public int getFillColor(){
        return _fillColor;
    }
    public int getStrokeColor(){
        return _strokeColor;
    }
    public int getStrokeWidth() {
        return _strokeWidth;
    }
    public int getRotationMode(){ return _rotationMode; }
    public boolean isAnimated(){
        return _isAnimated;
    }
    public Point getMinBounds(){return min;}
    public Point getMaxBounds(){return max;}
    public int sides(){
        return shape.size();
    }
    public int getBitmapID(){return _bitmapID.get(_bitmapState);}

    public boolean isReadyToAnimate() {
        return _readyToAnimate;
    }
}
/*
class quad extends shape{
    quad(Point p, Point p2, Point p3, Point p4){
        shape = new Vector<>();
        shape.add(p);
        shape.add(p2);
        shape.add(p3);
        shape.add(p4);
    }
}
class rect extends shape{
    private Point size;                         //static size Point
    rect(int x, int y, int x2, int y2){
        shape = new Vector<>();
        shape.add(new Point(x,y));
        shape.add(new Point(x,y2));
        shape.add(new Point(x2,y2));
        shape.add(new Point(x2,y));
        size = new Point(x2-x,y2-y);
    }
    public int width(){
        return size.x;
    }
    public int height(){
        return size.y;
    }
    public void changeSizeBy(int dx, int dy){
    }
}
*/
enum contentType{story,choice,minigame,cutscene,interactive,achievement,credits,ad}
class card {
    private int ID;
    private contentType ctype;
    private int miniGameID;
    private Vector<Integer> previous;
    private Vector<Integer> next;
    private Vector<shape> shapes;
    private Vector<Integer> sharedID;
    private Vector<Integer> states;
    private Vector<Integer> statUpdates;
    
    card (int cardID,contentType type){
        ID=cardID;
        ctype=type;
        shapes=new Vector<>();
    }
    public int ID(){
        return ID;
    }
    public contentType ctype(){return ctype;}
    public int previous(int index){return previous.get(index);}
    public int previousCount(){return previous.size();}
    public int next(int index){return next.get(index);}
    public int nextCount(){return next.size();}
    public Vector<shape> shapes(){return shapes;}
    public int sharedID(int index){return sharedID.get(index);}
    public int sharedCount(){
        return sharedID.size();
    }
    public Vector<Integer> states(){return states;}
    public int miniGame(){
        return miniGameID;
    }
    public Vector<Integer> statUpdates(){ return statUpdates;}
    
    public void setMiniGame(int miniGame){miniGameID=miniGame;}
    public void addPreviousCard(int prevCard){
        if(previous == null)
            previous = new Vector<>();
        previous.add(prevCard);
    }
    public void addNextCard(int nextCard){
        if(next == null)
            next = new Vector<>();
        next.add(nextCard);
    }
    public void addShape(shape shape){shapes.add(shape);}
    public void addShared(int shared){
        if(sharedID == null)
            sharedID = new Vector<>();
        sharedID.add(shared);
    }
    public void addState(int state){
        if(states == null)
            states = new Vector<>();
        states().add(state);
    }
}

class level {

    //system,level perma vars
    private Context context;
    public Paint p;
    public Canvas c;
    private Canvas _c;
    public Bitmap b;
    private Point deviceSize;
    private int notchHeight = 0;
    private Vector<card> pack;
    private card temp;

    private boolean canvasChanged = true;

    public boolean ready = false;

    int nextCard;

    level(Context contxt, float factor/*, layout layout_ID*/) {
        context = contxt;
        deviceSize = new Point(contxt.getResources().getDisplayMetrics().widthPixels, contxt.getResources().getDisplayMetrics().heightPixels);

        //init drawing tools
        b = Bitmap.createBitmap(deviceSize.x, deviceSize.y, Bitmap.Config.ARGB_8888);
        b.prepareToDraw();

        _c = new Canvas(b);

        p = new Paint();
        p.setAntiAlias(true);

        //calculate max dimensions for all elements
        //measuring initial dimensions and offsets
        //load episode
        int episodeID=1;
        nextCard=0;
        //load cards as much as memory allows but at-most fixed limit
        loadCards(episodeID);
        //loading bitmaps into cache

        //ready = true;
    }

    //methods busy most of the time
    public void processInput(Point was_clicked, int actionID){
        //if(actionID == this this) do this(); //swiping left/right
        //else
        if(ready)
        switch(trackClick(was_clicked)) {
            case 0: nextCard++; if(nextCard>=pack.size()) nextCard=0;
                pack.get(0).shapes().get(0).setPoint(2, was_clicked.x, was_clicked.y);
                break;
            case 1: break;
            case 2: break;
            case 3: break;
        }
    }
    public void update_counters(){
    }
    private int trackClick(Point input){
        //return toolID for tools that was touched
        return 0;
    }
    private int random(int min, int max){
        return new Random().nextInt((max - min) + 1) + min;
    }

    //methods for game progress

    //methods for drawing on canvas
    public void draw(){
        if(canvasChanged) {
            drawBackground();
            drawNextCard();
            drawTools();
            canvasChanged = false;
        }
        //blit the bitmap! YAY!!
        if(pack.get(0).shapes().get(0).isReadyToAnimate()) {
            if (!pack.get(0).shapes().get(0).hitTargetApprox()) {
                pack.get(0).shapes().get(0).updatePath();
                canvasChanged = true;
            }
        }
        Matrix matrix = new Matrix();
        c.drawBitmap(b, matrix, new Paint());
        ready = true;
    }
    private void drawBackground(){
        p.setColor(0xFFFFFFFF);
        _c.drawRect(0,0,deviceSize.x,deviceSize.y,p);
    }
    private void drawNextCard(){
        drawCard(nextCard);
    }
    private void drawCard(int ID){
        if(temp.ID() != ID)
        temp = pack.get(ID);
        for(int i=0; i<temp.shapes().size();i++)
        drawShape(temp.shapes().get(i));
    }
    private void drawTools(){}
    private void drawShape(shape shape){
        p.setStrokeWidth(shape.getStrokeWidth());
        try{
            //load and draw bitmap first

            //draw filled shape
            p.setColor(shape.getFillColor());
            _c.drawPath(shape.getPath(), p);
            //draw border
            p.setColor(shape.getStrokeColor());
            for(int i=0; i<shape.sides(); i++)
                _c.drawLine(shape.get(i).x, shape.get(i).y, shape.get(i+1).x, shape.get(i+1).y, p);
            _c.drawLine(shape.get(0).x, shape.get(0).y, shape.get(shape.sides()-1).x, shape.get(shape.sides()-1).y, p);
        }
        catch (Exception e){
            e.fillInStackTrace();
            System.out.println("Error: drawShape() failed!");
        }
    }
    private void drawText(String val, int x, int y, float text_size, int text_color, @Nullable Typeface typeface){
        p.setTextSize(text_size);
        p.setColor(text_color);
        if(typeface!=null)
            p.setTypeface(typeface);
        _c.drawText(val, x, y, p);
    }

    //methods used to load cards
    private void loadAd(int cardID){}
    private void loadCards(int id){
        pack = new Vector<>();
        int i=0;
        //retrieve resources

        //decode and load resources
        temp = new card(i,contentType.story);
        //sample data
        temp.addNextCard(i+1);
        temp.addPreviousCard(i);
        temp.addState(1);
        temp.addShared(0);
        
        //shapes will be stored like percentage, and then calculated here
        shape s = new shape();
        s.setPivot(0);
        s.setPalette(0xFFFFFFFF, 0xFF000000);
        
        //we should have some standard presets
        s.add(new Point(100,120));
        s.add(new Point(600, 200));
        s.add(new Point(60, 520));
        s.add(new Point(240, 300));
        s.initPath();
        s.setStrokeWidth(5);
        s.setIsAnimated(true);
        
        temp.addShape(s);
        pack.add(temp);
    }
    private int getResourceID(String resourceName, String resourceType){
        try {
            int rID = context.getResources().getIdentifier(resourceName, resourceType, context.getPackageName());
            return rID;
        } catch(Exception e){
            e.printStackTrace();
            return -1;
        }
    }

}