package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.FileProvider;
import androidx.loader.content.CursorLoader;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private Group userProfileImageGroup;
    private ImageView userProfileImage;
    private TextView userProfileName;
    private TextView userProfileEmail;
    private Button changePasswordButton;
    private TextView logoutButton;

    private String uid;

    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    private GoogleSignInClient googleSignInClient;

    public static final int PICK_FROM_ALBUM = 1;
//    private static final int PICK_FROM_ALBUM = 2; //앨범에서 사진 가져오기
//    private static final int CROP_FROM_CAMERA = 3; //가져온 사진을 자르기 위한 변수

    private Uri imageUri;
    private String pathUri;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userProfileImageGroup = findViewById(R.id.userProfileImageGroup);
        userProfileImage = findViewById(R.id.userProfileImage);
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

        //파이어베이스 storage 접근 설정
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // 로그아웃 위한 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // 유저데이터베이스에서 이름, 이메일 불러와 보여주기
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
                    gotoAlbum();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_FROM_ALBUM: { // 갤러리에서 이미지 가져왔을때
                if (data != null) {
                    imageUri = data.getData();
                    pathUri = getPath(imageUri);
                    userProfileImage.setImageURI(imageUri);
//                    Bitmap bitmap = null;
//                    try {
//                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//                        userProfileImage.setImageBitmap(bitmap);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    // 파이어베이스 storage profile_image 폴더에 해당 유저의 uid 로 프로필 이미지를 저장한다
                    // 저장할때 uid 로 이름을 지정하여, 프로필 이미지 변경시에는 기존파일에 덮어씌우도록 한다
                    StorageReference storageRef = storageReference.child("profile_image/" + uid);
                    UploadTask uploadTask = storageRef.putFile(imageUri);

                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //
                        }
                    });
                }
                break;
            }
        }
    }

    // uri 절대경로 가져오기
    public String getPath(Uri uri) {

        String[] projection = {MediaStore.Images.Media.DATA};
        CursorLoader cursorLoader = new CursorLoader(this, uri, projection, null, null, null);

        Cursor cursor = cursorLoader.loadInBackground();
        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();
        return cursor.getString(index);
    }

    private void gotoAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
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