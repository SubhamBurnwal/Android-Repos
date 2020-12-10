package subham.kudoku;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class MainActivity extends Activity {

    private GameView mGameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.levelview);
        mGameView = findViewById(R.id.gameView);
    }
    @Override
    protected void onPause(){
        super.onPause();
        mGameView.pause();
    }
    @Override
    protected void onResume(){
        super.onResume();
        mGameView.resume();
    }
}
