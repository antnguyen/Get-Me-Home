package cs117.getmehome;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class Direction extends AppCompatActivity {
    TextView direction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);

        direction = (TextView) findViewById(R.id.textView);

    }
}
