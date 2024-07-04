package com.example.suivinfc;

import androidx.appcompat.app.AppCompatActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView txtNFCData;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        txtNFCData = findViewById(R.id.txtNFCData);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show();
            finish();
        }

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        Button btnScanNFC = findViewById(R.id.btnScanNFC);
        btnScanNFC.setOnClickListener(view -> enableForegroundDispatchSystem());

        Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(view -> resetData());
    }

    private void enableForegroundDispatchSystem() {
        IntentFilter[] intentFilters = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void resetData() {
        txtNFCData.setText("NFC Data will appear here");
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processNfcIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            processNfcIntent(intent);
        }
    }

    private void processNfcIntent(Intent intent) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages != null && rawMessages.length > 0) {
            NdefMessage message = (NdefMessage) rawMessages[0];
            NdefRecord[] records = message.getRecords();
            for (NdefRecord record : records) {
                if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    byte[] payload = record.getPayload();
                    String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
                    int languageCodeLength = payload[0] & 0077;
                    try {
                        String nfcData = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
                        txtNFCData.setText(nfcData);
                        sendTokenToServer(nfcData); // Send token to server
                    } catch (Exception e) {
                        txtNFCData.setText("Error reading text.");
                        Log.e(TAG, "Error reading NFC text: ", e);
                    }
                }
            }
        } else {
            txtNFCData.setText("No NDEF messages found.");
        }
    }

    private void sendTokenToServer(String token) {
        APIInterface apiService = RetrofitClient.getClient().create(APIInterface.class);
        Call<TokenResponse> call = apiService.verifyToken(new TokenRequest(token));
        call.enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MainActivity.this, "Server Response: " + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Server Response: " + response.body().getMessage());

                    // Store user information in local storage (SharedPreferences)
                    SharedPreferences sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("token", token);
                    editor.putString("user_data", new Gson().toJson(response.body().getMessage()));
                    editor.apply();

                    // Open browser with token parameter
                    openBrowser(token);
                } else {
                    Toast.makeText(MainActivity.this, "Token verification failed.", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Token verification failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error connecting to server: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error connecting to server: ", t);
            }
        });
    }

    private void openBrowser(String token) {
        try {
            String encodedToken = URLEncoder.encode(token, "UTF-8");
            String url = "http://172.20.10.4:3000?token=" + encodedToken;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "No application can handle this request. Please install a web browser.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "No application can handle this request. Please install a web browser.");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error encoding token.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error encoding token: ", e);
        }
    }

}
