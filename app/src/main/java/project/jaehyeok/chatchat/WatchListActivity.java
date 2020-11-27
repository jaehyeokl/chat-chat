package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class WatchListActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private BackPressHandler backPressHandler = new BackPressHandler(this);

    private String uid;

    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference; // 데이터베이스경로 (path : root)

    private RecyclerView chatWatchListRecyclerview;
    private RecyclerView chatMyChatRecyclerview;
    private RecyclerChatRoomAdapter chatWatchListAdaptor;
    private RecyclerChatRoomAdapter chatLatestAdapter;

    private ArrayList<DataSnapshot> watchChatSnapShotList;
    private ArrayList<DataSnapshot> myChatSnapShotList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_list);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.actionWatch);

        // SharedPreferences 에 저장된 uid 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("firebaseAuth", MODE_PRIVATE);
        uid = sharedPreferences.getString("uid",null);

        // 파이어베이스 realtime database 접근 설정
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootReference = firebaseDatabase.getReference();

        // 좋아요한 채팅목록, 내가 만든 채팅목록 리사이클러뷰 초기화
        chatWatchListRecyclerview = findViewById(R.id.chatWatchListRecyclerview);
        chatMyChatRecyclerview = findViewById(R.id.chatMyChatRecyclerview);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 좋아요한 채팅목록, 내가 만든 채팅 목록을 보여주기 위해
        // 파이어베이스 데이터베이스에서 채팅데이터를 리스트에 후 리사이클러뷰의 어댑터에 전달한다
        // 해당 액티비티로 전환될 때 마다 추가된 데이터를 목록에 최신화하여 보여주기 위해 onResume 에서 구현

        // 좋아요한 채팅방 목록 구현하기
        watchChatSnapShotList = new ArrayList<>(); // 좋아요 순으로 저장할 리스트
        rootReference.child("thumb").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 각 채팅방의 좋아요 유저를 저장하는 데이터베이스에서
                // 현재 로그인한 유저가 좋아요 한 채팅방의 unique key 값을 리스트에 저장한다
                final ArrayList<String> thumbUserChatKeyList = new ArrayList<>();
                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    // 한 채팅방의 좋아요 한 유저를 저장하는 map
                    Map<String, Object> getThumbUser = (Map<String, Object>) dataSnapshot.getValue();
                    // 좋아요한 유저의 uid 만 저장한 set
                    Set<String> thumbUserUidSet  = getThumbUser.keySet();
                    // 채팅을 좋아요 한 목록에 현재 유저가 있을때, 채팅방의 고유 식별값을 리스트에 저장한다
                    if (thumbUserUidSet.contains(uid)) {
                        String chatUniqueKey = dataSnapshot.getKey();
                        thumbUserChatKeyList.add(chatUniqueKey);
                    }
                }

                // 위의 좋아요 채팅방 unique key 값 리스트를 이용하여 데이터베이스 chats 에서
                // 해당 key 값을 가지는 채팅방들의 Chat 데이터를 리스트에 저장한다.
                // 좋아요한 채팅방의 목록을 나타낼 리사이클러뷰의 데이터로 이용된다
                rootReference.child("chats").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                            String chatKey = dataSnapshot.getKey();
                            if (thumbUserChatKeyList.contains(chatKey)) {
                                watchChatSnapShotList.add(dataSnapshot);
                            }
                        }
                        chatWatchListAdaptor = new RecyclerChatRoomAdapter(watchChatSnapShotList, uid);
                        chatWatchListRecyclerview.setAdapter(chatWatchListAdaptor);
                        LinearLayoutManager chatThumbLayoutManager = new LinearLayoutManager(WatchListActivity.this);
                        chatWatchListRecyclerview.setLayoutManager(chatThumbLayoutManager);

                        ItemTouchHelper.Callback callback = new ThumbChatItemTouchHelper(chatWatchListAdaptor);
                        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
                        touchHelper.attachToRecyclerView(chatWatchListRecyclerview);
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

        // 내가 만든 채팅방목록 구현하기
        myChatSnapShotList = new ArrayList<>(); // 최신순으로 저장할 리스트
        rootReference.child("chats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapShot: snapshot.getChildren()) {
                    Chat chat = chatSnapShot.getValue(Chat.class);
                    String masterUid = chat.getMasterUid();
                    // chats 데이터베이스에서 채팅개설자의 uid (masterUid) 가 현재 로그인한 uid 와 같을때 리스트에 저장
                    if (masterUid.equals(uid)) {
                        myChatSnapShotList.add(chatSnapShot);
                    }
                }
                chatLatestAdapter = new RecyclerChatRoomAdapter(myChatSnapShotList, uid);
                chatMyChatRecyclerview.setAdapter(chatLatestAdapter);
                LinearLayoutManager chatThumbLayoutManager = new LinearLayoutManager(WatchListActivity.this);
                chatMyChatRecyclerview.setLayoutManager(chatThumbLayoutManager);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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
                        intent = new Intent(getApplicationContext(), ChatListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                    case R.id.actionWatch:
                        //
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
    public void onBackPressed() {
        //super.onBackPressed();
        backPressHandler.onBackPressed();
    }
}