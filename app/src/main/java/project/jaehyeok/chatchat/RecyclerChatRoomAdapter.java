package project.jaehyeok.chatchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

// ChatListActivity 에서 채팅방 목록에 대한 리사이클러뷰 어댑터 (세로)
public class RecyclerChatRoomAdapter extends RecyclerView.Adapter<RecyclerChatRoomAdapter.ViewHolder> implements Filterable {
    private ArrayList<DataSnapshot> chatDataSnapShotList = null;
    private int categoryPosition;
    private String uid;
    // 데이터베이스 접근
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference rootReference = firebaseDatabase.getReference();

    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
    public RecyclerChatRoomAdapter(ArrayList<DataSnapshot> chatDataSnapShotList, String uid) {
        this.chatDataSnapShotList = chatDataSnapShotList;
//        this.categoryPosition = categoryPosition;
        this.uid = uid;

        // 카테고리에 따라 데이터를 정렬하는 메소드
//        dataOrderByCategory();
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView chatViewTitle;
        TextView chatViewCurrentCount;
        TextView chatViewPersonnel;
        TextView chatThumbCount;
        Button chatThumbButton;
        Intent toChatActivityIntent;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            chatViewTitle = itemView.findViewById(R.id.chatViewTitle);
            chatViewCurrentCount = itemView.findViewById(R.id.chatViewCurrentCount);
            chatViewPersonnel = itemView.findViewById(R.id.chatViewPersonnel);
            chatThumbCount = itemView.findViewById(R.id.chatThumbCount);
            chatThumbButton = itemView.findViewById(R.id.chatThumbButton);

            // 클릭이벤트 밖에서 인텐트를 초기화하는 이유
            // ChatActivity 에 전달할 데이터를 추가하기 위해서 onBindViewHolder 메소드에서
            // Intent 객체를 참조할 수 있도록 한다
            toChatActivityIntent = new Intent(itemView.getContext(), ChatActivity.class);

            // 아이템 클릭이벤트를 액티비티(ChatListActivity)에서 처리
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 리사이클러뷰 목록에서 아이템의 포지션
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // 채팅 목록의 아이템을 클릭했을때 채팅방으로 입장한다
                        // onBindViewHolder 에서 Intent 에 데이터를 추가하였음
                        view.getContext().startActivity(toChatActivityIntent);
                        //Toast.makeText(view.getContext(), categoryPosition + "/" + position, Toast.LENGTH_SHORT).show();
                    }
                }
            });

//            chatThumbButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    // 리사이클러뷰 목록에서 아이템의 포지션
//                    int position = getAdapterPosition();
//                    if (position != RecyclerView.NO_POSITION) {
//                        // 채팅에 좋아요 표시할 수 있도록 한다
////                        chatThumbButton.setBackgroundResource(R.drawable.ic_heart_pull_red);
//                        chatThumbButton.getBackground();
//
//                        //스틱코드에 접속하여 생산성을 향상시켜 보세요, https://stickode.com/
//                        System.out.println(chatThumbButton.getBackground().toString());
//                        Toast.makeText(view.getContext(), chatThumbButton.getBackground() + "", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
        }
    }

    // 리사이클러뷰의 아이템을 만든다
    // 아이템은 아이템 레이아웃 xml 으로 뷰를 만들고, 해당 뷰를 뷰홀더 객체에 담아 만들어진다
    @NonNull
    @Override
    public RecyclerChatRoomAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.chat_room_list_item, parent,false);
        RecyclerChatRoomAdapter.ViewHolder viewHolder = new RecyclerChatRoomAdapter.ViewHolder(view);
        return viewHolder;

//        View rootView = inflater.inflate(R.layout.chat_room_list_item, null, false);
//        RecyclerView.LayoutParams aa = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        rootView.setLayoutParams(aa);
//        return new ViewHolder(rootView);

    }

    // 만들어진 아이템에 보여줄 데이터를 반영한다
    @Override
    public void onBindViewHolder(@NonNull final RecyclerChatRoomAdapter.ViewHolder holder, int position) {
        DataSnapshot chatDataSnapShot = chatDataSnapShotList.get(position);
        // 데이터베이스 chats 의 key 값과 같은 key 값을 공유하는 다른 데이터베이스에 접근하기위해서
        String databaseKey = chatDataSnapShot.getKey();

        // key 값을 통해 참가 인원을 저장하는 members 에 접근하여 현재 참가중인 인원을 구한다
        rootReference.child("members").child(databaseKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentUserCount = (int) snapshot.getChildrenCount();
                holder.chatViewCurrentCount.setText(currentUserCount + "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // 좋아요 여부를 저장하는 thumb 에 접근하여 현재 유저가 해당 채팅방을 좋아요 했는지 확인한다
        rootReference.child("thumb").child(databaseKey).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null && (boolean) snapshot.getValue()) {
                    // 좋아요 확인됐을때 꽉찬 하트로 변경
                    holder.chatThumbButton.setBackgroundResource(R.drawable.ic_heart_pull_red);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // 데이터 뷰에 적용하기
        Chat chat = chatDataSnapShot.getValue(Chat.class);
        String title = chat.getTitle();
        int personnel = chat.getPersonnel();
        int thumb = chat.getThumb();
        holder.chatViewTitle.setText(title);
        holder.chatViewPersonnel.setText((personnel + ""));
        holder.chatThumbCount.setText(thumb + "");

//        // 아이템의 좋아요 눌렀을때 반영하기
//        holder.chatThumbButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (holder.chatThumbButton.getBackground().toString().equals("android.graphics.drawable.BitmapDrawable@814e51c")) {
//                    System.out.println("좋아요!");
//                } else {
//                    System.out.println("좋아요 해제!");
//                }
//            }
//        });

        // ChatActivity 에서 파이어베이스 데이터베이스에 저장된 메세지, 참가인원 데이터를 불러오기 위해서는
        // 접근하기 위한 key 값이 필요하다. 이를 위해 chatDataSnapShot 에 저장된 key 값을 Intent 를 통해 전달한다
        holder.toChatActivityIntent.putExtra("ChatKey", chatDataSnapShot.getKey());
    }

    // 전체 데이터(아이템) 개수를 리턴
    @Override
    public int getItemCount() {
        return chatDataSnapShotList.size();
    }

    // 데이터 정렬하기
    // 채팅방 목록의 리스트에서 가로목록의 포지션에 따른 채팅방 정렬 변경
//    public void dataOrderByCategory() {
//        switch(categoryPosition){
//            case 0:
//                // 최신 생성 순서로 정렬
//                Collections.reverse(chatDataSnapShotList);
//                break;
//            case 1:
//                // 이후에 좋아요 순서로 정렬
//                break;
//            default:
//                break;
//        }
//    }
}
