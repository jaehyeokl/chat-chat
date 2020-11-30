package project.jaehyeok.chatchat;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class RecyclerChatMessageAdapter extends RecyclerView.Adapter<RecyclerChatMessageAdapter.ViewHolder> {
    // 데이터
    ArrayList<Message> chatMessageList;
    String currentUserUid;
    String checkPreviousUid = "";

    private FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private StorageReference storageReference= firebaseStorage.getReference();

    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
    public RecyclerChatMessageAdapter(ArrayList<Message> chatMessageList, String currentUserUid) {
        this.chatMessageList = chatMessageList;
        this.currentUserUid = currentUserUid;
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView chatUserName;
        private TextView chatMessageOther;
        private TextView chatMessageUser;
        private TextView chatMessageBroadcast;
        private ImageView chatUserProfile;

        private FirebaseDatabase firebaseDatabase; // 데이터베이스 진입
        private DatabaseReference usersReference;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chatUserName = itemView.findViewById(R.id.chatUserName);
            chatMessageOther = itemView.findViewById(R.id.chatMessageOther);
            chatMessageUser = itemView.findViewById(R.id.chatMessageUser);
            chatMessageBroadcast = itemView.findViewById(R.id.chatMessageBroadcast);
            chatUserProfile = itemView.findViewById(R.id.chatUserProfile);
            // 파이어베이스 realtime database 접근 설정
            firebaseDatabase = FirebaseDatabase.getInstance();
            usersReference = firebaseDatabase.getReference("users");
        }
    }

    // 리사이클러뷰의 아이템을 만든다
    // 아이템은 아이템 레이아웃 xml 으로 뷰를 만들고, 해당 뷰를 뷰홀더 객체에 담아 만들어진다
    @NonNull
    @Override
    public RecyclerChatMessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.chat_message_item, parent,false);
        RecyclerChatMessageAdapter.ViewHolder viewHolder = new RecyclerChatMessageAdapter.ViewHolder(view);

        return viewHolder;
    }

    // 만들어진 아이템에 보여줄 데이터를 반영한다
    @Override
    public void onBindViewHolder(@NonNull final RecyclerChatMessageAdapter.ViewHolder holder, int position) {
        Message message = chatMessageList.get(position);
        String getUid = message.getUid();

        // 메세지 객체의 작성자가 있을때는 일반 채팅 메세지이고
        // null 일때 전체 broadcast 메세지이다
        if (getUid != null) {
            if (currentUserUid.equals(getUid)) {
                // 메세지 객체의 작성자가 현재 로그인한 계정일때 (유저 디바이스에서 작성했을때)
                // 카카오톡처럼 오른쪽 정렬 및 내 닉네임 보이지 않도록 설정한다
                holder.chatUserName.setVisibility(View.GONE);
                holder.chatMessageOther.setVisibility(View.GONE);
                holder.chatUserProfile.setVisibility(View.GONE);
                holder.chatMessageUser.setText(message.getMessage());

            } else {
                // 다른 유저의 채팅일때
                holder.chatMessageUser.setVisibility(View.GONE);
                holder.chatMessageOther.setText(message.getMessage());

                // 현재 메세지의 작성자가 이전메세지의 작성자(checkPreviousUid)와 다를때는
                // 채팅메세지에 새로운 작성자를 나타내는 TextView 를 보여준다
//                if (checkPreviousUid.equals(getUid)) {
//                    holder.chatUserName.setText(message.getShowName());
//                } else {
//                    // 이전 작성자와 같은 작성자일때는 중복되는 작성자 이름을 보여주지 않는다
//                    holder.chatUserName.setVisibility(View.GONE);
//                }

                // 일단 보류
                holder.chatUserName.setText(message.getShowName());

                // 상대 프로필 이미지
//                System.out.println("@@@@@@@@@@@@");
//                storageReference.child("profile_image/" + getUid).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                    @Override
//                    public void onSuccess(Uri uri) {
//                        // 이미지 로드 성공시
//                        Glide.with(holder.itemView.getContext())
//                                .load(uri)
//                                .circleCrop()
//                                .into(holder.chatUserProfile);
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                        //이미지 로드 실패시
//                        // Toast.makeText(holder.itemView.getContext(), "실패", Toast.LENGTH_SHORT).show();
//                    }
//                });
            }
        } else {
            // 전체메세지일때
            holder.chatUserName.setVisibility(View.GONE);
            holder.chatMessageOther.setVisibility(View.GONE);
            holder.chatMessageUser.setVisibility(View.GONE);
            holder.chatUserProfile.setVisibility(View.GONE);
            holder.chatMessageBroadcast.setVisibility(View.VISIBLE);
            holder.chatMessageBroadcast.setText(message.getMessage());
        }

        // 현재 메세지의 작성자를 전역변수인 checkPreviousUid 으로 저장해 놓는다
        // 이 변수는 다음메세지에서 작성자가 연속되는 메세지의 작성자의 동일여부를 판단하는데 사용한다.
//        checkPreviousUid = getUid;



        // 나중에 프사 받아올때 쓰일듯함
//        if (uid != null) {
//            holder.usersReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot snapshot) {
//                    UserData userData = snapshot.getValue(UserData.class);
//                    String userName = userData.getName();
//
//                    if (userName != null) {
//                        holder.name.setText(userName);
//                    } else {
//                        holder.name.setText(userData.getEmail());
//                    }
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError error) {
//
//                }
//            });
//        }
    }

    // 전체 데이터(아이템) 개수를 리턴
    @Override
    public int getItemCount() {
        return chatMessageList.size();
    }

    // 채팅내용 리사이클러뷰 스크롤 하다보면 아이템 내용이 무작위로 바뀌는 문제가 생겼음
    // getItemViewType 메소드의 return 값으로 position 을 지정하여 해결한다
    // https://stackoverflow.com/questions/32065267/recyclerview-changing-items-during-scroll
    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
