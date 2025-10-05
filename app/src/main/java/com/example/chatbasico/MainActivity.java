package com.example.chatbasico;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.chatbasico.adapters.MessageAdapter;
import com.example.chatbasico.models.Message;
import com.google.firebase.FirebaseApp;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebasez
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        messagesRef = db.collection("messages");

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

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
        //Para registrar el callback por el resultado de la galeria
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        //se seleccionó la imagen con éxito
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            //llamar la función para subir imagen al firebase
                            uploadImageToFirebase(imageUri);
                        }
                    }
                }
        );
        //configurar el click del botón
        btnAttachImage.setOnClickListener(v -> {
            openGallery();
        });

        // Botón enviar (guardar en Firestore)
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                // Guardar el UID del usuario, no el correo
                String senderId = mAuth.getCurrentUser().getUid();

                Message msg = new Message(
                        text,
                        senderId,
                        System.currentTimeMillis()
                );

                messagesRef.add(msg)
                        .addOnSuccessListener(doc -> etMessage.setText(""))
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        // Botón logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Escuchar mensajes en tiempo real
        listenMessages();
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
                        Toast.makeText(this, "Error al cargar mensajes", Toast.LENGTH_SHORT).show();
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
        StorageReference storageReference = FirebaseStorage.getInstance()
                .getReference("chat_images/" + fileName);

        storageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        sendMessageWithImage(imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al subir la imagen a Firebase Storage", e);
                    Toast.makeText(this, "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendMessageWithImage(String imageUrl) {
        String senderId = mAuth.getCurrentUser().getUid();
        // El texto puede ser un string vacío o un texto predeterminado
        Message msg = new Message(
                "Imagen",
                senderId,
                System.currentTimeMillis()
        );
        msg.setImageUrl(imageUrl); // Necesitamos agregar este campo en el modelo Message

        messagesRef.add(msg)
                .addOnSuccessListener(doc -> {
                    // Opcional: limpiar el EditText si es necesario
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al enviar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
