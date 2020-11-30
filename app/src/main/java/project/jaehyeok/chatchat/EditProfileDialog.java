package project.jaehyeok.chatchat;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditProfileDialog {
    private FirebaseUser userAuth = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();; // 데이터베이스 진입
    private DatabaseReference usersReference = firebaseDatabase.getReference("users");
    private String uid;

    private Context context;
    private WindowManager.LayoutParams params;
    // 0일때 이름변경, 1일때 비밀번호 변경
    private int type;

    public EditProfileDialog(Context context, int type, String uid) {
        this.context = context;
        this.type = type;
        this.uid = uid;
    }

    // 호출할 다이얼로그 함수를 정의한다.
    public void callFunction(final TextView viewUserName) {
        // 기존 닉네임
        String name = viewUserName.getText().toString();

        // 커스텀 다이얼로그를 정의하기위해 Dialog클래스를 생성한다.
        final Dialog dialog = new Dialog(context);
        // 액티비티의 타이틀바를 숨긴다.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);


        switch (type) {
            case 0: // 이름변경 다이얼로그
                dialog.setContentView(R.layout.edit_user_name_dialog);
                break;
            case 1: // 비밀번호 변경 다이얼로그
                dialog.setContentView(R.layout.edit_user_password_dialog);
                break;
            default:
                break;
        }


        // 다이얼로그의 width, height 가 XML 파일에 입력한 대로 적용되지 않는 문제
        // 아래 코드를 이용하여 다이얼로그에 width, height 속성을 적용한다
        params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        // 다이얼로그 외부 터치 시 종료되지 않도록 설정
        dialog.setCancelable(false);

        // 키보드 위에 버튼 올라오도록 설정
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // 커스텀 다이얼로그를 노출한다.
        dialog.show();

        // 유저 정보를 수정을 위해 입력을 바로 할 수 있도록 키보드를 띄운다
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        // 두 레이아웃에서의 기능이 동일한 백버튼 설정
        final Button editDialogBack = dialog.findViewById(R.id.editDialogBack);
        editDialogBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 다이얼로그 종료 시 키보드가 올라온상태로 유지되기 때문에 키보드르 숨긴다
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0 );
                dialog.dismiss();
            }
        });

        final TextView dialogTitle = dialog.findViewById(R.id.editDialogTitle);
        final Button editDialogSave = dialog.findViewById(R.id.editDialogSave);
        // 뷰에 값 초기화 및 변경사항 입력
        switch (type) {
            case 0: // 이름변경 다이얼로그
                dialogTitle.setText("이름변경");

                // 처음 다이얼로그가 떳을때 기존 이름이 적힌 EditText 에 포커스를 준다
                final EditText inputName = dialog.findViewById(R.id.inputChangePassword);
                inputName.setText(name);
                inputName.requestFocus();

                // 변경 사항 저장 눌렀을때
                editDialogSave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 변경된 이름을 users 데이터베이스에 저장하고
                        // UserProfileActivity 로 돌아왔을때 변경된 유저이름이 나타나도록 한다
                        String newName = inputName.getText().toString().trim();

                        Map<String, Object> setNewName = new HashMap<>();
                        setNewName.put("name", newName);
                        usersReference.child(uid).updateChildren(setNewName);

                        viewUserName.setText(newName);

                        // 커스텀 다이얼로그를 종료한다.
                        // 종료이후 키보드가 떠있기 때문에 강제로 키보드를 숨긴다
                        dialog.dismiss();
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0 );
                        Toast.makeText(context, "이름 변경완료.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;

            case 1: // 비밀번호 변경 다이얼로그
                dialogTitle.setText("비밀번호 변경");
                final EditText inputPassword = dialog.findViewById(R.id.inputChangePassword);
                final EditText inputPasswordCheck = dialog.findViewById(R.id.inpupCheckPassword);
                final TextView checkPasswordMessage = dialog.findViewById(R.id.checkPasswordMessge);
                inputPassword.requestFocus();

                // 변경 사항 저장 눌렀을때
                editDialogSave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 비밀번호 재입력값과 비밀번호가 일치할때
                        checkPasswordMessage.setVisibility(View.INVISIBLE);
                        String newPassword = inputPassword.getText().toString().trim();
                        String checkPassword = inputPasswordCheck.getText().toString().trim();

                        if (newPassword.equals(checkPassword)) {
                            // 파이어베이스 Auth 에 변경된 비밀번호를 설정한다
                            userAuth.updatePassword(newPassword)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, "비밀번호 변경완료.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                            // 커스텀 다이얼로그를 종료한다.
                            // 종료이후 키보드가 떠있기 때문에 강제로 키보드를 숨긴다
                            dialog.dismiss();
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0 );
                        } else {
                            checkPasswordMessage.setVisibility(View.VISIBLE);
                        }
                    }
                });
                break;
            default:
                break;
        }
    }
}
