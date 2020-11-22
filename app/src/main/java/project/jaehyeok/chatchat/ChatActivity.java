package project.jaehyeok.chatchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 채팅방에 들어왔을때 DB에 추가

        Intent getIntent = getIntent();
        String data = getIntent.getStringExtra("데이터");

        Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
    }
}