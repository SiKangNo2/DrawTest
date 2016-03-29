package sikang.drawtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private DrawView mDrawView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawView= (DrawView) this.findViewById(R.id.mDrawView);

    }

    @Override
    protected void onDestroy() {
        mDrawView.onDestory();
        super.onDestroy();
    }
}
