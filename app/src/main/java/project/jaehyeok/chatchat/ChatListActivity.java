package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private EditText inputSearchChat;
    private Button searchChatButton;
    private FloatingActionButton addChatButton;
    private ConstraintLayout parentLayout;

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference usersReference; // 데이터베이스경로 (path : users)
    private DatabaseReference chatsReference; // 데이터베이스경로 (path : chats)

    private RecyclerView chatCategoryRecyclerview;
    private RecyclerChatCategoryAdapter chatCategoryAdapter;

    private RecyclerView chatListRecyclerview;
    private RecyclerChatRoomAdapter chatListAdapter;

    private ArrayList<DataSnapshot> chatDataSnapShotList;
    private String uid;

    private static final int CREATE_CHAT = 7000;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        inputSearchChat = findViewById(R.id.inputSearchChat);
        searchChatButton = findViewById(R.id.searchChatButton);
        addChatButton = findViewById(R.id.addChatButton);
        parentLayout = findViewById(R.id.activityChatListLayout);

        // 파이어베이스 접근 권한 갖기
        firebaseAuth = FirebaseAuth.getInstance();
        // 파이어베이스 realtime database 접근 설정
        firebaseDatabase = FirebaseDatabase.getInstance();
        usersReference = firebaseDatabase.getReference("users");
        chatsReference = firebaseDatabase.getReference("chats");
        // String / Long / Double / Boolean / Map<String, Object> / List<Object>


        // 현재 로그인한 사용자 프로필 가져오기
        // ex) [로그인제공업체, uid, name, email]
        ArrayList<String> userProfile = getCurrentUserProfile();
        uid = userProfile.get(1);

        // 파이어베이스 DB 에서 uid 를 통해 계정 데이터의 저장여부를 확인 / 최초 로그인 여부를 판별한다
        // 최초 로그인일때 파이어베이스 DB 의 경로 users 에 새로운 유저 데이터를 생성한다
        verifyUserSavedDatabase(userProfile);
//        chatCategoryRecyclerview.scrollToPosition(1);

        // 파이어베이스 realtime database 에서 채팅 목록 가져오기
        // 리스트에 채팅데이터<DataSnapshot> 담아 리사이클러뷰의 어댑터로 전달한다
        // 해당 액티비티로 전환될 때 마다 추가된 데이터를 목록에 최신화하여 보여주기 위해 onResume 에서 구현
        chatListRecyclerview = findViewById(R.id.chatCategoryRecyclerview);
        chatDataSnapShotList = new ArrayList<>();
        chatsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            // addListenerForSingleValueEvent 실행될때 딱 한번 경로의 데이터를 불러온다
            // chats 경로에 저장된 모든 데이터(채팅)를 불러온다
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapShot: snapshot.getChildren()) {
                    chatDataSnapShotList.add(chatSnapShot);
                }

                // 카테고리별 채팅 목록
                // 채팅목록에 대하여 인기순, 검색결과, 최신순으로 가로스크롤 형태로 지원한다
//                chatCategoryRecyclerview = findViewById(R.id.chatCategoryRecyclerview);
//                chatCategoryAdapter = new RecyclerChatCategoryAdapter(chatDataSnapShotList, uid);
//                chatCategoryRecyclerview.setAdapter(chatCategoryAdapter);
//                LinearLayoutManager chatCategoryLayoutManager = new LinearLayoutManager(ChatListActivity.this, LinearLayoutManager.HORIZONTAL, false);
//                chatCategoryRecyclerview.setLayoutManager(chatCategoryLayoutManager);

                chatListRecyclerview = findViewById(R.id.chatCategoryRecyclerview);
                chatListAdapter = new RecyclerChatRoomAdapter(chatDataSnapShotList, uid);
                chatListRecyclerview.setAdapter(chatListAdapter);
                LinearLayoutManager chatListLayoutManager = new LinearLayoutManager(ChatListActivity.this);
                chatListRecyclerview.setLayoutManager(chatListLayoutManager);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 채팅목록을 아래로 스크롤할때 채팅추가버튼 사라지게하고, 올릴때는 다시 보이도록 한다
        chatListRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // 아래로 스크롤 할 때
                    addChatButton.hide();
                } else if (dy < 0) {
                    addChatButton.show();
                }
            }
        });

        // 채팅개설을 위한 정보를 입력하는 페이지로 이동
        addChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 채팅 개설 페이지에서 생성한 채팅의 데이터를 realtime database 에 저장할 때
                // 생성 계정을 식별하기 위한 uid 를 필요로 한다. 이를 위해 intent 를 통해 uid 를 전달해준다
                ArrayList<String> userProfile = getCurrentUserProfile();
                String uid = userProfile.get(1);

                Intent intent = new Intent(getApplicationContext(), CreateChatActivity.class);
                intent.putExtra("uid", uid);
                startActivityForResult(intent, CREATE_CHAT);
            }
        });

        // 검색버튼 누르면 보이지 않던 검색어 입력창이 나타난다
        // 검색어 입력창이 나타난 상태에서 한번 더 누르면 해당 검색어로 검색 실행한다
        searchChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputSearchChat.getVisibility() == View.INVISIBLE) {
                    // 검색버튼 누르면 검색입력창 보이게 하고, 바로 입력할 수 있도록 포커스를 준다
                    inputSearchChat.setVisibility(View.VISIBLE);
                    inputSearchChat.requestFocus();
                    // 키보드 올리기
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(inputSearchChat, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    // 검색입력창에 검색어 입력됐을때 해당내용으로 검색하기
                    if (inputSearchChat.getText() != null) {
                        CharSequence searchWord = inputSearchChat.getText();
                        chatListAdapter.getFilter().filter(searchWord);
                        Toast.makeText(ChatListActivity.this, searchWord.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 검색어 입력창 외부 터치 했을때 키보드 사라지고 검색입력창도 보이지않도록 설정한다
        parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(inputSearchChat.getWindowToken(), 0);
                inputSearchChat.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 채팅방 생성 후 현재 액티비티로 돌아왔을때
        // 데이터베이스로부터 리사이클러뷰 채팅목록을 새로 전달받고
        // 생성한 채팅방을 유저가 볼 수 있도록 최신순(가로 목록) 가장 상단을 표시하도록 한다
        if (requestCode == CREATE_CHAT && resultCode == RESULT_OK) {

        }

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

    // userProfile 의 uid 를 이용하여 로그인한 계정의 가입여부를 확인한다
    // 파이어베이스 DB (path: users)의 key 값 중 로그인한 계정의 uid와 일치한 것이 있는지 확인
    // 일치한 것이 있을때 -> 이미 가입된 계정
    // 일치한 것이 없을때 -> 최초로그인한 신규유저
    private void verifyUserSavedDatabase(ArrayList<String> userProfile) {
        final String provider = userProfile.get(0);
        final String uid = userProfile.get(1);
        final String name = userProfile.get(2);
        final String email = userProfile.get(3);

        usersReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData value = snapshot.getValue(UserData.class);

                if (value == null) {
                    // value == null 일때 해당 uid 으로 저장된 기록이 없다 -> 최초로그인
                    // 최초 로그인일때 파이어베이스 DB에 유저 데이터를 저장한다
                    UserData userData = new UserData(provider, name, email);
                    usersReference.child(uid).setValue(userData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //
            }
        });
    }


}