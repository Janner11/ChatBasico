package com.example.chatbasico.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbasico.R;
import com.example.chatbasico.models.Message;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private FirebaseFirestore db;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        this.db = FirebaseFirestore.getInstance(); // inicializar Firestore
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        // Cargar nombre desde Firestore usando senderId
        db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        holder.tvSender.setText(document.getString("name")); // mostrar nombre real
                    } else {
                        holder.tvSender.setText("Desconocido");
                    }
                })
                .addOnFailureListener(e -> holder.tvSender.setText("Error"));

        // Mostrar mensaje
        holder.tvMessage.setText(message.getText());

        // Convertir timestamp a hora legible
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(message.getTimestamp());
        holder.tvTimestamp.setText(time);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTimestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
