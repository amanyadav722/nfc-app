package com.example.suivinfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView txtNFCData;

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

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        Button btnScanNFC = findViewById(R.id.btnScanNFC);
        btnScanNFC.setOnClickListener(view -> enableForegroundDispatchSystem());
    }

    private void enableForegroundDispatchSystem() {
        IntentFilter[] intentFilters = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
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
                        txtNFCData.setText(new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding));
                    } catch (Exception e) {
                        txtNFCData.setText("Error reading text.");
                    }
                }
            }
        } else {
            txtNFCData.setText("No NDEF messages found.");
        }
    }
}
