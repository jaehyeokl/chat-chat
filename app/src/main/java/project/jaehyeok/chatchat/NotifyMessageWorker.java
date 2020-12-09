package project.jaehyeok.chatchat;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import project.jaehyeok.chatchat.data.Chat;
import project.jaehyeok.chatchat.data.UserData;

// 백그라운드에서 채팅 메세지를 수신하여 알림으로 보여주기 위한 WorkManager 클래스
public class NotifyMessageWorker extends Worker {
    private FirebaseAuth firebaseAuth = null;
    private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
    private DatabaseReference rootReference; // 데이터베이스경로 (path : root)



    public NotifyMessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootReference = firebaseDatabase.getReference();

//        // 현재 로그인 계정의 uid 얻기
//        FirebaseUser user = firebaseAuth.getCurrentUser();
//        ArrayList<String> userProfile = new ArrayList<>();
//        if (user != null) {
//            for (UserInfo profile : user.getProviderData()) {
//                // UID 각 계정마다 부여되는 고유 값
//                userProfile.add(profile.getUid());
//            }
//        } else {
//            notificationMessage(null);
//            System.out.println("로그인 되어있지 않다");
//        }
//        String uid = userProfile.get(0);

        // 앱이 실행중인지 확인
//        if (appInForeground(getApplicationContext())) {
//            System.out.println("################ 포그라운드");
//        } else {
//            System.out.println("################ 포그라운드 아님");
//        }


        // WorkRequest 생성 시 setInputData 를 통하여 전달한 Data
        // 메세지 수신을 요청한 계정의 uid, 수신할 채팅방의 데이터베이스 unique key 값을 전달받는다
        Data data = getInputData();
        String chatUniqueKey = data.getString("chatUniqueKey");
        final String getUid = data.getString("uid");

//        // 채팅방에 업로드되는
//        rootReference.child("chats").child(chatUniqueKey).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Chat chat = snapshot.getValue(Chat.class);
//                String senderUid = chat.getLatestUid();
//
//                // SharedPreferences 에 저장된 uid, 가장 최근에 로그인한 계정을 나타낸다
//                // onDataChange 안에
//                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("firebaseAuth", Context.MODE_PRIVATE);
//                String uid = sharedPreferences.getString("uid",null);
//
//                if (getUid.equals(uid)) {
//                    //
//                }
//
//                // 본인이 작성한 메세지가 아닌것만 알림 실행
//                if (!senderUid.equals(uid)) {
////                                    notificationMessage(chat);
//                    // 오레오 이후 버전부터는 백그라운드에서의 작업이 제한된다
//                    //
//                    setForegroundAsync(notificationMessage(chat));
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });


        // SharedPreferences 에 저장된 uid 가져오기
        // 어플리케이션이 종료됐을때 파이어베이스를 통해 로그인 여부를 확인할 수가 없어
        // 로그인 시 SharedPreferences 에 저장되는 uid 를 사용한다
        // todo 파이어베이스를 통해 어플이 종료됐을때도 로그인 여부를 확인할 수 있는 방법 알아보기
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("firebaseAuth", Context.MODE_PRIVATE);
        final String uid = sharedPreferences.getString("uid",null);

        // 데이터베이스 경로 thumb 에서 현재 로그인한 계정이 좋아요한 채팅방 unique key 저장
        // 이후 경로 chats 에서 해당 채팅방의 데이터 변화를 감지하는 리스너 실행

        final String finalUid = uid;
        rootReference.child("thumb").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 각 채팅방의 좋아요 유저를 저장하는 데이터베이스에서
                // 현재 로그인한 유저가 좋아요 한 채팅방의 unique key 값을 리스트에 저장한다
                ArrayList<String> thumbUserChatKeyList = new ArrayList<>();
                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    // 한 채팅방의 좋아요 한 유저를 저장하는 map
                    Map<String, Object> getThumbUser = (Map<String, Object>) dataSnapshot.getValue();
                    // 좋아요한 유저의 uid 만 저장한 set
                    Set<String> thumbUserUidSet  = getThumbUser.keySet();
                    // 채팅을 좋아요 한 목록에 현재 유저가 있을때, 채팅방의 고유 식별값을 리스트에 저장한다
                    if (thumbUserUidSet.contains(finalUid)) {
                        String chatUniqueKey = dataSnapshot.getKey();
                        thumbUserChatKeyList.add(chatUniqueKey);
                    }
                }

                // chats 데이터베이스에서 unique key 가 일치하는 채팅방을 찾고,
                // 해당 채팅방 데이터의 변화를 감지하는 리스너를 실행시킴으로써 채팅데이터의 변화를 감지한다
                for (String chatUniqueKey: thumbUserChatKeyList) {
                    rootReference.child("chats").child(chatUniqueKey).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Chat chat = snapshot.getValue(Chat.class);
                            String senderUid = chat.getLatestUid();
                            //System.out.println("@@@@@@@@@@@@@@@@@@@@ : "  + senderUid);

                            // 본인이 작성한 메세지가 아닌것만 알림 실행
                            if (!senderUid.equals(uid)) {
//                                    notificationMessage(chat);
                                // 오레오 이후 버전부터는 백그라운드에서의 작업이 제한된다
                                //
                                    setForegroundAsync(notificationMessage(chat));
                                // todo 포그라운드, 백그라운드 체크 후 작업 설정
//                                if (appInForeground(getApplicationContext())) {
//                                    System.out.println("################ 포그라운드");
//                                    //setForegroundAsync(notificationMessage(chat));
//
//                                } else {
//                                    System.out.println("################ 포그라운드 아님");
//                                    //setForegroundAsync(notificationMessage(chat));
//                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return Result.success();
    }

    // 새로운 메세지의 내용을 포함한 알림을 생성한다
    private ForegroundInfo notificationMessage(Chat chat) {
        String title = chat.getTitle();
        String sender = chat.getLatestSender();
        String message = chat.getLatestMessage();
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(getApplicationContext(), WatchListActivity.class);
        notificationIntent.putExtra("sender", sender);
        notificationIntent.putExtra("message", message);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "testChannel")
                .setContentTitle(title)
                .setContentText(sender + " : " + message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // 오레오 이상 알림 설정 시 채널을 만들고 등록을 해야한다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
            CharSequence channelName  = "testChannel";
            String description = "오레오 이상을 테스트 채널";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("testChannel", channelName , importance);
            channel.setDescription(description);

            // 노티피케이션 채널을 시스템에 등록
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);

        } else builder.setSmallIcon(R.mipmap.ic_launcher);

        assert notificationManager != null;
        notificationManager.notify(1234, builder.build()); // 고유숫자로 노티피케이션 동작시킴

        return new ForegroundInfo(1,builder.build());
    }

    // 채팅어플리케이션이 포그라운드, 백그라운드인지 확인한다
    // 이를 활용하여 포그라운드일때는 백그라운드일때와 달리 다른 방식으로 메세지를 보여주기 위해 사용한다
    private boolean appInForeground(@NonNull Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return false;
        }

        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            if (runningAppProcess.processName.equals(context.getPackageName()) &&
                    runningAppProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }
}
