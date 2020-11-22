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

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

public class CreateChatActivity extends AppCompatActivity {

    private EditText inputChatTitle;
    private EditText inputChatPersonnel;
    private Button createChatButton;

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference;

    private String masterUid;

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
        rootReference = firebaseDatabase.getReference();

        // 이전 (ChatListActivity) 에서 Intent 통해 전달한 전달받은 uid
        Intent getIntent = getIntent();
        masterUid = getIntent.getStringExtra("uid");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 채팅 생성완료 버튼 눌렀을때
        createChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 파이어베이스 데이터베이스에 새로운 채팅방의 데이터가 추가될때
                // 해당 채팅방에 대한 참가멤버, 메세지에 대한 데이터도 함께 생성한다
                // 이 때 채팅방이 가지는 key 값과 같은 key 값을 부여함으로써, 관련성을 확인 할 수 있도록 한다

                // 채팅데이터 추가 및 key 값 반환받기
                String newChatReferenceKey = addDataPathChats();
                // 참가멤버, 메세지 데이터 생성(같은 key 값을 가지도록)
                addDataPathMembers(newChatReferenceKey);
                addDataPathMessages(newChatReferenceKey);

                setResult(RESULT_OK);
                finish();
            }
        });
    }

    // 파이어베이스 realtime database (path: chats) 에 새로운 채팅방의 데이터를 추가한다
    // 이때 데이터에 랜덤으로 부여되는 key(String)를 반환한다
    // addDataPathMembers, addDataPathMessages 메소드의 인자로 사용하기 위해서
    private String addDataPathChats() {
        // DB 경로 chats 진입
        DatabaseReference chatsReference = rootReference.child("chats");

        // Chat 객체에 들어갈 생성자 파라미터  = (만드는 계정 uid, 제목, 최대참가인원)
        String title = inputChatTitle.getText().toString().trim(); // 제목
        int personnel = Integer.parseInt(inputChatPersonnel.getText().toString().trim()); // 정원
        Chat newChat = new Chat(masterUid, title, personnel);

        // 파이어베이스 데이터베이스에서 새 채팅정보가 저장될 경로에 접근(push)하여
        // 해당 위치에 Chat 객체와 데이터를 가지는 채팅데이터 추가(setValue)
        DatabaseReference newChatReference = chatsReference.push();
        newChatReference.setValue(newChat);

        // 멤버, 메세지 데이터를 생성하는 메소드에서 같은 key 값을 사용할 수 있도록 반환한다
        String newChatReferenceKey = newChatReference.getKey();
        return newChatReferenceKey;
    }


    // 파이어베이스 realtime database (path: members) 에 새로운 채팅참가인원 데이터를 추가한다
    // 이때 인자로 입력받은 값을 추가할 데이터를 식별하기 위한 key 값으로 설정한다
    private void addDataPathMembers(String key) {
        // DB 경로 members 진입
        DatabaseReference membersReference = rootReference.child("members");
        // 인자로 받은 값을 key 값이 되도록 지정 후 데이터 추가
        // 채팅방 최초 개설 / 참가인원으로 채팅방 마스터를 추가한다
        Map<String, String> masterMap = new HashMap<>();
        masterMap.put(masterUid, "master");
        membersReference.child(key).setValue(masterMap);
    }


    // 파이어베이스 realtime database (path: messages) 에 새로운 채팅메세지 데이터를 추가한다
    // 이때 인자로 입력받은 값을 추가할 데이터를 식별하기 위한 key 값으로 설정한다
    private void addDataPathMessages(String key) {
        // DB 경로 messages 진입
        DatabaseReference messagesReference = rootReference.child("messages");
        // 인자로 받은 값을 key 값이 되도록 지정 후 데이터 추가
        // 채팅방 최초 개설에 대한 전체 메세지를 추가한다
        String chatOpenMessage = "채팅방이 생성되었습니다!";
        Message chatOpenBroadcast = new Message(true, chatOpenMessage);
        messagesReference.child(key).push().setValue(chatOpenBroadcast);
    }
}