package subham.unprodev.asiantales;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
    private View decorView;

    //private SurfaceView sview1;
    //private SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        decorView = getWindow().getDecorView();

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        setContentView(R.layout.activity_main);
        //sview1 = new SurfaceView(getApplicationContext());
        //sview1 = findViewById(R.id.sview1);
        //surfaceHolder = sview1.getHolder();
        hideFirstMenu();
        hideInfoPage();
    }
    protected void onResume(){
        super.onResume();
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        /*if (surfaceHolder.getSurface().isValid()) {
            Canvas c = surfaceHolder.lockCanvas();
            Paint p = new Paint();
            p.setColor(0xFF00FFFF);
            if(c!=null) {
                c.save();
                c.drawColor(0xFF00FFFF);
                c.restore();
                surfaceHolder.unlockCanvasAndPost(c);
            }
        }*/
    }

    public void zoomIn(View v, int t){
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(v,"scaleX", 2.0f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(v,"scaleY", 2.0f);
        scaleUpX.setDuration(t);
        scaleUpY.setDuration(t);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.play(scaleUpX).with(scaleUpY);
        scaleUp.start();
    }
    public void zoomOut(final View v, int t){
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(v,"scaleX", 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(v,"scaleY", 1.0f);
        scaleDownX.setDuration(t);
        scaleDownY.setDuration(t);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY);
        scaleDown.start();
        scaleDown.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }
            @Override
            public void onAnimationEnd(Animator animation) {
                onClickSimpleButtonAction(v);
            }
            @Override
            public void onAnimationCancel(Animator animation) {

            }
            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onClickSimpleButton(View v){
        Button btn = (Button)v;
        //resetBtn();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomIn(v, 100);
                zoomOut(v, 100);
            }
        });
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_DOWN:
                        zoomIn(v, 300);
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_UP:
                        zoomOut(v, 300);
                        break;
                }
                return true;
            }
        });
    }
    public void showFirstPage() {
        LinearLayout firstpage = findViewById(R.id.firstpage);
        firstpage.setVisibility(View.VISIBLE);
    }
    public void hideFirstPage(){
        LinearLayout firstpage = findViewById(R.id.firstpage);
        firstpage.setVisibility(View.GONE);
    }
    public void showFirstMenu() {
        FrameLayout firstmenu = findViewById(R.id.firstmenu);
        firstmenu.setVisibility(View.VISIBLE);
    }
    public void hideFirstMenu(){
        FrameLayout firstmenu = findViewById(R.id.firstmenu);
        firstmenu.setVisibility(View.GONE);
    }
    public void showInfoPage() {
        LinearLayout infopage = findViewById(R.id.infopage);
        infopage.setVisibility(View.VISIBLE);
    }
    public void hideInfoPage(){
        LinearLayout infopage = findViewById(R.id.infopage);
        infopage.setVisibility(View.GONE);
    }
    public void onClickSimpleButtonAction(View v){
        switch(v.getId()){
            case R.id.appname: hideFirstPage(); showFirstMenu(); break;
            case R.id.back_btn:
                if(findViewById(R.id.characterPage).getVisibility() == View.VISIBLE) {
                LinearLayout temp = findViewById(R.id.menupage);
                temp.setOrientation(LinearLayout.VERTICAL);
                temp.setVerticalGravity(Gravity.CENTER_VERTICAL);
                temp.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
                for(int i=0;i<temp.getChildCount();i++) {
                    if(i == 1 || i == 3)
                        temp.getChildAt(i).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT,2f));
                    else{
                        temp.getChildAt(i).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT,(i==2?1.95f:1.3f)));
                        temp.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                }
                    ((ImageButton)findViewById(R.id.play_btn)).setScaleType(ImageView.ScaleType.FIT_CENTER);
                temp.requestLayout();
                findViewById(R.id.characterPage).setVisibility(View.GONE);
            }
            else {
                hideFirstMenu();
                showFirstPage();
            }
            break;
            case R.id.info_button: hideFirstMenu(); showInfoPage(); break;
            case R.id.closeinfopage: hideInfoPage(); showFirstMenu(); break;
            case R.id.gallery_btn: break;
            case R.id.play_btn:
                LinearLayout temp = findViewById(R.id.menupage);
                temp.setOrientation(LinearLayout.HORIZONTAL);
                temp.setVerticalGravity(Gravity.TOP);
                temp.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
                for(int i=0;i<temp.getChildCount();i++) {
                    if(i == 1 || i == 3)
                        temp.getChildAt(i).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
                    else {
                        temp.getChildAt(i).setVisibility(View.GONE);
                    }
                }
                temp.requestLayout();
                temp = findViewById(R.id.characterPage);
                temp.setVisibility(View.VISIBLE);
                findViewById(R.id.characterImage).setOnTouchListener(new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                startActivity(new Intent(getApplicationContext(), GameActivity.class));
                break;
        }
        return true;
    }
    });
                break;
            default: break;
            //TODO: complete basic UI!
        }
    }

    public static void setMarginsVertical(View view, int val){
        if(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.setMargins(0, val, 0, val);
            view.requestLayout();
        }
    }
}