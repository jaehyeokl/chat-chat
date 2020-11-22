package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private EditText inputChatMessage;
    private Button sendMessageButton;

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference;

    private String uid;
    private String databaseChatKey;

    private RecyclerView chatMessageRecyclerview;
    private RecyclerChatMessageAdapter chatMessageAdapter;

    private ArrayList<Message> chatMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inputChatMessage = findViewById(R.id.inputChatMessage);
        sendMessageButton = findViewById(R.id.sendMessageButton);

        // 파이어베이스 접근 권한 갖기
        firebaseAuth = FirebaseAuth.getInstance();
        // 파이어베이스 realtime database 접근 설정
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootReference = firebaseDatabase.getReference();

        // uid 구하기
        ArrayList<String> userProfile = getCurrentUserProfile();
        uid = userProfile.get(1);

        // 채팅목록을 터치해서 채팅방에 들어왔을때
        // 데이터베이스에서 현재 채팅에 대한 메세지, 멤버에 접근하기 위한 채팅의 key 값
        Intent getIntent = getIntent();
        databaseChatKey = getIntent.getStringExtra("ChatKey");

        // 메세지 목록 리사이클러뷰
        chatMessages = new ArrayList<>();
        // 채팅방 처음 참가했을때, 채팅 내역 불러와 목록에 보여주기
//        rootReference.child("messages").child(databaseChatKey).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot messageSnapShot: snapshot.getChildren()) {
//                    // 리사이클러뷰의 데이터로 저장하기 위해 각 메세지를 메세지 객체로 변환하여 리스트에 저장한다
//                    Message message = messageSnapShot.getValue(Message.class);
//                    chatMessages.add(message);
//                    //System.out.println(message.getMessage());
//                }
//
//                // 리사이클러뷰 리스트 적용했으니 데이터 새로고침만 해주기
//                chatMessageRecyclerview = findViewById(R.id.chatMessageRecyclerview);
//                chatMessageAdapter = new RecyclerChatMessageAdapter(chatMessages);
//                chatMessageRecyclerview.setAdapter(chatMessageAdapter);
//                chatMessageRecyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });

        // 새로운 채팅이 추가됐을때
        rootReference.child("messages").child(databaseChatKey).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                System.out.println("/////////////////add : " + snapshot);
                Message message = snapshot.getValue(Message.class);
                chatMessages.add(message);

                chatMessageRecyclerview = findViewById(R.id.chatMessageRecyclerview);
                chatMessageAdapter = new RecyclerChatMessageAdapter(chatMessages);
                chatMessageRecyclerview.setAdapter(chatMessageAdapter);
                chatMessageRecyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                System.out.println("////////////////Change");
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        // Chats 에서 채팅 타이틀 가져오기
//        rootReference.child("chats");

        // Members 에서 최대 인원 가져오기
//        rootReference.child("members");

        // Messages 에서 채팅 내역 올리기
        // messages 의 데이터 변경 이벤트 수신









    }

    @Override
    protected void onResume() {
        super.onResume();

        // 메세지 전송 눌렀을때
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Message 객체 생성 후 데이터베이스 경로 messages 에 추가
                // Message 객체 생성 시 인자로 유저 uid 와 입력한 내용을 넣어준다
                String inputMessage = inputChatMessage.getText().toString().trim();
                DatabaseReference messagesReference = rootReference.child("messages");
                Message chatOpenBroadcast = new Message(uid, inputMessage);
                messagesReference.child(databaseChatKey).push().setValue(chatOpenBroadcast);

                inputChatMessage.setText(null);
            }
        });
    }

    // 현재 로그인한 계정의 프로필을 리스트에 저장하여 반환한다
    // ex) [로그인제공업체, uid, name, email]
    private ArrayList<String> getCurrentUserProfile() {
        ArrayList<String> userProfile = new ArrayList<>();
        // 현재 로그인한 사용자 프로필 가져오기
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // 로그인방식의 경우가 여러가지이기 때문에 (일반 이메일, 구글계정)
            // 반복문을 통해 현재 계정과 연결된 로그인제공 업체를 찾고 프로필을 가져온다
            for (UserInfo profile : user.getProviderData()) {
                // 로그인 계정 제공업체 (ex: google.com)
                String providerId = profile.getProviderId();
                // UID 각 계정마다 부여되는 고유 값
                String uid = profile.getUid();
                String name = profile.getDisplayName();
                String email = profile.getEmail();
                //Uri photoUrl = profile.getPhotoUrl();
                userProfile.add(providerId);
                userProfile.add(uid);
                userProfile.add(name);
                userProfile.add(email);
            }
        } else {
            // 로그인 하지 않았음
        }
        return userProfile;
    }
}