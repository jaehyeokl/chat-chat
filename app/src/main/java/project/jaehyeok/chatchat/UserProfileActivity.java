package project.jaehyeok.chatchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.List;
import java.util.zip.Inflater;

import project.jaehyeok.chatchat.data.UserData;

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

    private static final int PICK_FROM_ALBUM = 1;
    private static final int PICK_FROM_CAMERA = 3;

    private Uri imageUri;
    private String pathUri;

    private ConnectivityManager connectivityManager;
    private NetworkReceiver networkReceiver;

    private PermissionListener permissionListener;
    private BottomSheetDialog selectGalleryOrCameraDialog;
    private Button selectGalleryButton;
    private Button selectCameraButton;


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
        storageReference.child(getString(R.string.storage_path_profile_image) + uid).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
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

        // 네트워크 상태 확인
        // 네트워크 연결상태에 변화에 대한 System Broadcast 를 감지하는 리시버를 실행시킨다
        // ex) wifi 연결상태, 셀룰러데이터 연결상태 변화
        // 리시버에서는 모든 네트워크에 연결되지 않았을때 다시 연결될 때 까지 로딩화면을 띄우도록 구현
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        IntentFilter checkNetworkFilter = new IntentFilter();
        networkReceiver = new NetworkReceiver();
        checkNetworkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, checkNetworkFilter);

        // 갤러리/ 카메라 권한 체크를 위한 리스너 초기화
        permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                //Toast.makeText(UserProfileActivity.this, "권한 허가", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                //Toast.makeText(UserProfileActivity.this, "권한 거부\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        // BottomSheetDialog (카메라/ 앨범 선택)
        // 프로필사진 설정 시 카메라촬영 또는 앨범에서 가져오도록 선택하는 다이얼로그
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View selectDialogView = inflater.inflate(R.layout.select_gallery_camera_dialog, null, false);
        selectGalleryOrCameraDialog = new BottomSheetDialog(UserProfileActivity.this);
        selectGalleryOrCameraDialog.setContentView(selectDialogView);
        selectGalleryOrCameraDialog.create();

        // 다이얼로그에서 앨범을 선택했을때
        selectDialogView.findViewById(R.id.selectGalleryButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 앨범 접근 권한 여부 체크
                TedPermission.with(getApplicationContext())
                        .setPermissionListener(permissionListener)
                        .setRationaleMessage("앨범에 접근하기 위해서는\n접근 권한이 필요합니다")
                        .setDeniedMessage("설정에서 앨범 접근 권한을 허용해주세요")
                        .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .check();

                gotoAlbum();
                selectGalleryOrCameraDialog.dismiss();
            }
        });

        // 다이얼로그에서 카메라를 선택했을때
        selectDialogView.findViewById(R.id.selectCameraButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 카메라 권한 여부 체크
                TedPermission.with(getApplicationContext())
                        .setPermissionListener(permissionListener)
                        .setRationaleMessage("카메라에 접근하기 위해서는\n접근 권한이 필요합니다")
                        .setDeniedMessage("설정에서 카메라 권한을 허용해주세요")
                        .setPermissions(Manifest.permission.CAMERA)
                        .check();

                gotoCamera();
                selectGalleryOrCameraDialog.dismiss();
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
                    // 프로필 이미지를 가져올 방식을 선택하는 BottomSheetDialog 를 보여준다 (앨범/카메라)
                    selectGalleryOrCameraDialog.show();
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
                    StorageReference storageRef = storageReference.child(getString(R.string.storage_path_profile_image) + uid);
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
            case PICK_FROM_CAMERA: {// 카메라로 촬영하여 가져올때
                Toast.makeText(this, "카메라 촬영", Toast.LENGTH_SHORT).show();
            }
            default:
                break;
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

    // 갤러리에서 프로필사진 불러오기
    private void gotoAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    private void gotoCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, PICK_FROM_CAMERA);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 네트워크 연결상태를 확인하는 리시버를 종료해준다
        unregisterReceiver(networkReceiver);
    }
}