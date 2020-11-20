package project.jaehyeok.chatchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import javax.xml.transform.Result;

public class CreateChatActivity extends AppCompatActivity {

    private EditText inputChatTitle;
    private EditText inputChatPersonnel;
    private Button createChatButton;

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference chatsReference; // 데이터베이스경로 (path : chats)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        inputChatTitle = findViewById(R.id.inputCreateChatTitle);
        inputChatPersonnel = findViewById(R.id.inputCreateChatPersonnel);
        createChatButton = findViewById(R.id.createChatButton);

        // 파이어베이스 접근 권한 갖기
        firebaseAuth = FirebaseAuth.getInstance();
        // 파이어베이스 realtime database 접근 설정
        firebaseDatabase = FirebaseDatabase.getInstance();
        chatsReference = firebaseDatabase.getReference("chats");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 채팅 생성완료 버튼 눌렀을때
        createChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // DB에 채팅데이터 생성 후 이전 액티비티로 돌아가기
                addNewChatToDB();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void addNewChatToDB() {
        // Chat 객체에 들어갈 생성자 파라미터  = (만드는 계정 uid, 생성시간, 제목, 최대참가인원)
        // 파이어베이스 리얼타임 데이터베이스의 path: chats 에 데이터 생성

        // 이전 (ChatListActivity) 에서 Intent를 통해 전달한 전달받은 uid
        Intent getIntent = getIntent();
        String masterUid = getIntent.getStringExtra("uid");

        long createdAt = System.currentTimeMillis(); // 생성시간
        String title = inputChatTitle.getText().toString().trim(); // 제목
        int personnel = Integer.parseInt(inputChatPersonnel.getText().toString().trim()); // 정원

        Chat newChat = new Chat(masterUid, createdAt, title, personnel);
        chatsReference.push().setValue(newChat);
    }
}