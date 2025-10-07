package com.example.chatbasico;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbasico.adapters.MessageAdapter;
import com.example.chatbasico.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference chatRef;
    private CollectionReference messagesRef;
    private DatabaseReference currentUserStatusRef;
    private DatabaseReference otherUserStatusRef; // Para escuchar el estado del otro usuario
    private ValueEventListener otherUserStatusListener; // Listener para poder quitarlo después

    private RecyclerView recyclerMessages;
    private MessageAdapter adapter;
    private List<Message> messageList;
    private EditText etMessage;
    private Button btnSend;
    private TextView toolbarTitle;
    private ImageView toolbarStatus;

    private ImageButton btnAttachImage;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "El permiso de notificación es necesario...", Toast.LENGTH_LONG).show();
                }
            });

    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        chatId = getIntent().getStringExtra("chatId");
        if (chatId == null || chatId.isEmpty()) {
            finish();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Ocultamos el título por defecto

        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarStatus = findViewById(R.id.toolbar_status);

        chatRef = db.collection("chats").document(chatId);
        messagesRef = chatRef.collection("messages");
        setupPresenceSystem();
        askNotificationPermission();

        recyclerMessages = findViewById(R.id.recyclerMessages);
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttachImage = findViewById(R.id.btnAttachImage);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        uploadImageToFirebase(result.getData().getData());
                    }
                }
        );

        btnAttachImage.setOnClickListener(v -> openGallery());
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text, null);
            }
        });

        listenMessages();
        setupToolbar();
    }

    private void setupToolbar() {
        chatRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String chatName = documentSnapshot.getString("name");
                List<String> userIds = (List<String>) documentSnapshot.get("userIds");

                if (chatName != null && !chatName.isEmpty()) { // Chat grupal
                    toolbarTitle.setText(chatName);
                    toolbarStatus.setVisibility(View.GONE);
                } else { // Chat individual
                    String currentUserId = mAuth.getCurrentUser().getUid();
                    String otherUserId = null;
                    for (String userId : userIds) {
                        if (!userId.equals(currentUserId)) {
                            otherUserId = userId;
                            break;
                        }
                    }
                    if (otherUserId != null) {
                        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                toolbarTitle.setText(userDoc.getString("name"));
                            }
                        });
                        listenToOtherUserStatus(otherUserId);
                    }
                }
            }
        });
    }
    
    private void listenToOtherUserStatus(String otherUserId) {
        otherUserStatusRef = FirebaseDatabase.getInstance().getReference("status/").child(otherUserId);
        toolbarStatus.setVisibility(View.VISIBLE);

        otherUserStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && "online".equals(snapshot.getValue(String.class))) {
                    toolbarStatus.setImageResource(R.drawable.ic_status_online);
                } else {
                    toolbarStatus.setImageResource(R.drawable.ic_status_offline);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                 toolbarStatus.setImageResource(R.drawable.ic_status_offline);
            }
        };
        otherUserStatusRef.addValueEventListener(otherUserStatusListener);
    }

    private void sendMessage(String text, String imageUrl) {
        String senderId = mAuth.getCurrentUser().getUid();
        Message msg = new Message(text, senderId, System.currentTimeMillis());
        if (imageUrl != null) {
            msg.setImageUrl(imageUrl);
            text = "Imagen";
        }

        String finalText = text;
        messagesRef.add(msg).addOnSuccessListener(doc -> {
            etMessage.setText("");
            chatRef.update("lastMessage", finalText);
        });
    }

    private void setupPresenceSystem() {
        currentUserStatusRef = FirebaseDatabase.getInstance().getReference("status/" + mAuth.getCurrentUser().getUid());
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue(Boolean.class)) {
                    currentUserStatusRef.setValue("online");
                    currentUserStatusRef.onDisconnect().setValue("offline");
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuth.getCurrentUser() != null) {
            currentUserStatusRef.setValue("offline");
        }
        // Detener el listener del otro usuario para evitar fugas de memoria
        if (otherUserStatusRef != null && otherUserStatusListener != null) {
            otherUserStatusRef.removeEventListener(otherUserStatusListener);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void listenMessages() {
        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Message msg = dc.getDocument().toObject(Message.class);
                            messageList.add(msg);
                            adapter.notifyItemInserted(messageList.size() - 1);
                            recyclerMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void uploadImageToFirebase(Uri imageUri) {
        String fileName = "img_" + System.currentTimeMillis();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("chat_images/" + fileName);

        storageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> sendMessage(null, uri.toString())))
                .addOnFailureListener(e -> Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
