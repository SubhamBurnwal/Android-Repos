package subham.dungeondash;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;

import java.util.Vector;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/*
class bhLine {
	    bhLine() {
        A = new Point();
        B = new Point();
        _isEmpty = true;
    }

    myLine(Point start, Point end) {
        A = new Point(start);
        B = new Point(end);
        _init();
    }

    public void setA(Point start) {
        A = start;
        _init();
    }

    public void setB(Point end) {
        B = end;
        _init();
    }

    public void init(Point start, Point end) {
            A = start;
            B = end;
            _init();
    }

    private void _init() {
        _isEmpty = false;
        traced = false;
        w = B.x - A.x;
        h = B.y - A.y;

        if (w < 0) dx = -1;
        else if (w > 0) dx = 1;
        else dx = 0;
        if (h < 0) dy = -1;
        else if (h > 0) dy = 1;
        else dy = 0;

        if (abs(h) >= abs(w)) error = abs(2 * w) - abs(h);
        else if (abs(w) > abs(h)) error = 2 * abs(h) - abs(w);
    }

    public boolean isTraced() {
        return traced;
    }

    public boolean isEmpty() {
        return _isEmpty;
    }

    private boolean _isEmpty;
    private boolean traced;
    private Point A;
    private Point B;
    private int dx, dy, w, h, error;

    public Point getNext(Point last) {
        traced = false;
        if (!last.equals(B)) {
            last = predict(last);
        } else traced = true;
        return last;
    }

    public Point getA() {
        return A;
    }

    public Point getB() {
        return B;
    }

    private Point predict(Point next) {
        int x = next.x, y = next.y;

        if (abs(w) > abs(h)) {
            x += dx;
            if (error >= 0) {
                y += dy;
                error -= abs(2 * w);
            }
            error += abs(2 * h);
        } else if (abs(h) > abs(w)) {
            y += dy;
            if (error >= 0) {
                x += dx;
                error -= abs(2 * h);
            }
            error += abs(2 * w);
        } else {
            x = next.x + dx;
            y = next.y + dy;
        }

        next.set(x, y);
        return next;
    }
}
*/
class myLine {
    myLine() {
        A = new Point();
        B = new Point();
        _isEmpty = true;
    }

    myLine(Point start, Point end) {
        A = new Point(start);
        B = new Point(end);
        _init();
    }

    public void setA(Point start) {
        A = start;
        _init();
    }

    public void setB(Point end) {
        B = end;
        _init();
    }

    public void init(Point start, Point end) {
        A = start;
        B = end;
        _init();
    }

    private void _init() {
        _isEmpty = false;
        traced = false;
        w = B.x - A.x;
        h = B.y - A.y;

        if (w < 0) dx = -1;
        else if (w > 0) dx = 1;
        else dx = 0;
        if (h < 0) dy = -1;
        else if (h > 0) dy = 1;
        else dy = 0;

        if (abs(h) >= abs(w)) error = abs(2 * w) - abs(h);
        else if (abs(w) > abs(h)) error = 2 * abs(h) - abs(w);
    }

    public boolean isTraced() {
        return traced;
    }

    public boolean isEmpty() {
        return _isEmpty;
    }

    private boolean _isEmpty;
    private boolean traced;
    private Point A;
    private Point B;
    private int dx, dy, w, h, error;

    public Point getNext(Point last) {
        traced = false;
        if (!last.equals(B)) {
            last = predict(last);
        } else traced = true;
        return last;
    }

    public Point getA() {
        return A;
    }

    public Point getB() {
        return B;
    }

    private Point predict(Point next) {
        int x = next.x, y = next.y;

        if (abs(w) > abs(h)) {
            x += dx;
            if (error >= 0) {
                y += dy;
                error -= abs(2 * w);
            }
            error += abs(2 * h);
        } else if (abs(h) > abs(w)) {
            y += dy;
            if (error >= 0) {
                x += dx;
                error -= abs(2 * h);
            }
            error += abs(2 * w);
        } else {
            x = next.x + dx;
            y = next.y + dy;
        }

        next.set(x, y);
        return next;
    }
}

class physics {
    Point relativeTo(Point Target, Point Origin) {
        Point relativePt = new Point(Origin.x + Target.x, Origin.y + Target.y);
        return relativePt;
    }

    Point relativeToZero(Point Target, Point Origin) {
        Point relativePt = new Point(Target.x - Origin.x, Target.y - Origin.y);
        return relativePt;
    }

