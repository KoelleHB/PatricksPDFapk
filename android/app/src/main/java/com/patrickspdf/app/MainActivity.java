package com.patrickspdf.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "PatricksPDF_Native";
    private volatile String pendingPdfJson = null;

    public class NativePdfBridgeInterface {
        @JavascriptInterface
        public String getPendingPdf() {
            String res = pendingPdfJson;
            pendingPdfJson = null;
            return res;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Expose NativePdfBridge to JavaScript
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().addJavascriptInterface(new NativePdfBridgeInterface(), "NativePdfBridge");
        }

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Incoming intent action: " + action);

        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action) || Intent.ACTION_EDIT.equals(action)) {
            Uri targetUri = null;

            // Check EXTRA_STREAM first (used by Share Sheet)
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        targetUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
                    } else {
                        targetUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error getting EXTRA_STREAM: " + e.getMessage());
                }
            }

            // Check intent.getData() (used by "Open with..." / VIEW)
            if (targetUri == null) {
                targetUri = intent.getData();
            }

            // Fallback to ClipData
            if (targetUri == null && intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                targetUri = intent.getClipData().getItemAt(0).getUri();
            }

            if (targetUri != null) {
                readAndDeliverPdfUri(targetUri);
            }
        }
    }

    private void readAndDeliverPdfUri(Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Processing PDF URI: " + uri);

                    // Resolve display name
                    String fileName = "document.pdf";
                    try {
                        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                if (nameIndex >= 0) {
                                    String n = cursor.getString(nameIndex);
                                    if (n != null && !n.trim().isEmpty()) {
                                        fileName = n;
                                    }
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Could not query display name: " + e.getMessage());
                    }

                    if ("document.pdf".equals(fileName) && uri.getLastPathSegment() != null) {
                        String last = uri.getLastPathSegment();
                        if (last.toLowerCase().endsWith(".pdf")) {
                            fileName = last;
                        } else {
                            fileName = last + ".pdf";
                        }
                    }

                    // Read bytes directly from ContentResolver with granted URI permissions
                    InputStream is = getContentResolver().openInputStream(uri);
                    if (is == null) {
                        Log.e(TAG, "ContentResolver returned null InputStream for " + uri);
                        return;
                    }

                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[32768];
                    int nRead;
                    while ((nRead = is.read(chunk, 0, chunk.length)) != -1) {
                        buffer.write(chunk, 0, nRead);
                    }
                    buffer.flush();
                    is.close();

                    byte[] pdfBytes = buffer.toByteArray();
                    Log.d(TAG, "Read " + pdfBytes.length + " bytes for " + fileName);

                    String base64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP);

                    JSONObject json = new JSONObject();
                    json.put("name", fileName);
                    json.put("base64", base64);
                    final String jsonStr = json.toString();

                    pendingPdfJson = jsonStr;

                    // Forward to WebView via javascript
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getBridge().getWebView().evaluateJavascript(
                                    "window.onNativePdfReceived && window.onNativePdfReceived(" + jsonStr + ");",
                                    null
                                );
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed reading PDF from URI: " + e.getMessage(), e);
                }
            }
        }).start();
    }
}
