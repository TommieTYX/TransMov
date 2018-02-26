package mhci.transmov;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

public class LoadActivity extends AppCompatActivity {
    Handler handler = new Handler();
    ProgressBar progressBar;
    boolean stop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        progressBar = (ProgressBar) findViewById(R.id.LoadBar);

        handler.post(runnableCode);
    }

    private Runnable runnableCode = new Runnable() {
        int progress = 0;

        @Override
        public void run() {
            Log.i("TAG","still loading runnable");
            try {
                if (progress == 90) {
                    progress += 10;
                    progressBar.setProgress(progress);
                    stop = true;
                    Intent i = new Intent(LoadActivity.this,
                            MainActivity.class);
                    startActivity(i);
                    finish();
                } else {
                    progress += 30;
                    progressBar.setProgress(progress);
                }
            } finally {
                if(!stop) {
                    handler.postDelayed(this, 1000);
                }
            }
        }
    };
}
