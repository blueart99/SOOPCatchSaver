
package com.soop.catchsaver;

import android.app.Activity;
import android.app.DownloadManager;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    EditText urlInput;
    TextView status;
    Button resolveButton;
    Button downloadButton;

    String resolvedVideoUrl = "";
    String sourceUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(18, 20, 27));
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("SOOP Catch Saver");
        title.setTextSize(27);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("비로그인 공개 영상 주소 확인");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setHint("SOOP 캐치 URL 입력");
        urlInput.setText("https://vod.sooplive.com/player/211051473/catch");

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(-1, -2);
        inputParams.topMargin = 30;
        layout.addView(urlInput, inputParams);

        resolveButton = new Button(this);
        resolveButton.setText("영상 주소 확인");
        layout.addView(resolveButton);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setPadding(0, 24, 0, 24);
        status.setText("캐치 주소를 확인할 준비가 됐습니다.");
        layout.addView(status);

        Button openButton = new Button(this);
        openButton.setText("SOOP 영상 열기");
        layout.addView(openButton);

        downloadButton = new Button(this);
        downloadButton.setText("MP4 다운로드");
        downloadButton.setEnabled(false);
        layout.addView(downloadButton);

        resolveButton.setOnClickListener(v -> resolveVideo());

        openButton.setOnClickListener(v -> {
            String input = urlInput.getText().toString().trim();

            if (!isSoopUrl(input)) {
                status.setText("올바른 SOOP 주소를 입력해주세요.");
                return;
            }

            try {
                IntentHelper.open(this, input);
            } catch (Exception e) {
                status.setText("영상을 열 수 없습니다.");
            }
        });

        downloadButton.setOnClickListener(v -> downloadVideo());

        setContentView(scroll);
    }

    private void resolveVideo() {
        String input = urlInput.getText().toString().trim();

        if (!isSoopUrl(input)) {
            status.setText("올바른 SOOP 주소가 아닙니다.");
            return;
        }

        String id = extractVideoId(input);

        if (id == null) {
            status.setText("영상 번호를 확인할 수 없습니다.");
            return;
        }

        sourceUrl = input;
        resolvedVideoUrl = "";
        downloadButton.setEnabled(false);
        resolveButton.setEnabled(false);
        status.setText("SOOP 서버에 영상 주소를 요청하고 있습니다...");

        new Thread(() -> {
            try {
                JSONObject response;

                if (input.toLowerCase().contains("/catch")) {
                    response = requestApi(
                            "https://api.m.sooplive.co.kr/station/video/a/catchview",
                            id,
                            true
                    );

                    if (response == null) {
                        response = requestApi(
                                "https://api.m.sooplive.co.kr/station/video/a/view",
                                id,
                                false
                        );
                    }
                } else {
                    response = requestApi(
                            "https://api.m.sooplive.co.kr/station/video/a/view",
                            id,
                            false
                    );
                }

                if (response == null) {
                    throw new Exception("SOOP 서버 응답이 없습니다.");
                }

                String media = findMediaUrl(response);

                if (media == null) {
                    throw new Exception(
                            "서버 응답에서 다운로드 가능한 영상 주소를 찾지 못했습니다."
                    );
                }

                final String found = media;

                runOnUiThread(() -> {
                    resolvedVideoUrl = found;

                    if (found.toLowerCase().contains(".m3u8")) {
                        status.setText(
                                "영상 주소 확인 완료!\n" +
                                "영상 형식: HLS 스트리밍\n" +
                                "현재 버전은 MP4 직접 다운로드만 지원합니다."
                        );
                    } else {
                        downloadButton.setEnabled(true);
                        status.setText(
                                "영상 주소 확인 완료!\n" +
                                "영상 형식: MP4\n" +
                                "다운로드 버튼을 눌러주세요."
                        );
                    }

                    resolveButton.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText(
                            "영상 주소 확인 실패\n" +
                            e.getMessage()
                    );
                    resolveButton.setEnabled(true);
                });
            }
        }).start();
    }

    private JSONObject requestApi(
            String api,
            String id,
            boolean catchApi
    ) throws Exception {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(api);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
            );
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 17) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36"
            );
            connection.setRequestProperty(
                    "Origin",
                    "https://www.sooplive.co.kr"
            );
            connection.setRequestProperty(
                    "Referer",
                    sourceUrl
            );

            String fields;

            if (catchApi) {
                fields =
                        "nTitleNo=" + URLEncoder.encode(id, "UTF-8") +
                        "&nPageNo=1&nLimit=20";
            } else {
                fields =
                        "nTitleNo=" + URLEncoder.encode(id, "UTF-8") +
                        "&nApiLevel=10&nPlaylistIdx=0";
            }

            try (OutputStream output = connection.getOutputStream()) {
                output.write(fields.getBytes("UTF-8"));
                output.flush();
            }

            int code = connection.getResponseCode();

            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (stream == null) {
                throw new Exception("HTTP 오류: " + code);
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int count;

            while ((count = stream.read(data)) != -1) {
                buffer.write(data, 0, count);
            }

            stream.close();

            String result = buffer.toString("UTF-8");

            if (code < 200 || code >= 300) {
                throw new Exception("SOOP 서버 HTTP 오류: " + code);
            }

            return new JSONObject(result);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String findMediaUrl(JSONObject object) {
        return findMediaInJson(object);
    }

    private String findMediaInJson(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Iterator<String> keys = object.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object child = object.opt(key);

                if (child instanceof String) {
                    String candidate = (String) child;

                    if (isMediaUrl(candidate)) {
                        return candidate;
                    }
                }

                String found = findMediaInJson(child);

                if (found != null) {
                    return found;
                }
            }
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;

            for (int i = 0; i < array.length(); i++) {
                String found = findMediaInJson(array.opt(i));

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean isMediaUrl(String value) {
        if (value == null) return false;

        String lower = value.toLowerCase();

        return lower.startsWith("https://")
                && (lower.contains(".mp4")
                || lower.contains(".m3u8"));
    }

    private void downloadVideo() {
        if (resolvedVideoUrl.isEmpty()) {
            status.setText("먼저 영상 주소를 확인해주세요.");
            return;
        }

        if (resolvedVideoUrl.toLowerCase().contains(".m3u8")) {
            status.setText(
                    "이 영상은 HLS 형식입니다.\n" +
                    "HLS 다운로드 기능은 아직 추가되지 않았습니다."
            );
            return;
        }

        try {
            Uri uri = Uri.parse(resolvedVideoUrl);

            String filename = "SOOP_Catch_" +
                    extractVideoId(sourceUrl) + ".mp4";

            DownloadManager manager =
                    (DownloadManager) getSystemService(
                            Context.DOWNLOAD_SERVICE
                    );

            DownloadManager.Request request =
                    new DownloadManager.Request(uri);

            request.setTitle(filename);
            request.setDescription("SOOP 영상 다운로드");
            request.setMimeType("video/mp4");

            request.addRequestHeader("Referer", sourceUrl);
            request.addRequestHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 17) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36"
            );

            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    filename
            );

            manager.enqueue(request);

            status.setText(
                    "다운로드 요청을 보냈습니다.\n" +
                    "알림창에서 진행 상태를 확인해주세요."
            );

        } catch (Exception e) {
            status.setText("다운로드 요청 실패: " + e.getMessage());
        }
    }

    private String extractVideoId(String input) {
        Pattern pattern = Pattern.compile(
                "/player/(\\d+)"
        );

        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private boolean isSoopUrl(String input) {
        try {
            Uri uri = Uri.parse(input);
            String host = uri.getHost();

            return ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme()))
                    && host != null
                    && (host.equals("sooplive.com")
                    || host.endsWith(".sooplive.com")
                    || host.equals("sooplive.co.kr")
                    || host.endsWith(".sooplive.co.kr"));

        } catch (Exception e) {
            return false;
        }
    }

    private static class IntentHelper {
        static void open(Activity activity, String url) {
            android.content.Intent intent =
                    new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            activity.startActivity(intent);
        }
    }
}
