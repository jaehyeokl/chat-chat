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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
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
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private TextView chatRoomTitle;
    private TextView chatRoomCurrentCount;
    private TextView chatRoomPersonnel;
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

        chatRoomTitle = findViewById(R.id.chatRoomTitle);
        chatRoomCurrentCount = findViewById(R.id.chatRoomCurrentCount);
        chatRoomPersonnel = findViewById(R.id.chatRoomPersonnel);
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

        // 데이터베이스 chats 에서 데이터 가져와 TextView 에 초기화하기
        // 가져온 데이터의 채팅방 정원을 확인하여 현재 유저가 참가할 수 있는지 체크한다
        rootReference.child("chats").child(databaseChatKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Chat chat = snapshot.getValue(Chat.class);
                String title = chat.getTitle();
                final String masterUid = chat.getMasterUid();
                final int personnel = chat.getPersonnel();

                // 채팅방 타이틀 최대인원 초기화
                chatRoomTitle.setText(title);
                chatRoomPersonnel.setText(personnel + "");

                // 1차적으로 채팅 목록에서 최대인원일때 들어올 수 없도록 하되, 리사이클러뷰에서 반영되지 않은 상태에서 유저가 들어올 여지가 있음
                // 2차 체크 -> 현재 채팅방 참가인원과 최대 참가인원(personnel)을 비교하여 유저의 채팅방 참가 여부를 결정한다
                // 입장 가능할때, members 에 유저 참가 데이터를 생성하고, 유저 참가 메세지를 broadcast 되도록 한다
                rootReference.child("members").child(databaseChatKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    // 데이터베이스 members 에서 현재 채팅방 참가인원 불러오기
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long chatMemberCount = (int) snapshot.getChildrenCount();

                        if (uid.equals(masterUid)) {
                            // 마스터는 인원 상관없이 입장
                        } else {
                            // 마스터 아닐때
                            if (chatMemberCount < personnel) {
                                // 현재 인원 textView 초기화
                                chatRoomCurrentCount.setText(chatMemberCount + "");
                                // 참가인원이 정원보다 적을때 유저 채팅방 참가 및 DB 추가
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put(uid, true);
                                rootReference.child("members").child(databaseChatKey).updateChildren(userMap);
                            } else {
                                finish();
                                Toast.makeText(ChatActivity.this, "채팅방 인원이 초과되었습니다", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // 채팅방의 메세지를 보여주는 리사이클러뷰
        // addChildEventListener 을 통해 가장 첫 이벤트로 전체 채팅 데이터를 불러온다
        // 이후 해당 채팅방에서 메세지가 추가될 때 마다 발생하는 이벤트를 통해 추가되는 메세지를 보여준다
        chatMessages = new ArrayList<>(); // 리사이클러뷰 데이터

        rootReference.child("messages").child(databaseChatKey).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //System.out.println("/////////////////add : " + snapshot);
                // 데이터베이스에서 리스너를 통해 데이터를 제공하는 방식 DataSnapshot 을 객체로 변환하기
                Message message = snapshot.getValue(Message.class);
                chatMessages.add(message);

                chatMessageRecyclerview = findViewById(R.id.chatMessageRecyclerview);
                chatMessageAdapter = new RecyclerChatMessageAdapter(chatMessages, uid);
                chatMessageRecyclerview.setAdapter(chatMessageAdapter);
                chatMessageRecyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                // 채팅 추가될 때 마다 추가된 메세지로 화면 이동 (리사이클러뷰 가장 마지막 아이템 보여주기)
                chatMessageRecyclerview.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 메세지 전송 눌렀을때
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Message(uid, 작성자이름, 입력내용) 객체 데이터베이스 경로 messages 에 추가
                final String inputMessage = inputChatMessage.getText().toString().trim();

                // 인자에 들어갈 작성자 이름을 users 데이터베이스에서 구한다음 Message 객체 생성한다
                rootReference.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserData userData = snapshot.getValue(UserData.class);
                        String userName = userData.getName();
                        Message chatMessage = new Message(uid, userName, inputMessage);

                        // 유저 닉네임이 설정되어 있지 않을때
                        // 채팅방에서 메세지작성자로 이메일을 표시한다
                        if (userName == null) {
                            String userEmail = userData.getEmail();
                            chatMessage.setShowName(userEmail);
                        }

                        DatabaseReference messagesReference = rootReference.child("messages");
                        messagesReference.child(databaseChatKey).push().setValue(chatMessage);

                        inputChatMessage.setText(null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
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