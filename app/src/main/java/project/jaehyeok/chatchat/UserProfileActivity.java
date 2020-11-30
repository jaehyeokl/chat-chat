package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserProfileActivity extends AppCompatActivity {

    private Group userProfileImageGroup;
    private TextView userProfileName;
    private TextView userProfileEmail;
    private Button changePasswordButton;
    private TextView logoutButton;

    private String uid;

    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference;

    private GoogleSignInClient googleSignInClient;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userProfileImageGroup = findViewById(R.id.userProfileImageGroup);
        userProfileName = findViewById(R.id.userProfileName);
        userProfileEmail = findViewById(R.id.userProfileEmail);
        changePasswordButton = findViewById(R.id.changePassword);
        logoutButton = findViewById(R.id.logoutButton);

        // SharedPreferences 에 저장된 uid 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("firebaseAuth", MODE_PRIVATE);
        uid = sharedPreferences.getString("uid",null);

        // 파이어베이스 realtime database 접근 설정
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootReference = firebaseDatabase.getReference();

        // 로그아웃 위한 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // 유저데이터베이스에서 유저 정보 초기화
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 프로필 이미지 변경
        int userProfileImageIds[] = userProfileImageGroup.getReferencedIds();
        for (int id : userProfileImageIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }

        // 이름 변경
        userProfileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 이름 변경하는 커스텀다이얼로그 띄우기
                EditProfileDialog editNameDialog = new EditProfileDialog(UserProfileActivity.this, 0, uid);
                editNameDialog.callFunction(userProfileName);
            }
        });

        // 비밀번호 변경
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 비밀번호 변경하는 커스텀다이얼로그 띄우기
                EditProfileDialog editNameDialog = new EditProfileDialog(UserProfileActivity.this, 1, uid);
                editNameDialog.callFunction(changePasswordButton);
            }
        });

        // 로그아웃
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
                finishAffinity();
            }
        });
    }

    private void signOut() {
        // 파이어베이스 로그아웃
        FirebaseAuth.getInstance().signOut();

        // 로그아웃 하기 위해서는, 파이어베이스 인증을 통한 로그아웃 뿐만 아니라
        // 구글 계정도 로그아웃을 해주어야 한다
        googleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        // 로그아웃 이므로 이전 태스크의 액티비티 모두 제거 및 새로운 태스크 생성
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
    }
}