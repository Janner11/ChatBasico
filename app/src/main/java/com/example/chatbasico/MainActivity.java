package com.example.chatbasico;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbasico.adapters.MessageAdapter;
import com.example.chatbasico.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference messagesRef;

    private RecyclerView recyclerMessages;
    private MessageAdapter adapter;
    private List<Message> messageList;
    private EditText etMessage;
    private Button btnSend, btnLogout;

    private ImageButton btnAttachImage;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            // El permiso fue concedido
        } else {
            Toast.makeText(this, "El permiso de notificación es necesario para recibir alertas de mensajes.", Toast.LENGTH_LONG).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        messagesRef = db.collection("messages");

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        askNotificationPermission();

        // Configurar RecyclerView
        recyclerMessages = findViewById(R.id.recyclerMessages);
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnLogout = findViewById(R.id.btnLogout);

        btnAttachImage = findViewById(R.id.btnAttachImage);
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageToFirebase(imageUri);
                        }
                    }
                }
        );

        btnAttachImage.setOnClickListener(v -> openGallery());

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                String senderId = mAuth.getCurrentUser().getUid();
                Message msg = new Message(text, senderId, System.currentTimeMillis());
                messagesRef.add(msg).addOnSuccessListener(doc -> etMessage.setText(""));
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        listenMessages();
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
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
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
                .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(this::sendMessageWithImage))
                .addOnFailureListener(e -> Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show());
    }

    private void sendMessageWithImage(Uri imageUri) {
        String imageUrl = imageUri.toString();
        String senderId = mAuth.getCurrentUser().getUid();
        Message msg = new Message("Imagen", senderId, System.currentTimeMillis());
        msg.setImageUrl(imageUrl);
        messagesRef.add(msg);
    }
}
