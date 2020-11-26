//package project.jaehyeok.chatchat;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.firebase.database.DataSnapshot;
//
//import java.util.ArrayList;
//
//// ChatListActivity 에서 채팅방 카테고리에 대한 리사이클러뷰 어댑터 (가로)
//public class RecyclerChatCategoryAdapter extends RecyclerView.Adapter<RecyclerChatCategoryAdapter.ViewHolder> {
//    ArrayList<DataSnapshot> chatDataSnapShotList = null;
//    String uid;
//
//    // 생성자를 통해 목록에 나타낼 데이터를 전달 받는다
//    public RecyclerChatCategoryAdapter(ArrayList<DataSnapshot> chatDataSnapShotList, String uid) {
//        this.chatDataSnapShotList = chatDataSnapShotList;
//        this.uid = uid;
//    }
//
//    // 아이템 뷰를 저장하는 뷰홀더 클래스
//    public class ViewHolder extends RecyclerView.ViewHolder {
//
//        // 카테고리목록 리사이클러뷰(가로)에 채팅목록 리사이클러뷰(세로)를 중첩하기 위해서는
//        // 현 위치 뷰 홀더클래스에서 중첩할 리사이클러뷰를 정의한다
//        RecyclerView chatRoomRecyclerview;
//        RecyclerChatRoomAdapter chatRoomAdapter;
//
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            // 뷰 객체에 대한 참조
//            // 중첩할 채팅목록 리사이클러뷰 정의
//            chatRoomRecyclerview = itemView.findViewById(R.id.chatRoomRecyclerview);
//            // 액티비티에서(ChatListActivity) 전달받은 채팅목록 데이터를
//            // 목록을 표기할 리사이클러뷰(중첩된)로 한번 더 전달한다
////            chatRoomAdapter = new RecyclerChatRoomAdapter(chatDataSnapShotList);
////            chatRoomRecyclerview.setAdapter(chatRoomAdapter);
////            chatRoomRecyclerview.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
//        }
//    }
//
//    // 리사이클러뷰의 아이템을 만든다
//    // 아이템은 아이템 레이아웃 xml 으로 뷰를 만들고, 해당 뷰를 뷰홀더 객체에 담아 만들어진다
//    @NonNull
//    @Override
//    public RecyclerChatCategoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        Context chatListActivityContext = parent.getContext();
//        LayoutInflater inflater = (LayoutInflater) chatListActivityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//
//        View view = inflater.inflate(R.layout.chat_room_category_item, parent,false);
//        RecyclerChatCategoryAdapter.ViewHolder viewHolder = new RecyclerChatCategoryAdapter.ViewHolder(view);
//
//        return viewHolder;
//    }
//
//    // 만들어진 아이템에 보여줄 데이터를 반영한다
//    @Override
//    public void onBindViewHolder(@NonNull RecyclerChatCategoryAdapter.ViewHolder holder, int position) {
////        holder.textView.setText("안녕하세요" + position);
//        holder.chatRoomAdapter = new RecyclerChatRoomAdapter(chatDataSnapShotList, uid, true);
//        holder.chatRoomRecyclerview.setAdapter(holder.chatRoomAdapter);
//        holder.chatRoomRecyclerview.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
//    }
//
//    // 전체 데이터(아이템) 개수를 리턴
//    @Override
//    public int getItemCount() {
//        return 2;
//    }
//}
