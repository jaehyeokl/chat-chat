package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import project.jaehyeok.chatchat.data.UserData;

public class UserActivity extends AppCompatActivity {

    private Group userProfileGroup;
    private ImageView userProfileImage;
    private TextView userProfileName;
    private TextView userProfileEmail;
    private ListView realtimeHotTopicList;

    private BottomNavigationView bottomNavigation;
    private BackPressHandler backPressHandler = new BackPressHandler(this);

    private String uid;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    private Handler getTopicListHandler;
    private Elements contents;
    private Document doc = null;

    private ArrayList<String> topicListData;
    private ArrayAdapter topicListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        userProfileGroup = findViewById(R.id.userProfileGroup);
        userProfileImage = findViewById(R.id.userProfileImage);
        userProfileName = findViewById(R.id.userProfileName);
        userProfileEmail = findViewById(R.id.userProfileEmail);
        realtimeHotTopicList = findViewById(R.id.realtimeHotTopicListview);

        // 실시간 토픽 순위 (네이버 전체연령 검색어 순위를 크롤링하여 보여준다)
        // 리스트뷰 데이터, 어댑터 연결
        topicListData = new ArrayList<>(); // 순위를 보여줄 리스트뷰 데이터
        System.out.println(topicListData.size() + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        topicListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, topicListData);
        realtimeHotTopicList.setAdapter(topicListAdapter);

        // 실시간 토픽을 크롤링할 스레드 실행 및 수신 핸들러 초기화
        getTopicListHandler = new Handler(Looper.getMainLooper());
        GetTopicListThread getTopicListThread = new GetTopicListThread();
        getTopicListThread.start();

        // 네비게이션의 아이콘이 유저아이콘에 포커스되어있도록 설정
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.actionUser);

        // SharedPreferences 에 저장된 uid 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("firebaseAuth", MODE_PRIVATE);
        uid = sharedPreferences.getString("uid",null);

        // 파이어베이스 realtime database 접근 설정
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootReference = firebaseDatabase.getReference();

        //파이어베이스 storage 접근 설정
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // 유저데이터베이스에서 유저 이름, 이메일 초기화
        rootReference.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData userData = snapshot.getValue(UserData.class);

                String userName = userData.getName();
                String userEmail = userData.getEmail();

                userProfileName.setText(userName);
                userProfileEmail.setText(userEmail);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // storage 에서 유저 프로필사진 불러오기
        storageReference.child("profile_image/" + uid).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // 이미지 로드 성공시
                Glide.with(getApplicationContext())
                        .load(uri)
                        .circleCrop()
                        .into(userProfileImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //이미지 로드 실패시
                //Toast.makeText(getApplicationContext(), "실패", Toast.LENGTH_SHORT).show();
            }
        });

        // 애드몹 광고 게시
        // 모바일 광고 SDK 초기화 (최초 실행시 한번 실행)
        /*
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
         */
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    // 네이버 실시간 검색어 순위를 크롤링하는 스레드 (10초마다 새로고침 되도록 적용)
    //
    class GetTopicListThread extends Thread {
        Handler receiveHandler = getTopicListHandler ;

        @Override
        public void run() {
            String targetUrl = "https://datalab.naver.com/keyword/realtimeList.naver";
            int getTopicNum = 10; // 실시간 검색어 10위까지 보여준다

            while (true) {

                // 크롤링 한 검색어 키워드를 리스트뷰 어댑터에 연결된 데이터에 저정한다
                // 이후 리스트뷰 어댑터를 새로고침하도록 정의된 Runnable 전달함으로써 실시간 순위를 반영한다
                try {
                    doc = Jsoup.connect(targetUrl).get(); // 크롤링할 페이지를 불러온다
                    // <ul class="ranking_list">
                    //    <span class="item_title> '키워드' </span>
                    contents = doc.select("li.ranking_item span.item_title");

                    int count = 0;
                    for(Element element: contents) {
                        String topic = (count + 1) + ".  " + element.text();

                        if (topicListData.size() == 10) {
                            // 새로운 데이터로 덮어씌울때
                            topicListData.set(count, topic);
                        } else {
                            // 크롤링한 데이터가 가장 처음 리스트에 저장될때
                            topicListData.add(count, topic);
                        }
                        count++;

                        if (count == getTopicNum) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 토픽리스트를 새로고침하도록 하는 runnable 정의 및 전달
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        topicListAdapter.notifyDataSetChanged();
                    }
                };
                receiveHandler.post(runnable);

                try {
                    // 10초뒤 반복실행
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 유저프로필 클릭 시 내정보 상세페이지 이동
        int userProfileGroupIds[] = userProfileGroup.getReferencedIds();
        for (int id : userProfileGroupIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), UserProfileActivity.class);
                    startActivity(intent);
                }
            });
        }

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
                        intent = new Intent(getApplicationContext(), WatchListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                    case R.id.actionUser:
                        //
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