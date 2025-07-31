package com.example.chatchat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.*;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ChatClient chatClient;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // ваш layout

        // 1) Подготовить RecyclerView для отображения сообщений
        RecyclerView rv = findViewById(R.id.rv_messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        // 2) Инпут и кнопка
        EditText etInput = findViewById(R.id.et_input);
        Button btnSend = findViewById(R.id.btn_send);

        // Готовим файлы
        File caCertFile;
        File clientCertFile;
        File clientKeyFile;
//        try {
//            caCertFile = new File(getFilesDir(), "ca_cert.pem");
//            clientCertFile = new File(getFilesDir(), "client_cert.pem");
//            clientKeyFile = new File(getFilesDir(), "client_key.pem");
//
//            copyRawResourceToFile(R.raw.ca_cert, caCertFile);
//            copyRawResourceToFile(R.raw.client_cert, clientCertFile);
//            copyRawResourceToFile(R.raw.client_key, clientKeyFile);
//
//            Log.d("MainActivity", "CA size: " + caCertFile.length());
//            Log.d("MainActivity", "Client cert size: " + clientCertFile.length());
//            Log.d("MainActivity", "Client key size: " + clientKeyFile.length());
//
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }



        chatClient = new ChatClient(
                "levchatproject.duckdns.org",
                5001,
                new ChatClient.Listener() {
                    @Override
                    public void onConnected() {
                        runOnUiThread(() -> adapter.add("SYSTEM: Connected"));
                    }

                    @Override
                    public void onMessageReceived(String msg) {
                        runOnUiThread(() -> adapter.add("Friend: " + msg));
                    }

                    @Override
                    public void onError(Throwable cause) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "Error: " + cause.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }
                }
        );

        chatClient.connect();

        // 4) Отправка по нажатию
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                chatClient.sendMessage(text);
                runOnUiThread(() -> adapter.add("Me: " + text));
                etInput.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatClient.disconnect();
    }

    private void copyRawResourceToFile(@RawRes int rawResId, File outFile) throws IOException {
        if (outFile.exists()) return;
        try (InputStream is = getResources().openRawResource(rawResId);
             OutputStream os = new FileOutputStream(outFile)) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) {
                os.write(buf, 0, r);
            }
        }
    }

}
