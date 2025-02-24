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
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import java.util.Collections;
import java.util.Comparator;

import project.jaehyeok.chatchat.data.Chat;
import project.jaehyeok.chatchat.data.UserData;

public class ChatListActivity extends AppCompatActivity {

    private EditText inputSearchChat;
    private Button searchChatButton;
    private TextView viewListOrderByLatest, viewListOrderByThumb;
    private FloatingActionButton addChatButton;
    private ConstraintLayout parentLayout;
    private BottomNavigationView bottomNavigation;
    private BackPressHandler backPressHandler = new BackPressHandler(this);

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference usersReference; // 데이터베이스경로 (path : users)
    private DatabaseReference chatsReference; // 데이터베이스경로 (path : chats)

    private RecyclerView chatRecyclerviewByThumb;
    private RecyclerView chatRecyclerviewByLatest;
    private RecyclerChatRoomAdapter chatThumbAdapter;
    private RecyclerChatRoomAdapter chatLatestAdapter;

    private ArrayList<DataSnapshot> chatDataSnapShotByThumb;
    private ArrayList<DataSnapshot> chatDataSnapShotByLatest;
    private String uid;

    private ConnectivityManager connectivityManager;
    private NetworkReceiver networkReceiver;

    private static final int CREATE_CHAT = 7000;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        inputSearchChat = findViewById(R.id.inputSearchChat);
        searchChatButton = findViewById(R.id.searchChatButton);
        viewListOrderByLatest = findViewById(R.id.viewListOrderByLatest);
        viewListOrderByThumb = findViewById(R.id.viewListOrderByThumb);
        addChatButton = findViewById(R.id.addChatButton);
        parentLayout = findViewById(R.id.activityChatListLayout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // 첫 실행 시 인기순 정렬
        changeButtonByOrder(true);

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

        // 네트워크 상태 확인
        // 네트워크 연결상태에 변화에 대한 System Broadcast 를 감지하는 리시버를 실행시킨다
        // ex) wifi 연결상태, 셀룰러데이터 연결상태 변화
        // 리시버에서는 모든 네트워크에 연결되지 않았을때 다시 연결될 때 까지 로딩화면을 띄우도록 구현
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        IntentFilter checkNetworkFilter = new IntentFilter();
        networkReceiver = new NetworkReceiver();
        checkNetworkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, checkNetworkFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 파이어베이스 realtime database 에서 채팅 목록 가져오기
        // 리스트에 채팅데이터<DataSnapshot> 담아 리사이클러뷰의 어댑터로 전달한다
        // 해당 액티비티로 전환될 때 마다 추가된 데이터를 목록에 최신화하여 보여주기 위해 onResume 에서 구현
        chatRecyclerviewByThumb = findViewById(R.id.chatThumbRecyclerview);
        chatRecyclerviewByLatest = findViewById(R.id.chatLatestRecyclerview);
        chatDataSnapShotByThumb = new ArrayList<>(); // 좋아요 순으로 저장할 리스트
        chatDataSnapShotByLatest = new ArrayList<>(); // 최신순으로 저장할 리스트
        chatsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            // addListenerForSingleValueEvent 실행될때 딱 한번 경로의 데이터를 불러온다
            // chats 경로에 저장된 모든 데이터(채팅)를 불러온다
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapShot: snapshot.getChildren()) {

                    if (!chatSnapShot.hasChild("deleteAt")) {
                        // 삭제된 데이터는 제외한다 (삭제된 채팅데이터만 deleteAt 값이 존재)
                        chatDataSnapShotByThumb.add(chatSnapShot);
                        chatDataSnapShotByLatest.add(chatSnapShot);
                    }
                }

                // 리스트를 각각 좋아요순, 최신순으로 정렬한다
                Collections.sort(chatDataSnapShotByThumb, new Comparator<DataSnapshot>() {
                    @Override
                    public int compare(DataSnapshot t1, DataSnapshot t2) {
                        Chat chat1 = t1.getValue(Chat.class);
                        Chat chat2 = t2.getValue(Chat.class);

                        if (chat1.getThumb() < chat2.getThumb()) {
                            return -1;
                        } else if (chat1.getThumb() > chat2.getThumb()) {
                            return 1;
                        }
                        return 0;
                    }
                });
                Collections.reverse(chatDataSnapShotByThumb);
                Collections.reverse(chatDataSnapShotByLatest);

