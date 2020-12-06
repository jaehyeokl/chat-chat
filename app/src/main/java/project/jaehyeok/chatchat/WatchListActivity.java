package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import project.jaehyeok.chatchat.data.Chat;

public class WatchListActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private BackPressHandler backPressHandler = new BackPressHandler(this);

    private String uid;

    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference; // 데이터베이스경로 (path : root)

    private RecyclerView chatWatchListRecyclerview;
    private RecyclerView chatMyChatRecyclerview;
    private RecyclerChatRoomAdapter chatWatchListAdaptor;
    private RecyclerChatRoomAdapter chatMyChatAdapter;

    private ArrayList<DataSnapshot> watchChatSnapShotList;
    private ArrayList<DataSnapshot> myChatSnapShotList;

    private static final int SELECT_WATCH_LAYOUT = 1;

    private WatchItemTouchHelper thumbChatItemTouchHelper = null;
    private WatchItemTouchHelper myChatItemTouchHelper = null;


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
                            if (!dataSnapshot.hasChild("deleteAt")) {
                                // 삭제된 데이터는 제외한다 (삭제된 채팅데이터만 deleteAt 값이 존재)

                                String chatKey = dataSnapshot.getKey();
                                if (thumbUserChatKeyList.contains(chatKey)) {
                                    watchChatSnapShotList.add(dataSnapshot);
                                }
                            }
                        }
                        chatWatchListAdaptor = new RecyclerChatRoomAdapter(watchChatSnapShotList, uid, SELECT_WATCH_LAYOUT);
                        chatWatchListRecyclerview.setAdapter(chatWatchListAdaptor);
                        LinearLayoutManager chatThumbLayoutManager = new LinearLayoutManager(WatchListActivity.this);
                        chatWatchListRecyclerview.setLayoutManager(chatThumbLayoutManager);

                        // 목록의 아이템에 추가되는 메세지를 실시간으로 표기하기 위한 메소드
                        notifyLatestMessage(watchChatSnapShotList);

                        // 관심 채팅목록의 아이템에대한 스와이프기능 설정
                        // 터치 감지 이벤트를 활용하는 ItemTouchHelper 메소드를 이용해 관심 목록에서의 스와이프 기능을 정의하여 사용한다
                        // (ThumbChatItemTouchHelper.java 에서 정의됨)
                        thumbChatItemTouchHelper = new WatchItemTouchHelper(0, getApplicationContext(), new WatchItemAction() {
                            @Override
                            public void onLeftClicked(int position) {
                                super.onLeftClicked(position);
                                //Toast.makeText(WatchListActivity.this, "왼쪽", Toast.LENGTH_SHORT).show();
                                //System.out.println("왼쪽");
                                //알림 여부
                            }

                            @Override
                            public void onRightClicked(final int position) {
                                super.onRightClicked(position);

                                // 제거여부를 확인하는 다이얼로그 생성
                                AlertDialog.Builder builder = new AlertDialog.Builder(WatchListActivity.this);
                                //builder.setTitle("AlertDialog Title");
                                builder.setMessage("관심 목록에서 제거하시겠습니까?");
                                builder.setPositiveButton("예",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // DB 에서 해당 아이템 채팅방에 대한 좋아요 데이터를 제거한다
                                                // 이후 리사이클러뷰에서 해당 아이템을 제거하여 유저입장에서 아이템이 즉시 사라지도록 구현

                                                // 리사이클러뷰 어댑터의 데이터리스트에서 해당 포지션의 데이터를 가져온다 (firebase realtime database 의 DataSnapShot 객체)
                                                // 데이터베이스에서 채팅방을 식별하는 unique key 값을 얻는다.
                                                DataSnapshot getChatData = chatWatchListAdaptor.filteredList.get(position);
                                                String chatUniqueKey = getChatData.getKey();
                                                // 채팅방 unique key 와 유저 uid 를 통해 DB에 접근하여 값을 좋아요 여부를 삭제한다
                                                rootReference.child("thumb").child(chatUniqueKey).child(uid).setValue(null);

                                                chatWatchListAdaptor.filteredList.remove(position);
                                                chatWatchListAdaptor.notifyItemRemoved(position);
                                                chatWatchListAdaptor.notifyItemRangeChanged(position, chatWatchListAdaptor.getItemCount());
                                            }
                                        });
                                builder.setNegativeButton("아니오",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // 취소
                                            }
                                        });
                                builder.setCancelable(false); // 다이얼로그 외부 터치 금지
                                builder.show();
                            }
                        });
                        // 직접 정의한 스와이프에 대한 추가 기능을(thumbChatItemTouchHelper)
                        // ItemTouchHelper 객체로 생성, 이후 리사이클러뷰와 연결한다
                        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(thumbChatItemTouchHelper);
                        itemTouchHelper.attachToRecyclerView(chatWatchListRecyclerview);

                        // 리사이클러뷰의 아이템마다 스와이프시 나타나는 버튼을 그리는 메소드
                        chatWatchListRecyclerview.addItemDecoration(new RecyclerView.ItemDecoration() {
                            @Override
                            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                                super.onDraw(c, parent, state);
                                thumbChatItemTouchHelper.onDraw(c);
                            }
                        });
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

                    if (!chatSnapShot.hasChild("deleteAt")) {
                        // 삭제된 데이터는 제외한다 (삭제된 채팅데이터만 deleteAt 값이 존재)

                        Chat chat = chatSnapShot.getValue(Chat.class);
                        String masterUid = chat.getMasterUid();
                        // chats 데이터베이스에서 채팅개설자의 uid (masterUid) 가 현재 로그인한 uid 와 같을때 리스트에 저장
                        if (masterUid.equals(uid)) {
                            myChatSnapShotList.add(chatSnapShot);
                        }
                    }
                }
                // 세번째 인자의 값을 통해 리사이클러뷰 어댑터에서 다른 레이아웃을 사용하도록 한다
                chatMyChatAdapter = new RecyclerChatRoomAdapter(myChatSnapShotList, uid, SELECT_WATCH_LAYOUT);
                chatMyChatRecyclerview.setAdapter(chatMyChatAdapter);
                LinearLayoutManager chatThumbLayoutManager = new LinearLayoutManager(WatchListActivity.this);
                chatMyChatRecyclerview.setLayoutManager(chatThumbLayoutManager);

                // 내 채팅목록 아이템 스와이프 구현
                myChatItemTouchHelper = new WatchItemTouchHelper(1, getApplicationContext(), new WatchItemAction() {
                    @Override
                    public void onRightClicked(final int position) {
                        super.onRightClicked(position);

                        // 제거여부를 확인하는 다이얼로그 생성
                        AlertDialog.Builder builder = new AlertDialog.Builder(WatchListActivity.this);
                        //builder.setTitle("AlertDialog Title");
                        builder.setMessage("채팅방을 삭제하시겠습니까?");
                        builder.setPositiveButton("예",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // DB chats 경로에서 채팅방이 삭제된 상태임을 나타내는 데이터를 추가한다
                                        // 실제로 삭제하는 것은 아니고, 삭제된것으로 여기도록 한다
                                        DataSnapshot getChatData = chatMyChatAdapter.filteredList.get(position);
                                        String chatUniqueKey = getChatData.getKey();

                                        Map<String, Object> chatStateDelete = new HashMap<>();
                                        chatStateDelete.put("deleteAt", System.currentTimeMillis());
                                        rootReference.child("chats").child(chatUniqueKey).updateChildren(chatStateDelete);

                                        // 리사이클러뷰에서 해당 아이템을 제거하여 유저입장에서 아이템이 즉시 사라지도록 구현
                                        chatMyChatAdapter.filteredList.remove(position);
                                        chatMyChatAdapter.notifyItemRemoved(position);
                                        chatMyChatAdapter.notifyItemRangeChanged(position, chatMyChatAdapter.getItemCount());
                                    }
                                });
                        builder.setNegativeButton("아니오",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 취소
                                    }
                                });
                        builder.setCancelable(false); // 다이얼로그 외부 터치 금지
                        builder.show();
                    }
                });

                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(myChatItemTouchHelper);
                itemTouchHelper.attachToRecyclerView(chatMyChatRecyclerview);

                // 리사이클러뷰의 아이템마다 스와이프시 나타나는 버튼을 그리는 메소드
                chatMyChatRecyclerview.addItemDecoration(new RecyclerView.ItemDecoration() {
                    @Override
                    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                        super.onDraw(c, parent, state);
                        myChatItemTouchHelper.onDraw(c);
                    }
                });
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

    // 채팅방에 추가되는 메세지를 실시간으로 반영하는 메소드
    private void notifyLatestMessage(ArrayList<DataSnapshot> watchChatSnapShotList) {
        // 순서를 이용하여 특정 아이템에
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressHandler.onBackPressed();
    }
}