    public boolean isWithinBounds(Point point, Point upLeftCorner, Point downRightCorner) {
        if(point.x >= upLeftCorner.x)
            if(point.x <= downRightCorner.x)
                if(point.y >= upLeftCorner.y)
                    if(point.y <= downRightCorner.y)
                        return true;
        return false;
    }
    public boolean isWithinXBounds(int x, int lowerLimit, int upperLimit) {
        if(x >= lowerLimit)
            if(x <= upperLimit)
                return true;
        return false;
    }
    public boolean isWithinYBounds(int y, int lowerLimit, int upperLimit) {
        if(y >= lowerLimit)
            if(y <= upperLimit)
                return true;
        return false;
    }

    double getDistanceBetween(Point point1, Point point2) {
        double x_dist_sq = pow(point1.x - point2.x, 2);
        double y_dist_sq = pow(point1.y - point2.y, 2);
        double dist = sqrt(x_dist_sq + y_dist_sq);
        return dist;
    }

    double getMagnitude(double alongX, double alongY) {
        return sqrt(pow(alongX, 2) + pow(alongY, 2));
    }

    double getMagnitude(Point vector) {
        return sqrt(pow(vector.x, 2) + pow(vector.y, 2));
    }
}

class Entity {
    private String _name;
    private Vector<Bitmap> _bitmaps;
    private int _radius = 1;        //to prevent divide-by-zero errors
    private int _mass = 0;
    private int _state = 1;
    private int _presetState = 1;
    private int _form = 0;
    private int _hasStates[];
    private int _hasForms = 1;
    private Vector<Point[]> _presetGroup;
    private int _presetID = 1;
    private Point _velocity;
    private Point _bounds;
    private Point _origin;
    private Point _pos;
    private Point _target;
    private boolean _collided = false;
    private boolean _accelerating = false;
    private Point _acceleration;
    private int _viewRadius = 0;
    private DIRECTION _viewLeakDir = DIRECTION.NONE;
    public enum DIRECTION {U, UR, R, DR, D, DL, L, UL, NONE}
    private boolean _isAnimatable = false, _isReversible = false;
    private int _animationDelay = 0;
    private STATE_CHANGE_STYLE _scstyle = STATE_CHANGE_STYLE.NONE;
    private boolean isStateReversed = false;
    private boolean _rotateClockwise = true;
    private int _counter = 0;

    Entity() {
    }
    Entity(String entity_name, int entity_mass, int entity_radius, int has_forms, int initial_form) {
        _name = entity_name;
        _radius = entity_radius;
        _mass = entity_mass;
        _velocity = new Point();
        _hasForms = has_forms;
        _hasStates = new int[_hasForms];
        _form = initial_form-1;
        _bitmaps = new Vector<>(1,1);
        _acceleration = new Point(0, 0);
        _viewRadius = _radius;
        _bounds = new Point(_radius, _radius);
    }
    Entity(String entity_name, int entity_mass, Bitmap bitmap, int entity_radius, int xOrigin, int yOrigin) {
        _name = entity_name;
        _radius = entity_radius;
        _mass = entity_mass;
        _bitmaps = new Vector<>(1,1);
        _bitmaps.add(bitmap);
        _velocity = new Point();
        _origin = new Point(xOrigin, yOrigin);
        _acceleration = new Point(0, 0);
        _viewRadius = _radius;
        _bounds = new Point(_radius, _radius);
    }
    Entity(String entity_name, int entity_mass, Bitmap bitmap, int entity_height, int entity_width) {
        _name = entity_name;
        _mass = entity_mass;
        _bounds = new Point(entity_width, entity_height);
        _bitmaps = new Vector<>(1,1);
        _bitmaps.add(bitmap);
        _velocity = new Point();
        _acceleration = new Point(0, 0);
        _viewRadius = entity_width > entity_height ? entity_width : entity_height;
    }
    public String name(){return _name;}

