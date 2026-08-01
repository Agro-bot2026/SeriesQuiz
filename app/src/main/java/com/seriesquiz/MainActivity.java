package com.seriesquiz;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private static final String AD_UNIT_ID = "ca-app-pub-4478373683231277/6306006128";
    private static final String LEADERBOARD_ID = "CgkI3Jj6kowbEAIQAQ";
    private WebView webView;
    private InterstitialAd interstitialAd;
    private boolean showAdOnFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PlayGamesSdk.initialize(this);
        attemptPlayGamesSignIn();
        checkAppIntegrity();

        MobileAds.initialize(this, initializationStatus -> {});
        loadInterstitialAd();

        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                result.confirm();
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                view.loadData("<html><body style='background:#12071f;color:#ff6;padding:40px;font-size:18px'><h2>Error " + errorCode + "</h2><p>" + description + "</p></body></html>", "text/html", "UTF-8");
            }
        });

        try {
            InputStream is = getAssets().open("index.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            webView.loadDataWithBaseURL("https://localhost/", sb.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            String err = e.getMessage() != null ? e.getMessage() : "Unknown error";
            webView.loadData("<html><body style='background:#12071f;color:white;padding:40px;text-align:center;font-size:18px'><h2>Error</h2><p>" + err + "</p></body></html>", "text/html", "UTF-8");
        }
    }

    private void attemptPlayGamesSignIn() {
        GamesSignInClient signInClient = PlayGames.getGamesSignInClient(this);
        signInClient.signIn();
    }

    private void checkAppIntegrity() {
        try {
            IntegrityManager integrityManager = IntegrityManagerFactory.create(this);
            byte[] nonceBytes = new byte[16];
            new java.security.SecureRandom().nextBytes(nonceBytes);
            String nonce = android.util.Base64.encodeToString(nonceBytes,
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);

            IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(930973912156L)
                .build();

            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(response -> sendIntegrityTokenToBackend(response.token()))
                .addOnFailureListener(e -> android.util.Log.w("PlayIntegrity", "No se pudo obtener el token", e));
        } catch (Exception e) {
            android.util.Log.w("PlayIntegrity", "Error iniciando chequeo de integridad", e);
        }
    }

    private void sendIntegrityTokenToBackend(String token) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://seriesquiz-api.charly-tricks.dev/api/verify-integrity");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                String body = "{\"token\":\"" + token + "\"}";
                conn.getOutputStream().write(body.getBytes("UTF-8"));
                int code = conn.getResponseCode();
                java.io.InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                android.util.Log.d("PlayIntegrity", "Verificacion (" + code + "): " + responseBody);
            } catch (Exception e) {
                android.util.Log.w("PlayIntegrity", "No se pudo verificar con el backend", e);
            }
        }).start();
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        interstitialAd = null;
                        loadInterstitialAd();
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        interstitialAd = null;
                        loadInterstitialAd();
                    }
                });
                if (showAdOnFirstLoad) {
                    showAdOnFirstLoad = false;
                    showInterstitialIfReady();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAd = null;
            }
        });
    }

    private void showInterstitialIfReady() {
        if (interstitialAd != null) {
            interstitialAd.show(MainActivity.this);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showAd() {
            runOnUiThread(() -> showInterstitialIfReady());
        }

        @JavascriptInterface
        public void shareScore(String text) {
            runOnUiThread(() -> {
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                startActivity(android.content.Intent.createChooser(shareIntent, "Compartir puntaje"));
            });
        }

        @JavascriptInterface
        public void checkPlayGamesSignedIn(final String jsCallback) {
            runOnUiThread(() -> {
                GamesSignInClient client = PlayGames.getGamesSignInClient(MainActivity.this);
                client.isAuthenticated().addOnCompleteListener(task -> {
                    boolean isAuth = task.isSuccessful() && task.getResult().isAuthenticated();
                    webView.evaluateJavascript(jsCallback + "(" + isAuth + ")", null);
                });
            });
        }

        @JavascriptInterface
        public void playGamesSignIn() {
            runOnUiThread(() -> {
                GamesSignInClient client = PlayGames.getGamesSignInClient(MainActivity.this);
                client.signIn();
            });
        }

        @JavascriptInterface
        public void submitToPlayGamesLeaderboard(long score) {
            runOnUiThread(() -> {
                LeaderboardsClient client = PlayGames.getLeaderboardsClient(MainActivity.this);
                client.submitScore(LEADERBOARD_ID, score);
            });
        }

        @JavascriptInterface
        public void showPlayGamesLeaderboard() {
            runOnUiThread(() -> {
                LeaderboardsClient client = PlayGames.getLeaderboardsClient(MainActivity.this);
                client.getLeaderboardIntent(LEADERBOARD_ID)
                    .addOnSuccessListener(intent -> startActivityForResult(intent, 9004))
                    .addOnFailureListener(e ->
                        webView.evaluateJavascript("showModal('🎮 Google Play Games','Inicia sesi\\u00f3n con tu cuenta de Google primero.')", null)
                    );
            });
        }
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript(
            "(function(){return document.getElementById('menuScreen').classList.contains('active');})()",
            value -> {
                if ("true".equals(value)) {
                    MainActivity.super.onBackPressed();
                } else {
                    webView.evaluateJavascript("showMenu()", null);
                }
            }
        );
    }
}
