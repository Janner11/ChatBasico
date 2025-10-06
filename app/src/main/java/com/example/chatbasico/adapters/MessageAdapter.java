package com.example.chatbasico.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatbasico.R;
import com.example.chatbasico.models.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private FirebaseFirestore db;
    private DatabaseReference statusRef; // Referencia para el estado de conexión

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        this.db = FirebaseFirestore.getInstance();
        // Apunta a la raíz de los estados de conexión en Realtime Database
        this.statusRef = FirebaseDatabase.getInstance().getReference("status");
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

        // Cargar nombre desde Firestore
        db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        holder.tvSender.setText(document.getString("name"));
                    } else {
                        holder.tvSender.setText("Desconocido");
                    }
                })
                .addOnFailureListener(e -> holder.tvSender.setText("Error"));

        // Observar el estado de conexión del remitente desde Realtime Database
        statusRef.child(message.getSenderId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && "online".equals(snapshot.getValue(String.class))) {
                    holder.ivStatus.setImageDrawable(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_status_online));
                } else {
                    holder.ivStatus.setImageDrawable(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_status_offline));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // En caso de error, mostrar como desconectado
                holder.ivStatus.setImageDrawable(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_status_offline));
            }
        });


        // Lógica para mostrar imagen o texto
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            holder.tvMessage.setVisibility(View.GONE);
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(message.getImageUrl())
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(message.getText());
        }

        // Formatear timestamp
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
        ImageView ivImage, ivStatus; // Añadido ivStatus

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivImage = itemView.findViewById(R.id.ivImage);
            ivStatus = itemView.findViewById(R.id.ivStatus); // Inicializar ivStatus
        }
    }
}
