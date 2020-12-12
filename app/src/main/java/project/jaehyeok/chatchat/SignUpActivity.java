package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.util.function.ToDoubleBiFunction;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputPassword;
    private EditText inputConfirmPassword;
    private Button createAccount;

    private FirebaseAuth firebaseAuth = null;

    private ConnectivityManager connectivityManager;
    private NetworkReceiver networkReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        inputEmail = findViewById(R.id.signUpInputEmail);
        inputPassword = findViewById(R.id.signUpInputPassword);
        inputConfirmPassword = findViewById(R.id.signUpConfirmPassword);
        createAccount = findViewById(R.id.createAccount);

        // 파이어베이스 접근 권한 갖기
        firebaseAuth = FirebaseAuth.getInstance();

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

        // 계정 생성하기
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
                // 입력한 아이디와 비밀번호의 형식이 유효할때 계정을 생성한다
                if (isValidEmail() && isConfirmedPassword()) {
                    createAccount(email, password);
                }
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

    // 비밀번호 확인
    private boolean isConfirmedPassword() {
        String password = inputPassword.getText().toString().trim();
        String checkPassword = inputConfirmPassword.getText().toString().trim();
        // 비밀번호, 비밀번호 확인란의 입력값이 일치한지 / 공백이 아닌지 확인
        if (password.isEmpty()) {
            return false;
        } else if (!password.equals(checkPassword)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    // 파이어베이스를 이용한 이메일 계정 생성
    private void createAccount(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // 계정 생성완료
                            // 로그인화면으로 이동 후 이전화면(회원가입)으로 돌아오지 않도록 한다
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            Toast.makeText(SignUpActivity.this, "가입완료", Toast.LENGTH_SHORT).show();
//                            verificationEmail();
                        } else {
                            Toast.makeText(SignUpActivity.this, "계정 생성에 실패하였습니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 네트워크 연결상태를 확인하는 리시버를 종료해준다
        unregisterReceiver(networkReceiver);
    }
}