package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth = null;
    private DatabaseReference myRef; // DB의 주소를 저장
    private FirebaseDatabase chatDB; //DB에 접근할 수 있는 진입점

    private RecyclerView chatCategoryRecyclerview;
    private RecyclerChatCategoryAdapter chatCategoryAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        FirebaseDatabase chatDB = FirebaseDatabase.getInstance();
        DatabaseReference myRef = chatDB.getReference();
        // String / Long / Double / Boolean / Map<String, Object> / List<Object>

        Map<String, UserData> userData = new HashMap<>();
        UserData a = new UserData("이름", "메일");
        userData.put("a", a);

        myRef.child("users").setValue(a);


        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData value = snapshot.getValue(UserData.class);
//                Toast.makeText(ChatListActivity.this, value, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // 파이어베이스 접근 권한 갖기
        firebaseAuth = FirebaseAuth.getInstance();

        // 현재 로그인한 사용자 프로필 가져오기
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // 로그인방식의 경우가 여러가지이기 때문에 (일반 이메일, 구글계정)
            // 반복문을 통해 현재 계정과 연결된 로그인제공 업체를 찾고 프로필을 가져온다
            for (UserInfo profile : user.getProviderData()) {
                // Id of the provider (ex: google.com)
                String providerId = profile.getProviderId();

                // UID specific to the provider
                String uid = profile.getUid();
                //user.getIdToken(); 을 사용하라고 한다

                // Name, email address, and profile photo Url
                String name = profile.getDisplayName();
                String email = profile.getEmail();
                Uri photoUrl = profile.getPhotoUrl();

                Log.d("유저", name + '/' + email + '/' + photoUrl);
            }
        } else {
            // No user is signed in
        }

        Task<com.google.firebase.auth.GetTokenResult> token = user.getIdToken(true);
        Toast.makeText(this, token.toString(), Toast.LENGTH_SHORT).show();
        System.out.println(token);

        // 카테고리별 채팅 목록
        // 채팅목록에 대하여 인기순, 검색결과, 최신순으로 가로스크롤 형태로 지원한다
        chatCategoryRecyclerview = findViewById(R.id.chatCategoryRecyclerview);
        chatCategoryAdapter = new RecyclerChatCategoryAdapter(); // 데이터 아직 없음
        chatCategoryRecyclerview.setAdapter(chatCategoryAdapter);
        LinearLayoutManager chatCategoryLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        chatCategoryRecyclerview.setLayoutManager(chatCategoryLayoutManager);

//        chatCategoryRecyclerview.scrollToPosition(1);
    }
}