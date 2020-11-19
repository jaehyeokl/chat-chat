package project.jaehyeok.chatchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

// ChatListActivity 에서 채팅방 목록에 대한 리사이클러뷰 어댑터 (세로)
public class RecyclerChatRoomAdapter extends RecyclerView.Adapter<RecyclerChatRoomAdapter.ViewHolder> {

    // 데이터
    List<String> data = Arrays.asList("aa", "bb", "cc", "dd", "ee", "ff", "gg");

    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
    public RecyclerChatRoomAdapter() {

    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {
        //
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

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
    public void onBindViewHolder(@NonNull RecyclerChatRoomAdapter.ViewHolder holder, int position) {
        String a = data.get(position);
        holder.textView.setText(a);
    }

    // 전체 데이터(아이템) 개수를 리턴
    @Override
    public int getItemCount() {
        return data.size();
    }
}