    //Set up animations
    public void setHasStates(int has_states){
        setHasStates(has_states, 1, _form);
    }
    public void setHasStates(int has_states, int initial_state){
        setHasStates(has_states, initial_state, _form);
    }
    public void setHasStates(int has_states, int initial_state, int form){
        if(_hasStates[form]!=has_states) {
            _bitmaps.ensureCapacity(has_states);
            //_bitmaps.trimToSize();
            _hasStates[form] = has_states;
        }
        _state = initial_state;
    }
    public int hasStates(){return _hasStates[_form];}
    public int hasStates(int form){return _hasStates[form];}
    public void setAnimationOptions(boolean isAnimatable, boolean isReversible, int animationDelay, STATE_CHANGE_STYLE style){
        _isAnimatable = isAnimatable;
        _isReversible = isReversible;
        _animationDelay = animationDelay;
        _scstyle = style;
        //TODO animation procedures
    }
    public void setAnimationOptions(boolean isAnimatable, boolean isReversible, int animationDelay, STATE_CHANGE_STYLE style, boolean rotateClockwise){
        _isAnimatable = isAnimatable;
        _isReversible = isReversible;
        _animationDelay = animationDelay;
        _scstyle = style;
        _rotateClockwise = rotateClockwise;
        //TODO animation procedures
    }
    public boolean isAnimatable(){return _isAnimatable;}      //can be made private
    public boolean isReversible(){return _isReversible;}      //can be made private
    public int getAnimationDelay(){return _animationDelay;}
    public void setRotation(boolean rotateClockwise){_rotateClockwise = rotateClockwise;}

    //Running animations
    public int hasCurrentState(){return _state;}
    public boolean changeStateTo(int target_state) {
        return changeStateTo(target_state, _form);
    }
    public boolean changeStateTo(int target_state, int form){
        if(target_state>0 && target_state <= this._hasStates[form]) {
            _state = target_state;
            return true;
        }
        return false;
    }
    public void rotateState(){
        //changes state as per default settings
        rotateStateInForm(_form);
    }
    public void rotateStateInForm(int form){
        //changes state as per default settings
        if(_hasStates[form] != 1 || _scstyle == STATE_CHANGE_STYLE.DIRECTIONAL) {
            if (_isReversible) {
                if (_state == _hasStates[form] && !isStateReversed) isStateReversed = true;
                if (_state == 1 && isStateReversed) isStateReversed = false;
                if (isStateReversed) _state--;
                else _state++;
            }
            else {
                if (_rotateClockwise) _state++;
                else _state--;
                if (_state > _hasStates[form]) _state = 1;
                else if (_state < 1) _state = _hasStates[form];
            }
        }
    }
    public void changeRotation(){_rotateClockwise=!_rotateClockwise;}
    public void animateState(){
        if(_isAnimatable) {
            if (_counter == _animationDelay) {
                this.rotateState();
                _counter = 0;
            }
            _counter++;
        }
    }

    //Set up preset complex animations
    public void setupPresetAnimation(){
        _presetGroup = new Vector<>();
    }
    public void addPresetState(Point[] xFormsYStates){
        _presetGroup.add(xFormsYStates);
    }

    //Running preset complex animations
    public void animatePreset(){
        if(_isAnimatable) {
            if (_counter == _animationDelay) {
                rotatePreset();
                _counter = 0;
            }
            _counter++;
        }
    }
    public void rotatePreset(){
        if (_isReversible) {
            if (_presetState == _presetGroup.get(_presetID).length && !isStateReversed) isStateReversed = true;
            if (_presetState == 1 && isStateReversed) isStateReversed = false;
            if (isStateReversed) _presetState--;
            else _presetState++;
        }
        else {
            if (_rotateClockwise) _presetState++;
            else _presetState--;
            if (_presetState > _presetGroup.get(_presetID).length)
                changeStateTo(_presetGroup.get(_presetID)[0].x,_presetGroup.get(_presetID)[0].y);
            else if (_presetState < 1) changeStateTo(
                    _presetGroup.get(_presetID)[_presetGroup.get(_presetID).length-1].x,
                    _presetGroup.get(_presetID)[_presetGroup.get(_presetID).length-1].y);
        }
    }

