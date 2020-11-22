package project.jaehyeok.chatchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerChatMessageAdapter extends RecyclerView.Adapter<RecyclerChatMessageAdapter.ViewHolder> {
    // 데이터
    ArrayList<Message> chatMessageList;

    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
    public RecyclerChatMessageAdapter(ArrayList<Message> chatMessageList) {
        this.chatMessageList = chatMessageList;
    }

    // 아이템 뷰를 저장하는 뷰홀더 클래스
    public class ViewHolder extends RecyclerView.ViewHolder {
        //
        TextView name;
        TextView chatMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            chatMessage = itemView.findViewById(R.id.chatMessage);
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
    public void onBindViewHolder(@NonNull RecyclerChatMessageAdapter.ViewHolder holder, int position) {
        if (chatMessageList != null) {
            Message message = chatMessageList.get(position);
            holder.name.setText(message.getName());
            holder.chatMessage.setText(message.getMessage());
        }
    }

    // 전체 데이터(아이템) 개수를 리턴
    @Override
    public int getItemCount() {
        if (chatMessageList != null) {
            return chatMessageList.size();
        } else {
            return 0;
        }
    }

}
