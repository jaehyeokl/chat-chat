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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import project.jaehyeok.chatchat.data.Chat;
import project.jaehyeok.chatchat.data.Message;
import project.jaehyeok.chatchat.data.UserData;

public class ChatActivity extends AppCompatActivity {

    private Button chatRoomBackButton;
    private TextView chatRoomTitle;
    private TextView chatRoomCurrentCount;
    private TextView chatRoomPersonnel;
    private ImageView chatRoomThumbButton;
    private EditText inputChatMessage;
    private Button sendMessageButton;

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference;

    private String uid;
    private String databaseChatKey;
    private String masterUid;

    private RecyclerView chatMessageRecyclerview;
    private RecyclerChatMessageAdapter chatMessageAdapter;

    private ArrayList<Message> chatMessages;

    private boolean userThumbChatState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRoomBackButton = findViewById(R.id.chatRoomBackButton);
        chatRoomTitle = findViewById(R.id.chatRoomTitle);
        chatRoomCurrentCount = findViewById(R.id.chatRoomCurrentCount);
        chatRoomPersonnel = findViewById(R.id.chatRoomPersonnel);
        chatRoomThumbButton = findViewById(R.id.chatRoomThumbButton);
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

        // 데이터베이스 members 조회하여 현재 채팅방 참가인원 초기화 및
        // 인원 변경내용 반영하기
        rootReference.child("members").child(databaseChatKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentUserCount = (int) snapshot.getChildrenCount();
                chatRoomCurrentCount.setText(currentUserCount + "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // 데이터베이스 chats 에서 데이터 가져와 TextView 에 초기화하기
        // 가져온 데이터의 채팅방 정원을 확인하여 현재 유저가 참가할 수 있는지 체크한다
        rootReference.child("chats").child(databaseChatKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Chat chat = snapshot.getValue(Chat.class);
                String title = chat.getTitle();
                masterUid = chat.getMasterUid();
                final int personnel = chat.getPersonnel();

                // 채팅방 타이틀 최대인원 초기화
                chatRoomTitle.setText(title);
                chatRoomPersonnel.setText(personnel + "");

                // 1차적으로 참가인원이 정원일때 채팅목록에서 참가할 수 없도록 하지만, 제한인원까지 참가 하기 전
                // 목록에 아이템이 초기화 되고 이후에 정원이 찼을때는 ChatActivity 에서 2차적으로 처리할 수 있도록 한다
                // 2차 체크 -> 현재 채팅방 참가인원과 최대 참가인원(personnel)을 비교하여 유저의 채팅방 참가 여부를 결정한다
                // 입장 가능할때, members 에 유저 참가 데이터를 생성하고, 유저 참가 메세지를 broadcast 되도록 한다
                rootReference.child("members").child(databaseChatKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    // 데이터베이스 members 에서 현재 채팅방 참가인원 불러오기
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long chatMemberCount = (int) snapshot.getChildrenCount();

                        if (!uid.equals(masterUid)) {
                            // 마스터 아닐때
                            if (chatMemberCount < personnel) {
                                // 참가인원이 정원보다 적을때 유저 채팅방 참가 및 DB 추가
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put(uid, true);
                                rootReference.child("members").child(databaseChatKey).updateChildren(userMap);

                                // 유저 입장 방송메세지 안내하기
                                addBroadcastMessageUserAttend(true);
                            } else {
                                finish();
                                Toast.makeText(ChatActivity.this, "채팅방 인원이 초과되었습니다", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // 마스터는 인원 상관없이 입장
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

        // 채팅방 안에서 유저가 좋아요를 누른 채팅방일때는 상단의 하트버튼이 꽉차도록 보이게 한다
        rootReference.child("thumb").child(databaseChatKey).child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null && (boolean) snapshot.getValue()) {
                    // 채팅방의 좋아요를 저장하는 데이터베이스에 <uid : true> 형태로 저장되어 있을때
                    chatRoomThumbButton.setBackgroundResource(R.drawable.ic_heart_pull_red);
                    userThumbChatState = true;
                } else {
                    chatRoomThumbButton.setBackgroundResource(R.drawable.ic_heart_blank);
                    userThumbChatState = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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

                if (inputMessage != null && inputMessage.length() > 0) {
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
            }
        });

        // 백버튼 눌렀을때 뒤로 이동
        chatRoomBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // 좋아요 버튼 눌렀을때
        chatRoomThumbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!uid.equals(masterUid)) {

                    Map<String, Object> userThumb = new HashMap<>();
                    if (userThumbChatState) {
                        // 좋아요 한 채팅방일때
                        // 좋아요를 저장하는 데이터베이스에 <uid : null> 형태로 데이터를 삭제한다
                        userThumb.put(uid, null);
                        rootReference.child("thumb").child(databaseChatKey).updateChildren(userThumb);
                        userThumbChatState = false;
                    } else {
                        // 아직 좋아요 상태가 아닐때
                        // 좋아요를 저장하는 데이터베이스에 <uid : true> 형태로 데이터를 저장한다
                        userThumb.put(uid, true);
                        rootReference.child("thumb").child(databaseChatKey).updateChildren(userThumb);
                        userThumbChatState = true;
                    }
                    refreshThumbCount();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 일단은 opPause 일때 채팅방 나가기
        // 이후에 백그라운드에서 구현해보도록 하자
        rootReference.child("members").child(databaseChatKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try{
                    String uidState = (String) snapshot.child(uid).getValue();
                    if (uidState.equals("master")) {
                        // 방장일때는 채팅에서 최장하더라도 데이터베이스에서 삭제하지 않는다
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    // try 에서 uid 의 String 형변환에 실패했을때
                    // value 로 true (boolean) 값을 가지는 일반 채팅 참가 유저인 경우이다
                    // boolean 으로 형변환 하여 채팅방에서 데이터를 삭제한다

                    if (snapshot.child(uid).getValue() != null) {
                        boolean uidState = (boolean) snapshot.child(uid).getValue();
                        if (uidState) {
                            rootReference.child("members").child(databaseChatKey).child(uid).setValue(null);
                        }
                    }

                    // 유저 퇴장메세지 추가하기
                    addBroadcastMessageUserAttend(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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


    // 유저 채팅방 참가/퇴장 시 채팅방 참가/퇴장을 알리는 메세지 추가
    // 인자가 true 일때 참가메세지, false 일때 퇴장메세지이다
    // ex) 재혁님이 입장하였습니다, 재혁님이 퇴장하였습니다
    private void addBroadcastMessageUserAttend(final boolean state) {
        // uid 를 통하여 참가여부 메세지에 보여줄 유저 이름을 구한다
        rootReference.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData userData = snapshot.getValue(UserData.class);
                String showUserName = userData.getName();
                // 유저 닉네임이 설정되어 있지 않을때
                // 채팅방에서 메세지작성자로 이메일을 표시한다
                if (showUserName == null) {
                    showUserName = userData.getEmail();
                }

                // 참가/ 퇴장 문장 만들기
                String stateMessage = "";
                if (state) {
                    stateMessage = " 들어왔습니다!";
                } else {
                    stateMessage = " 나갔습니다.";
                }
                String userActionMessage =  "'" + showUserName + "' 님이" + stateMessage;

                // 유저 이름을 포함한 메세지 작성 후 메세지 데이터베이스에 저장한다
                DatabaseReference messagesReference = rootReference.child("messages");
                Message userActionBroadcast = new Message(true, userActionMessage);
                messagesReference.child(databaseChatKey).push().setValue(userActionBroadcast);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // 데이터베이스에서 채팅이 좋아요 받은 개수(thumb 의 데이터 개수)를 채팅데이터 chats 에 저장한다
    // 기존에 chats 데이터에서 아이템에 보여줄 데이터를 참조하는 리사이클러뷰에서 좋아요 받은 개수를 보여주기 위해
    // 아이템마다 thumb 데이터에 접근할 때 아이템에 표시되는 속도가 느려지기 때문에
    private void refreshThumbCount() {
        rootReference.child("thumb").child(databaseChatKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int getThumbCount = (int) snapshot.getChildrenCount();
                Map<String, Object> chatThumbMap = new HashMap<>();
                chatThumbMap.put("thumb", getThumbCount);
                rootReference.child("chats").child(databaseChatKey).updateChildren(chatThumbMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}