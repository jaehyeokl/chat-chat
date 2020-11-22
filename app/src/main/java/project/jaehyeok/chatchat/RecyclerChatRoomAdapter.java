package project.jaehyeok.chatchat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;

// ChatListActivity 에서 채팅방 목록에 대한 리사이클러뷰 어댑터 (세로)
public class RecyclerChatRoomAdapter extends RecyclerView.Adapter<RecyclerChatRoomAdapter.ViewHolder> {
    ArrayList<DataSnapshot> chatDataSnapShotList = null;
    int categoryPosition;

    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
    public RecyclerChatRoomAdapter(ArrayList<DataSnapshot> chatDataSnapShotList, int categoryPosition) {
        this.chatDataSnapShotList = chatDataSnapShotList;
        this.categoryPosition = categoryPosition;
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        Intent toChatActivityIntent;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);

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
                        Toast.makeText(view.getContext(), categoryPosition + "/" + position, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            textView = itemView.findViewById(R.id.textView3);
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
    }

    // 만들어진 아이템에 보여줄 데이터를 반영한다
    @Override
    public void onBindViewHolder(@NonNull final RecyclerChatRoomAdapter.ViewHolder holder, int position) {
        DataSnapshot chatDataSnapShot = chatDataSnapShotList.get(position);
        Chat chat = chatDataSnapShot.getValue(Chat.class);
        String title = chat.getTitle();
        int personnel = chat.getPersonnel();
        holder.textView.setText(title);

        // ChatActivity 에서 파이어베이스 데이터베이스에 저장된 메세지, 참가인원 데이터를 불러오기 위해서는
        // 접근하기 위한 key 값이 필요하다. 이를 위해 chatDataSnapShot 에 저장된 key 값을 Intent 를 통해 전달한다
        holder.toChatActivityIntent.putExtra("ChatKey", chatDataSnapShot.getKey());
    }

    // 전체 데이터(아이템) 개수를 리턴
    @Override
    public int getItemCount() {
        return chatDataSnapShotList.size();
    }
}