    //Manage bitmaps
    public boolean addBitmap(Bitmap bitmap){
        if(_bitmaps.size() == _bitmaps.capacity()) return false;
        _bitmaps.add(bitmap);
        return true;
    }//TODO: implement application for returned value
    public Bitmap bitmap(){
        //this is the only bitmap function which updates current _state of the instance
        //if animatable then updates its current state
        //returns the current state bitmap
        if(_isAnimatable)
            rotateState();
        return _bitmaps.get(_state - 1);
    }
    public Bitmap bitmap(int target_state){
    /*this function is the fail-safe where invalid states are interpreted in three ways
        for objects with no default update style, its value is rounded off to end states
        eg: for objects with 8 states, -3 returns the 1st bmp, & 10 returns the 8th bmp
        */
    /*for objects with default style set to BUILDING or DIRECTIONAL
        the outgoing state is decided by _isReversible
        when _isReversible is true, the state will oscillate between 0 and max state
        when false, the state rotates clockwise or TODO: anticlockwise, decided by _rotateClockwise
        */
    /*errors due to very high or low arguments can be prevented this way,
        but the useless processing cannot. So it is recommended, that a
        maximum state limit is set for all loops deciding the next state
        */
        if(_scstyle == STATE_CHANGE_STYLE.NONE){
            if(target_state < 1 || target_state > _hasStates[_form])
                target_state = (target_state<1?1:_hasStates[_form]);                  //if invalid, set to limits
        }
        else if(_scstyle == STATE_CHANGE_STYLE.BUILDING || _scstyle == STATE_CHANGE_STYLE.DIRECTIONAL){
            if (isReversible()) target_state = (target_state<1?1:_hasStates[_form]);   //set to next oscillated state
            else if(target_state<1) target_state = 8;
            else if(target_state>8) target_state += 1 + target_state%8;                                              //set to next rotated state
            return bitmap(target_state, DIRECTION.NONE);                        //direction will be neglected
        }
        else if(target_state > _hasStates[_form]) target_state %= _hasStates[_form]; //set to next rotated state
        if(target_state<1)target_state=1;//TODO: something missing
        return _bitmaps.get(target_state-1);
    }
    public Bitmap bitmap(int target_state, int target_form){
    /*this function is the fail-safe where invalid states are interpreted in three ways
        for objects with no default update style, its value is rounded off to end states
        eg: for objects with 8 states, -3 returns the 1st bmp, & 10 returns the 8th bmp
        */
    /*for objects with default style set to BUILDING or DIRECTIONAL
        the outgoing state is decided by _isReversible
        when _isReversible is true, the state will oscillate between 0 and max state
        when false, the state rotates clockwise or TODO: anticlockwise, decided by _rotateClockwise
        */
    /*errors due to very high or low arguments can be prevented this way,
        but the useless processing cannot. So it is recommended, that a
        maximum state limit is set for all loops deciding the next state
        */
        if(_scstyle == STATE_CHANGE_STYLE.NONE){
            if(target_state < 1 || target_state > _hasStates[target_form])
                target_state = (target_state<1?1:_hasStates[target_form]);                  //if invalid, set to limits
        }
        else if(_scstyle == STATE_CHANGE_STYLE.BUILDING || _scstyle == STATE_CHANGE_STYLE.DIRECTIONAL){
            if (isReversible()) target_state = (target_state<1?1:_hasStates[target_form]);   //set to next oscillated state
            else if(target_state<1) target_state = 8;
            else if(target_state>8) target_state += 1 + target_state%8;                                              //set to next rotated state
            return bitmap(target_state, DIRECTION.NONE);                        //direction will be neglected
        }
        else if(target_state > _hasStates[target_form]) target_state %= _hasStates[target_form]; //set to next rotated state
        if(target_state<1)target_state=1;//TODO: something missing
        return _bitmaps.get(target_state-1);
    }
    public Bitmap bitmap(int target_state, DIRECTION dir){
        //returns the required state, rotated to given direction
        //for objects that should behave like buildings, input direction is neglected
        //the 8 states are minimized to 2 states & 8 directions, rotated by 90d
        //for others, the given state is returned rotated using bitmap(state,angle) by 45d;
        float angle;
        if(dir == DIRECTION.NONE) {
            if(_scstyle == STATE_CHANGE_STYLE.BUILDING) {
                if (((float) target_state) % 2 == 0) {
                    angle = (target_state / 2 - 1) * 90;
                    target_state = 2;   //corners
                } else {
                    angle = (target_state - 1) / 2 * 90;
                    target_state = 1;  //sides
                }
            }
            else {
                angle = (target_state-1)*45;
                target_state = 1;
            }
        }
        else angle = (dir.ordinal()-1)*45;  //where direction matters

        return bitmap(target_state, angle);
    }
    public Bitmap bitmap(int target_state, float angle){
        //returns the required state bitmap, rotated by given angle
        //for invalid states, the fail-safe is returned
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap temp = _bitmaps.get(target_state-1);
        if(target_state>0 && target_state <= _hasStates[_form]) {
            return Bitmap.createBitmap(temp,0,0,temp.getWidth(),temp.getHeight(),matrix,true);
        }
        return bitmap(target_state);    //fail-safe
    }

