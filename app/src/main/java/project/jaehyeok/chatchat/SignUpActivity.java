package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputPassword;
    private EditText confirmPassword;
    private Button createAccount;

    private FirebaseAuth firebaseAuth = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        inputEmail = findViewById(R.id.signUpInputEmail);
        inputPassword = findViewById(R.id.signUpInputPassword);
        confirmPassword = findViewById(R.id.signUpConfirmPassword);
        createAccount = findViewById(R.id.createAccount);

        /* 파이어베이스 접근 권한 갖기 */
        firebaseAuth = FirebaseAuth.getInstance();
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
                    setCreateAccount(email, password);
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
        String checkPassword = confirmPassword.getText().toString().trim();
        // 두번 입력한 비밀번호가 일치한지, 공백이 아닌지 확인
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
    private void setCreateAccount(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // 계정 생성완료, 로그인화면으로 이동
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            Toast.makeText(SignUpActivity.this, "가입완료", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignUpActivity.this, "계정 생성에 실패하였습니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}