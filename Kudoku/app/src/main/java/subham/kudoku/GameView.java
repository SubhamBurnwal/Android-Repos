package subham.kudoku;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class GameView extends SurfaceView implements Runnable {
    private AttributeSet _attrs;
    private SurfaceHolder mSurfaceHolder;

    private boolean mRunning;
    private Thread mGameThread;

    private level l;

    public GameView(Context context) {
        super(context);
    }
    public GameView(Context context, AttributeSet attrs){
        super(context,attrs);
        _attrs = attrs;
    }
    public GameView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context,attrs, defStyleAttr);
        _attrs = attrs;
    }

    @Override
    public void run(){
        while (mRunning) {
            lDraw();
        }
    }
    public void lDraw() {
        Canvas canvas;
        if (mSurfaceHolder.getSurface().isValid()) {
            canvas = mSurfaceHolder.lockCanvas();
            canvas.save();
            l.draw(canvas);
            canvas.restore();
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
    public void pause(){
        mRunning = false;
        try{
            mGameThread.join();
        }catch (InterruptedException e){
            e.fillInStackTrace();
        }
    }
    public void resume(){
        mRunning = true;
        mGameThread = new Thread(this);
        mGameThread.start();
    }
    public void init(){
        setWillNotDraw(false);
        mSurfaceHolder = getHolder();

        //select a theme
        int themeID = 0;

        //create a level
        l = new level(0, themeID, getContext());

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        l.initMetrics(displayMetrics.widthPixels,displayMetrics.heightPixels);
    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        int x, y;
        switch(motionEvent.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                x = (int)motionEvent.getX();
                y = (int)motionEvent.getY();
                interpretTouch(x, y);
                break;
            default: break;
        }
        return true;
    }
    private void interpretTouch(int x, int y){
        if(x<9.1*l.cell_size && y<9.1*l.cell_size){
            x /= l.cell_size;
            y /= l.cell_size;
            l.update(l.convert(y, x));
        }
        else{
            if(l!=null)
                if(l.theme == 2) l.theme = 0;
                else l.theme++;
        }
        invalidate();
    }
}