    //Check if a point lies within this object's boundaries, a YES means a collision has occurred
    public boolean collided(){return _collided;}
    public void setCollided(boolean collided){_collided = collided;}
    public boolean isWithinBounds(Point upLeftCorner, Point downRightCorner) {
        if(_pos.x+width()/2 >= upLeftCorner.x)
            if(_pos.x-width()/2 <= downRightCorner.x)
                if(_pos.y+height()/2 >= upLeftCorner.y)
                    if(_pos.y-height()/2 <= downRightCorner.y)
                        return true;
        return false;
    }
    public boolean isWithinXBounds(int lowerLimit, int upperLimit) {
        if(_pos.x+width()/2 >= lowerLimit)
            if(_pos.x-width()/2 <= upperLimit)
                return true;
        return false;
    }
    public boolean isWithinYBounds(int lowerLimit, int upperLimit) {
        if(_pos.y+height()/2 >= lowerLimit)
            if(_pos.y-height()/2 <= upperLimit)
                return true;
        return false;
    }

    //Manage Dimensions and Positional Vectors
    public Point origin(){return _origin;}
    public void setOrigin(Point origin){if(_origin == null) _origin = new Point();
        _origin.set(origin.x, _origin.y);}
    public void setOrigin(int x, int y){if(_origin == null) _origin = new Point();
        _origin.set(x, y);}
    public Point pos(){
        if (_pos == null)
            return _origin;
        else
            return _pos;
    }
    public void setPos(Point pos) {
        if (_pos == null) _pos = new Point();
        _pos.set(pos.x, pos.y);
    }
    public void setPos(int x, int y) {
        if (_pos == null) _pos = new Point();
        _pos.set(x, y);
    }
    public int radius(){return _radius;}
    public int height(){return _bounds.y;}
    public int width(){return _bounds.x;}
    public void resize(int x, int y){
        _bounds.x=x;
        _bounds.y=y;
        for(int i=0;i<_bitmaps.size();i++){
            _bitmaps.set(i, Bitmap.createScaledBitmap(_bitmaps.get(i), x, y, true));
        }
    }
    public int viewRadius(){return _viewRadius;}
    public void setViewRadius(int viewRadius){_viewRadius = viewRadius;}

    public DIRECTION viewLeakDir(){return _viewLeakDir;}
    public void setViewLeakDir(DIRECTION leakDirection){_viewLeakDir = leakDirection;}

    //Manage Auto-Movement
    public Point target() {
        if (_target == null)
            if (_pos == null)
                return _origin;
            else
                return _pos;
        else
            return _target;
    }
    public void setTarget(Point pos) {
        if (_target == null)
            _target = new Point();
        _target.set(pos.x, pos.y);
    }
    public void setTarget(int x, int y) {
        if (_target == null)
            _target = new Point();
        _target.set(x, y);
    }

    //Manage Velocity And Acceleration
    public void accelerate() {
        _velocity.x += _acceleration.x;
        _velocity.y += _acceleration.y;
    }
    public void setAcceleration(int xAcceleration, int yAcceleration, boolean active) {
        _accelerating = active;
        _acceleration.set(xAcceleration, yAcceleration);
    }
    public void setAcceleration(boolean active) {_accelerating = active;}
    public void setVelocity(int xVelocity, int yVelocity){_velocity.set(xVelocity, yVelocity);}
    public Point velocity(){return _velocity;}
    public boolean isAccelerating(){return _accelerating;}

}

class gmtry{
    public static int wrtCircle(Point p, Point origin, Point radius/*out 1, eq 0, in -1*/){
        return wrtCircle(p.x, p.y, origin, radius);
    }
    public static int wrtCircle(int x, int y, Point origin, Point radius/*out 1, eq 0, in -1*/){
        int a = x-origin.x;
        int b = y-origin.y;
        a = a*a + b*b;
        b = radius.x*radius.y;
        if(a>b) return 1;
        else if (a==b) return 0;
        else return -1;
    }
    public static Point getCircleBisector(Point p, Point origin, Point radius){
        return getCircleBisector(p.x, p.y, origin, radius);
    }
    public static Point getCircleBisector(int x, int y, Point origin, Point radius){
        double m = sqrt((x - origin.x)*(x - origin.x) + (y - origin.y)*(y - origin.y));
        Point p = new Point((int)(origin.x + radius.x*(x-origin.x)/m),(int)(origin.y + radius.y*(y-origin.y)/m));
        return p;
    }
}