package project.jaehyeok.chatchat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import project.jaehyeok.chatchat.data.Chat;

// ChatListActivity 에서 채팅방 목록에 대한 리사이클러뷰 어댑터 (세로)
public class RecyclerChatRoomAdapter extends RecyclerView.Adapter<RecyclerChatRoomAdapter.ViewHolder> implements Filterable {
    private ArrayList<DataSnapshot> chatDataSnapShotList = null; // unfiltered
    private String uid;

    public ArrayList<DataSnapshot> filteredList;

    // 유저가 좋아요한 채팅목록을 보는 리사이클러뷰에서 해당 어댑터를 사용할때는 다른 xml 레이아웃을 사용한다.
    // 이를 식별하기 위한 변수 (default 0  채팅리스트일때 / 1 좋아요한 채팅 목록일때)
    private int selectLayout = 0;

    // 데이터베이스 접근
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference rootReference = firebaseDatabase.getReference();

    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
    public RecyclerChatRoomAdapter(ArrayList<DataSnapshot> chatDataSnapShotList, String uid) {
        this.chatDataSnapShotList = chatDataSnapShotList;
        this.filteredList = chatDataSnapShotList;
        this.uid = uid;
    }

    public RecyclerChatRoomAdapter(ArrayList<DataSnapshot> chatDataSnapShotList, String uid, int selectLayout) {
        this.chatDataSnapShotList = chatDataSnapShotList;
        this.filteredList = chatDataSnapShotList;
        this.uid = uid;
        this.selectLayout = selectLayout;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if(charString.isEmpty()) {
                    filteredList = chatDataSnapShotList;
                } else {
                    System.out.println(charString + ": //////////////////////////////");
                    ArrayList<DataSnapshot> filteringList = new ArrayList<>();
                    for(DataSnapshot dataSnapshot: chatDataSnapShotList) {
                        Chat chatData = dataSnapshot.getValue(Chat.class);
                        String chatTitle = chatData.getTitle();
                        System.out.println(chatTitle);

                        if(chatTitle.toLowerCase().contains(charString)) {
                            filteringList.add(dataSnapshot);
                        }
                    }
                    filteredList = filteringList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredList = (ArrayList<DataSnapshot>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView chatViewTitle;
        TextView chatViewName;
        TextView chatViewMessage;
        TextView chatViewCurrentCount;
        TextView chatViewPersonnel;
        TextView chatThumbCount;
        Button chatThumbButton;
        Intent toChatActivityIntent;

        TextView name;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);

            switch(selectLayout){
                case 0:
                    chatViewTitle = itemView.findViewById(R.id.chatViewTitle);
                    chatViewCurrentCount = itemView.findViewById(R.id.chatViewCurrentCount);
                    chatViewPersonnel = itemView.findViewById(R.id.chatViewPersonnel);
                    chatThumbCount = itemView.findViewById(R.id.chatThumbCount);
                    chatThumbButton = itemView.findViewById(R.id.chatThumbButton);
                    break;
                case 1:
                    chatViewTitle = itemView.findViewById(R.id.chatViewTitle);
                    chatViewName = itemView.findViewById(R.id.chatViewName);
                    chatViewMessage = itemView.findViewById(R.id.chatViewMessage);
                    chatViewCurrentCount = itemView.findViewById(R.id.chatViewCurrentCount);
                    chatViewPersonnel = itemView.findViewById(R.id.chatViewPersonnel);
                    break;
                default:
                    break;
            }

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
        }
    }

    // 리사이클러뷰의 아이템을 만든다
    // 아이템은 아이템 레이아웃 xml 으로 뷰를 만들고, 해당 뷰를 뷰홀더 객체에 담아 만들어진다
    @NonNull
    @Override
    public RecyclerChatRoomAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 상황에따라 사용할 레이아웃을 리스트에 저장
        List<Integer> selectLayoutList = Arrays.asList(R.layout.chat_room_list_item, R.layout.chat_watch_list_item);

        // selectLayout (레이아웃 선택) 값에 따라 다른 레이아웃을 아이템에서 사용할 수 있도록 한다
        View view = inflater.inflate(selectLayoutList.get(selectLayout), parent,false);
        RecyclerChatRoomAdapter.ViewHolder viewHolder = new RecyclerChatRoomAdapter.ViewHolder(view);
        return viewHolder;
    }

    // 만들어진 아이템에 보여줄 데이터를 반영한다
    @Override
    public void onBindViewHolder(@NonNull final RecyclerChatRoomAdapter.ViewHolder holder, int position) {
        DataSnapshot chatDataSnapShot = filteredList.get(position);
        // 데이터베이스 chats 의 key 값과 같은 key 값을 공유하는 다른 데이터베이스에 접근하기위해서
        final String databaseKey = chatDataSnapShot.getKey();

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

        // 데이터 뷰에 적용하기
        Chat chat = chatDataSnapShot.getValue(Chat.class);
        String title = chat.getTitle();
        int personnel = chat.getPersonnel();
        int thumb = chat.getThumb();


        switch(selectLayout){
            case 0:
//                // key 값을 통해 참가 인원을 저장하는 members 에 접근하여 현재 참가중인 인원을 구한다
//                rootReference.child("members").child(databaseKey).addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        int currentUserCount = (int) snapshot.getChildrenCount();
//                        holder.chatViewCurrentCount.setText(currentUserCount + "");
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//
//                    }
//                });

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

//                // 데이터 뷰에 적용하기
//                Chat chat = chatDataSnapShot.getValue(Chat.class);
//                String title = chat.getTitle();
//                int personnel = chat.getPersonnel();
//                int thumb = chat.getThumb();
                holder.chatViewTitle.setText(title);
                holder.chatViewPersonnel.setText((personnel + ""));
                holder.chatThumbCount.setText(thumb + "");


                break;
            case 1:
                rootReference.child("chats").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                            // 채팅이 같을때
                            if (dataSnapshot.getKey().equals(databaseKey)) {
                                Chat chatData = dataSnapshot.getValue(Chat.class);
                                holder.chatViewTitle.setText(chatData.getTitle());
                                holder.chatViewPersonnel.setText((chatData.getPersonnel() + ""));
                                holder.chatViewName.setText(chatData.getLatestSender());
                                holder.chatViewMessage.setText(chatData.getLatestMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                break;
            default:
                break;
        }

        // ChatActivity 에서 파이어베이스 데이터베이스에 저장된 메세지, 참가인원 데이터를 불러오기 위해서는
        // 접근하기 위한 key 값이 필요하다. 이를 위해 chatDataSnapShot 에 저장된 key 값을 Intent 를 통해 전달한다
        holder.toChatActivityIntent.putExtra("ChatKey", databaseKey);
    }

    // 전체 데이터(아이템) 개수를 리턴
    @Override
    public int getItemCount() {
        return filteredList.size();
    }
}
