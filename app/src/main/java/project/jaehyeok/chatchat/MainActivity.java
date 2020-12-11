package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputPassword;
    private Button signInEmail;
    private Button signUpEmail;
    private BackPressHandler backPressHandler = new BackPressHandler(this);

    private FirebaseAuth firebaseAuth = null;
    private GoogleSignInClient googleSignInClient;
    private SignInButton signInGoogle;
    private static final int GOOGLE_SIGN_IN = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputEmail = findViewById(R.id.signInInputEmail);
        inputPassword = findViewById(R.id.signInInputPassword);
        signInEmail = findViewById(R.id.signInEmail);
        signInGoogle = findViewById(R.id.signInGoogle);
        signUpEmail = findViewById(R.id.signUpEmail);

        // 파이어베이스 접근 권한 갖기
        firebaseAuth = FirebaseAuth.getInstance();

        // 파이어베이스의 로그인 세션이 유지되어 있으면 자동 로그인
        // 로그인 액티비티를 생략하고 바로 로그인 이후 채팅목록으로 이동
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            updateUI(user);
        }

        // 구글 로그인 설정하기
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 회원가입 페이지 이동
        signUpEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivity(intent);
            }
        });

        // 이메일을 통한 로그인
        signInEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (isValidEmail()) {
                    signIn(email, password);
                }
            }
        });

        // 구글 로그인 버튼 클릭
        signInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });
    }

    // 이메일 유효성 검사
    private boolean isValidEmail() {
        String email = inputEmail.getText().toString().trim();
        // 올바른 이메일 형식인지 확인
        if (email.isEmpty()) {
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        } else {
            return true;
        }
    }

    // 이메일로 로그인
    private void signIn(String email, String password) {
        // 파이어베이스 이메일을 통한 로그인 메소드
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // uid 를 로컬 데이터에 저장하기
                            localSaveUserUid();

                            Intent intent = new Intent(getApplicationContext(), ChatListActivity.class);
                            // 로그인 이후 백버튼을 통해 다시 로그인페이지로 돌아올 수 없도록 한다
                            // task 의 액티비티를 모두 제거하고, 새로운 task 를 만든다
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "가입된 이메일이 아닙니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    // 구글계정 로그인을 위한 계정입력 페이지로 안내한다
    private void googleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // googleSignIn() 을 통해 전달 받은 구글 로그인 결과(성공/ 실패)
        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // 구글 로그인에 성공했을때, 파이어베이스 인증절차를 진행한다
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("구글 로그인", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("구글 로그인", "Google sign in failed", e);
            }
        }
    }

    // 구글 로그인을 위한 파이어베이스 인증 절차
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // 구글 로그인 최종 성공, 로그인 이후 이동할 페이지 안내
                            Log.d("구글 로그인", "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            // uid 를 로컬 데이터에 저장하기
                            localSaveUserUid();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("구글 로그인", "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    // 구글 로그인 이후 이동할 UI 설정
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(this, ChatListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    // 로그인을 했을때 디바이스에(SharedPreferences) uid 를 저장한다
    // 해당 uid 는 다른 액티비티 또는 클래스에서 현재 로그인한 유저 정보를 식별하기 위해 사용한다
    private void localSaveUserUid() {
        // 저장할 uid 를 가져온다
        ArrayList<String> userProfile = getCurrentUserProfile();
        String uid = userProfile.get(1);

        SharedPreferences sharedPreferences = getSharedPreferences("firebaseAuth", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uid", uid);
        editor.commit();

        //스틱코드에 접속하여 생산성을 향상시켜 보세요, https://stickode.com/
        //System.out.println("@@@@@@@@@@@@@@@@@@@@@@ : " + uid);
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

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressHandler.onBackPressed();
    }
}