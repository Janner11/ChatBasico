package com.example.chatbasico.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbasico.ChatActivity;
import com.example.chatbasico.R;
import com.example.chatbasico.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public ChatAdapter(List<Chat> chatList, Context context) {
        this.chatList = chatList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Lógica para mostrar el nombre del chat
        if (chat.getName() != null && !chat.getName().isEmpty()) {
            // Es un grupo, mostrar el nombre del grupo
            holder.tvChatName.setText(chat.getName());
        } else {
            // Es un chat individual, buscar el nombre del otro usuario
            String currentUserId = mAuth.getCurrentUser().getUid();
            String otherUserId = null;
            for (String userId : chat.getUserIds()) {
                if (!userId.equals(currentUserId)) {
                    otherUserId = userId;
                    break;
                }
            }

            if (otherUserId != null) {
                db.collection("users").document(otherUserId).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        holder.tvChatName.setText(documentSnapshot.getString("name"));
                    }
                });
            }
        }

        holder.tvLastMessage.setText(chat.getLastMessage());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("chatId", chat.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatName, tvLastMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}
