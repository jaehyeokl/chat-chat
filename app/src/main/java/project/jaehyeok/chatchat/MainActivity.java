package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputPassword;
    private Button signInEmail;
    private Button signUpEmail;

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

        /* 파이어베이스 접근 권한 갖기 */
        firebaseAuth = FirebaseAuth.getInstance();

        /* 파이어베이스연동 구글 계정으로 로그인 */
        // 구글 로그인 설정하기
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // 구글 로그인 버튼 클릭
        signInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* 이메일을 통해 회원가입 하기 */
        signUpEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivity(intent);
            }
        });

        /* 이메일을 통한 로그인 */
        signInEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
//                firebaseAuth.signInWithEmailAndPassword(email, password)
            }
        });
    }

    // 구글 로그인을 위한 계정입력 페이지로 안내한다
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
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("구글 로그인", "signInWithCredential:failure", task.getException());
                            //Snackbar.make(mBinding.mainLayout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    // 로그인 이후 이동할 UI 설정
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(this, testActivity.class);
            startActivity(intent);
            finish();
        }
    }
}