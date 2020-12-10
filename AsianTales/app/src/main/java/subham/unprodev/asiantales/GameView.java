package subham.unprodev.asiantales;

import android.content.Context;
import android.graphics.*;
import android.view.*;

public class GameView extends SurfaceView implements Runnable{
    private Thread gameThread = null;
    volatile boolean playing;
    private SurfaceHolder surfaceHolder;
    private Point MAX;
    private level l;
    public GameView(Context context, Point screenResolution) {
        super(context);
        MAX = new Point(screenResolution);
        surfaceHolder = getHolder();
        l = new level(context,1);
    }

    @Override
    public void run() {
        while (playing) {
            draw();
            control(10);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        //TODO: Now that you understand how events work, repair this asap
        int x, y, action=1;
        switch(motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                x = (int) motionEvent.getX();
                y = (int) motionEvent.getY();
                action = 1;
                if(l.ready) l.processInput(new Point(x,y),action);
                break;
            case MotionEvent.ACTION_MOVE:
                action = 2;
                break;
            case MotionEvent.ACTION_DOWN:
                action = 3;
                break;
        }
        return true;
    }

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            l.c = surfaceHolder.lockCanvas();
            if(l.c!=null) {
                l.c.save();
                l.draw();
                l.c.restore();
                surfaceHolder.unlockCanvasAndPost(l.c);
            }
        }
    }
    public void pause() {
        playing = false;
        try {
            gameThread.join();  //stopping the thread actually
        } catch (InterruptedException e) {
            e.fillInStackTrace();
        }
    }
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
    private void control(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
