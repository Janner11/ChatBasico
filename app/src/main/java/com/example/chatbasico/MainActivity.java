package com.example.chatbasico;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.chatbasico.adapters.MessageAdapter;
import com.example.chatbasico.models.Message;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private RecyclerView recyclerMessages;
    private MessageAdapter adapter;
    private List<Message> messageList;
    private EditText etMessage;
    private Button btnSend, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

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

        // Botón enviar (por ahora solo agrega al RecyclerView localmente)
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                Message msg = new Message(text, mAuth.getCurrentUser().getEmail(), System.currentTimeMillis());
                messageList.add(msg);
                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerMessages.scrollToPosition(messageList.size() - 1);
                etMessage.setText("");
            }
        });

        // Botón logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
