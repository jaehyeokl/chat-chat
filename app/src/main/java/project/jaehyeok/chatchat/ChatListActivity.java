package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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

    private Button addChatButton;

    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference usersReference; // 데이터베이스경로 (path : users)
    private DatabaseReference chatsReference; // 데이터베이스경로 (path : chats)

    private RecyclerView chatCategoryRecyclerview;
    private RecyclerChatCategoryAdapter chatCategoryAdapter;

    private ArrayList<DataSnapshot> chatDataSnapShotList;

    private static final int CREATE_CHAT = 7000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        addChatButton = findViewById(R.id.addChatButton);

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

        // 파이어베이스 DB 에서 uid 를 통해 계정 데이터의 저장여부를 확인 / 최초 로그인 여부를 판별한다
        // 최초 로그인일때 파이어베이스 DB 의 경로 users 에 새로운 유저 데이터를 생성한다
        verifyUserSavedDatabase(userProfile);


        // 파이어베이스 realtime database 에서 채팅 목록 가져오기
        // 리스트에 채팅데이터<DataSnapshot> 담아 리사이클러뷰의 어댑터로 전달한다
        chatDataSnapShotList = new ArrayList<>();
        chatsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            // addListenerForSingleValueEvent 실행될때 딱 한번 경로의 데이터를 불러온다
            // chats 경로에 저장된 모든 데이터(채팅)를 불러온다
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapShot: snapshot.getChildren()) {
//                    System.out.println("////스냅샷/// : " + chatSnapShot);
                    chatDataSnapShotList.add(chatSnapShot);
//                    System.out.println("//// 리스트////: " + chatDataSnapShotList);
                }

                // 카테고리별 채팅 목록
                // 채팅목록에 대하여 인기순, 검색결과, 최신순으로 가로스크롤 형태로 지원한다
                chatCategoryRecyclerview = findViewById(R.id.chatCategoryRecyclerview);
                chatCategoryAdapter = new RecyclerChatCategoryAdapter(chatDataSnapShotList); // 데이터 아직 없음
                chatCategoryRecyclerview.setAdapter(chatCategoryAdapter);
                LinearLayoutManager chatCategoryLayoutManager = new LinearLayoutManager(ChatListActivity.this, LinearLayoutManager.HORIZONTAL, false);
                chatCategoryRecyclerview.setLayoutManager(chatCategoryLayoutManager);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        System.out.println("///////////" + chatDataSnapShotList);

        // 카테고리별 채팅 목록
        // 채팅목록에 대하여 인기순, 검색결과, 최신순으로 가로스크롤 형태로 지원한다
//        chatCategoryRecyclerview = findViewById(R.id.chatCategoryRecyclerview);
//        chatCategoryAdapter = new RecyclerChatCategoryAdapter(); // 데이터 아직 없음
//        chatCategoryRecyclerview.setAdapter(chatCategoryAdapter);
//        LinearLayoutManager chatCategoryLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
//        chatCategoryRecyclerview.setLayoutManager(chatCategoryLayoutManager);
//        chatCategoryRecyclerview.scrollToPosition(1);
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        // 중첩 리사이클러뷰의 세로(채팅 목록) 아이템에 대한 클릭이벤트 처리
//        RecyclerChatRoomAdapter adapter = null;
//        adapter.setOnItemClickListener(new RecyclerChatRoomAdapter.OnItemClickListener() {
//            @Override
//            public void onItemCLick(View view, int position) {
//
//            }
//        });

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