                // 좋아요 순서로 채팅을 정렬한 리사이클러뷰 초기화
                chatRecyclerviewByThumb = findViewById(R.id.chatThumbRecyclerview);
                chatThumbAdapter = new RecyclerChatRoomAdapter(chatDataSnapShotByThumb, uid);
                chatRecyclerviewByThumb.setAdapter(chatThumbAdapter);
                LinearLayoutManager chatThumbLayoutManager = new LinearLayoutManager(ChatListActivity.this);
                chatRecyclerviewByThumb.setLayoutManager(chatThumbLayoutManager);

                // 최신 순서로 채팅을 정렬한 리사이클러뷰 초기화
                chatRecyclerviewByLatest = findViewById(R.id.chatLatestRecyclerview);
                chatLatestAdapter = new RecyclerChatRoomAdapter(chatDataSnapShotByLatest, uid);
                chatRecyclerviewByLatest.setAdapter(chatLatestAdapter);
                LinearLayoutManager chatLatestLayoutManager = new LinearLayoutManager(ChatListActivity.this);
                chatRecyclerviewByLatest.setLayoutManager(chatLatestLayoutManager);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // 채팅 목록 리사이클러뷰를 아래로 스크롤 할 때 유저가 목록에 집중 할 수 있도록
        // 채팅 생성 버튼을 보이지 않도록 설정, 반대로 올릴때 다시 나타난다
        chatRecyclerviewByThumb.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

                if (chatRecyclerviewByThumb.getVisibility() == View.VISIBLE) {
                    if (dy > 0) {
                        // 아래로 스크롤 할 때
                        addChatButton.hide();
                    } else if (dy < 0) {
                        addChatButton.show();
                    }
                }
            }
        });

        // 채팅 목록 리사이클러뷰를 아래로 스크롤 할 때 유저가 목록에 집중 할 수 있도록
        // 채팅 생성 버튼을 보이지 않도록 설정, 반대로 올릴때 다시 나타난다
        chatRecyclerviewByLatest.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (chatRecyclerviewByLatest.getVisibility() == View.VISIBLE) {
                    if (dy > 0) {
                        // 아래로 스크롤 할 때
                        addChatButton.hide();
                    } else if (dy < 0) {
                        addChatButton.show();
                    }
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
                        chatThumbAdapter.getFilter().filter(searchWord);
                        chatLatestAdapter.getFilter().filter(searchWord);
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

        // 좋아요 순서로 정렬하는 버튼이 시각적으로 선택된것 처럼 보이게 설정
        viewListOrderByThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonByOrder(true);
                chatRecyclerviewByThumb.setVisibility(View.VISIBLE);
                chatRecyclerviewByLatest.setVisibility(View.INVISIBLE);
                addChatButton.show();
            }
        });

        // 최신 순서로 정렬하는 버튼이 시각적으로 선택된것 처럼 보이게 설정
        viewListOrderByLatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonByOrder(false);
                chatRecyclerviewByLatest.setVisibility(View.VISIBLE);
                chatRecyclerviewByThumb.setVisibility(View.INVISIBLE);
                addChatButton.show();
            }
        });

        // 하단 네비게이션을 통한 메뉴 이동
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("ResourceType")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                    case R.id.actionHome:
                        //
                        return true;
                    case R.id.actionWatch:
                        intent = new Intent(getApplicationContext(), WatchListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                    case R.id.actionUser:
                        intent = new Intent(getApplicationContext(), UserActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                }
                return false;
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

    // 채팅 리스트 인기순, 최신순 보기 선택 시 버튼 레이아웃 바꾸기
    private void changeButtonByOrder(Boolean viewStateThumb) {
        if (viewStateThumb) {
            viewListOrderByThumb.setTextSize(14);
            viewListOrderByThumb.setBackgroundResource(R.drawable.bottom_border_line);
            viewListOrderByLatest.setTextSize(12);
            viewListOrderByLatest.setBackgroundResource(0);
        } else {
            viewListOrderByLatest.setTextSize(14);
            viewListOrderByLatest.setBackgroundResource(R.drawable.bottom_border_line);
            viewListOrderByThumb.setTextSize(12);
            viewListOrderByThumb.setBackgroundResource(0);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressHandler.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 네트워크 연결상태를 확인하는 리시버를 종료해준다
        unregisterReceiver(networkReceiver);
    }
}