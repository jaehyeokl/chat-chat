package project.jaehyeok.chatchat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class UserProfileActivity extends AppCompatActivity {

    private Group touchChangeBackground;

    private TextView name;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        touchChangeBackground = findViewById(R.id.touchChangeBackground);

//        int touchChangeBackgroundIds[] = touchChangeBackground.getReferencedIds();
//        for (int id : touchChangeBackgroundIds) {
//            findViewById(id).setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View view, MotionEvent motionEvent) {
//
//                    view.setBackgroundColor(Color.BLACK);
//
//                    return true;
//                }
//            });
//        }

    }
}