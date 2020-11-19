package project.jaehyeok.chatchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// ChatListActivity 에서 채팅방 카테고리에 대한 리사이클러뷰 어댑터 (가로)
public class RecyclerChatCategoryAdapter extends RecyclerView.Adapter<RecyclerChatCategoryAdapter.ViewHolder> {
    // 데이터

    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
    public RecyclerChatCategoryAdapter() {

    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {

        // 카테고리목록 리사이클러뷰(가로)에 채팅목록 리사이클러뷰(세로)를 중첩하기 위해서는
        // 현 위치 뷰 홀더클래스에서 중첩할 리사이클러뷰를 정의한다
        TextView textView;
        RecyclerView chatRoomRecyclerview;
        RecyclerChatRoomAdapter chatRoomAdapter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 뷰 객체에 대한 참조
            textView = itemView.findViewById(R.id.textView2);
            // 중첩할 채팅목록 리사이클러뷰
            chatRoomRecyclerview = itemView.findViewById(R.id.chatRoomRecyclerview);
            chatRoomAdapter = new RecyclerChatRoomAdapter(); // 데이터 아직
            chatRoomRecyclerview.setAdapter(chatRoomAdapter);
            chatRoomRecyclerview.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }

    // 리사이클러뷰의 아이템을 만든다
    // 아이템은 아이템 레이아웃 xml 으로 뷰를 만들고, 해당 뷰를 뷰홀더 객체에 담아 만들어진다
    @NonNull
    @Override
    public RecyclerChatCategoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context chatListActivityContext = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) chatListActivityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.chat_room_category_item, parent,false);
        RecyclerChatCategoryAdapter.ViewHolder viewHolder = new RecyclerChatCategoryAdapter.ViewHolder(view);

        return viewHolder;
    }

    // 만들어진 아이템에 보여줄 데이터를 반영한다
    @Override
    public void onBindViewHolder(@NonNull RecyclerChatCategoryAdapter.ViewHolder holder, int position) {
        holder.textView.setText("안녕하세요" + position);
    }

    // 전체 데이터(아이템) 개수를 리턴
    @Override
    public int getItemCount() {
        return 3;
    }
}
