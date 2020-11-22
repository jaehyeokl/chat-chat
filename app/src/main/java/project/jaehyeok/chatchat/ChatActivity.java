package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference;

    private String databaseChatKey;

    private RecyclerView chatMessageRecyclerview;
    private RecyclerChatMessageAdapter chatMessageAdapter;

    private ArrayList<Message> chatMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 파이어베이스 접근 권한 갖기
        firebaseAuth = FirebaseAuth.getInstance();
        // 파이어베이스 realtime database 접근 설정
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootReference = firebaseDatabase.getReference();

        // 채팅목록을 터치해서 채팅방에 들어왔을때
        // 데이터베이스에서 현재 채팅에 대한 메세지, 멤버에 접근하기 위한 채팅의 key 값
        Intent getIntent = getIntent();
        databaseChatKey = getIntent.getStringExtra("ChatKey");

        // 메세지 목록 리사이클러뷰
        chatMessages = new ArrayList<>();
        // 채팅방 처음 참가했을때, 채팅 내역 불러와서 저장하기
        rootReference.child("messages").child(databaseChatKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapShot: snapshot.getChildren()) {
                    // 리사이클러뷰의 데이터로 저장하기 위해 각 메세지를 메세지 객체로 변환하여 리스트에 저장한다
                    Message message = messageSnapShot.getValue(Message.class);
                    chatMessages.add(message);
                    //System.out.println(message.getMessage());
                }

                // 리사이클러뷰 리스트 적용했으니 데이터 새로고침만 해주기
                chatMessageRecyclerview = findViewById(R.id.chatMessageRecyclerview);
                chatMessageAdapter = new RecyclerChatMessageAdapter(chatMessages); // 데이터
                chatMessageRecyclerview.setAdapter(chatMessageAdapter);
                chatMessageRecyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        // 채팅방에 처음 들어왔을때 메세지 목록에 나타내기
//        firstTime();











        // Chats 에서 채팅 타이틀 가져오기
//        rootReference.child("chats");

        // Members 에서 최대 인원 가져오기
//        rootReference.child("members");

        // Messages 에서 채팅 내역 올리기
        // messages 의 데이터 변경 이벤트 수신








//        chatMessages = new ArrayList<>();
//        rootReference.child("messages").child(databaseChatKey).addChildEventListener(new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                System.out.println("aaaaaaaaaaaaaaaaaaaaaa : " + snapshot);
//                System.out.println("추가");
//
//                chatMessages.add(snapshot.getValue(Message.class));
//
//                System.out.println(chatMessages);
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                System.out.println("aaaaaaaaaaaaaaaaaaaaaa : " + snapshot);
//                System.out.println("체인지");
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
//                System.out.println("aaaaaaaaaaaaaaaaaaaaaa : " + snapshot);
//                System.out.println("삭제");
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                System.out.println("aaaaaaaaaaaaaaaaaaaaaa : " + snapshot);
//                System.out.println("이동");
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
    }

//    private void firstTime() {
//        chatMessages = new ArrayList<>();
//        // 채팅방 처음 참가했을때, 채팅 내역 불러와서 저장하기
//        rootReference.child("messages").child(databaseChatKey).addValueEventListener(new ValueEventListener() {
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
//                chatMessageAdapter.notifyDataSetChanged();
//                System.out.println("/////// 리스트 적용됐나 : " + chatMessages);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
//    }
}