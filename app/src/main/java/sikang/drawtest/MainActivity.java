package sikang.drawtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private DrawView mDrawView;
    private Button mButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawView= (DrawView) this.findViewById(R.id.mDrawView);
        mButton= (Button) this.findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawView.changeLayout();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mDrawView.onDestory();
        super.onDestroy();
    }
}
