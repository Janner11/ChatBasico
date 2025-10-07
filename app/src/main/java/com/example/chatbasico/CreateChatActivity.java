package com.example.chatbasico;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbasico.adapters.UserAdapter;
import com.example.chatbasico.models.Chat;
import com.example.chatbasico.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CreateChatActivity extends AppCompatActivity {

    private static final String TAG = "CreateChatActivity";
    private Toolbar toolbar;
    private RecyclerView recyclerUsers;
    private Button btnCreateChat;

    private UserAdapter adapter;
    private List<User> userList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerUsers = findViewById(R.id.recyclerUsers);
        btnCreateChat = findViewById(R.id.btnCreateChat);

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList, this);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        loadUsers();

        btnCreateChat.setOnClickListener(v -> createChat());
    }

    private void loadUsers() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    if (!document.getId().equals(currentUserId)) {
                        User user = document.toObject(User.class);
                        user.setId(document.getId());
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void createChat() {
        List<User> selectedUsers = adapter.getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> userIds = new ArrayList<>();
        userIds.add(mAuth.getCurrentUser().getUid());
        for (User user : selectedUsers) {
            userIds.add(user.getId());
        }

        if (selectedUsers.size() > 1) {
            // Chat grupal: pedir un nombre
            promptForGroupName(userIds);
        } else {
            // Chat individual: crearlo directamente sin nombre
            createChatDocument(null, userIds);
        }
    }

    private void promptForGroupName(List<String> userIds) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nombre del Grupo");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            String groupName = input.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, "El nombre del grupo no puede estar vacío", Toast.LENGTH_SHORT).show();
            } else {
                createChatDocument(groupName, userIds);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createChatDocument(String name, List<String> userIds) {
        Chat newChat = new Chat();
        newChat.setUserIds(userIds);
        newChat.setLastMessage("Chat creado");
        if (name != null) { // Solo se guarda el nombre si es un grupo
            newChat.setName(name);
        }

        db.collection("chats")
                .add(newChat)
                .addOnSuccessListener(documentReference -> {
                    String chatId = documentReference.getId();
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al crear el chat", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
