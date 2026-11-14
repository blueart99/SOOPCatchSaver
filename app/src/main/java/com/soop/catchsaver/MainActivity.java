
package com.soop.catchsaver;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {

    EditText urlInput;
    TextView status;
    Button resolveButton, downloadButton;

    String resolvedVideoUrl = "";
    String sourceUrl = "";
    boolean downloading = false;

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
        subtitle.setText("비로그인 공개 영상 다운로드");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setHint("SOOP 캐치 URL 입력");
        urlInput.setText(
                "https://vod.sooplive.com/player/211051473/catch"
        );
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
        downloadButton.setText("영상 다운로드");
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
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(input)
                );
                startActivity(intent);
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
                JSONObject response = null;

                if (input.toLowerCase().contains("/catch")) {
                    try {
                        response = requestApi(
                                "https://api.m.sooplive.co.kr/station/video/a/catchview",
                                id,
                                true
                        );
                    } catch (Exception ignored) {
                        response = null;
                    }

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
                            "다운로드 가능한 영상 주소를 찾지 못했습니다."
                    );
                }

                final String found = media;

                runOnUiThread(() -> {
                    resolvedVideoUrl = found;
                    downloadButton.setEnabled(true);

                    if (found.toLowerCase().contains(".m3u8")) {
                        status.setText(
                                "영상 주소 확인 완료!\n" +
                                "영상 형식: HLS 스트리밍\n" +
                                "영상 다운로드 버튼을 눌러주세요."
                        );
                    } else {
                        status.setText(
                                "영상 주소 확인 완료!\n" +
                                "영상 형식: MP4\n" +
                                "영상 다운로드 버튼을 눌러주세요."
                        );
                    }

                    resolveButton.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText(
                            "영상 주소 확인 실패\n" + e.getMessage()
                    );
                    resolveButton.setEnabled(true);
                });
            }
        }).start();
    }

    private JSONObject requestApi(
            String api, String id, boolean catchApi
    ) throws Exception {

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    new URL(api).openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
            );
            connection.setRequestProperty(
                    "User-Agent", userAgent()
            );
            connection.setRequestProperty(
                    "Origin", "https://www.sooplive.co.kr"
            );
            connection.setRequestProperty("Referer", sourceUrl);

            String fields;

            if (catchApi) {
                fields = "nTitleNo=" +
                        URLEncoder.encode(id, "UTF-8") +
                        "&nPageNo=1&nLimit=20";
            } else {
                fields = "nTitleNo=" +
                        URLEncoder.encode(id, "UTF-8") +
                        "&nApiLevel=10&nPlaylistIdx=0";
            }

            try (OutputStream out = connection.getOutputStream()) {
                out.write(fields.getBytes("UTF-8"));
            }

            int code = connection.getResponseCode();

            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (stream == null) {
                throw new Exception("HTTP 오류: " + code);
            }

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            byte[] data = new byte[8192];
            int count;

            while ((count = stream.read(data)) != -1) {
                buffer.write(data, 0, count);
            }

            stream.close();

            if (code < 200 || code >= 300) {
                throw new Exception("서버 HTTP 오류: " + code);
            }

            return new JSONObject(buffer.toString("UTF-8"));

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
                    if (isMediaUrl(candidate)) return candidate;
                }

                String found = findMediaInJson(child);
                if (found != null) return found;
            }
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;

            for (int i = 0; i < array.length(); i++) {
                String found = findMediaInJson(array.opt(i));
                if (found != null) return found;
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
        if (resolvedVideoUrl.isEmpty() || downloading) {
            return;
        }

        if (resolvedVideoUrl.toLowerCase().contains(".m3u8")) {
            downloadHls();
            return;
        }

        status.setText(
                "현재 MP4 직접 다운로드는 다음 단계에서 연결할 예정입니다."
        );
    }

    private void downloadHls() {
        downloading = true;
        downloadButton.setEnabled(false);
        resolveButton.setEnabled(false);
        status.setText("HLS 재생목록을 분석하고 있습니다...");

        new Thread(() -> {
            Uri savedUri = null;
            File savedFile = null;

            try {
                String playlistUrl = resolvedVideoUrl;

                // 마스터 재생목록이면 가장 높은 대역폭 선택
                for (int depth = 0; depth < 3; depth++) {
                    String playlist = fetchText(playlistUrl);

                    String next = selectHighestVariant(
                            playlist, playlistUrl
                    );

                    if (next == null) break;
                    playlistUrl = next;
                }

                String playlist = fetchText(playlistUrl);

                if (!playlist.contains("#EXT-X-ENDLIST")) {
                    throw new Exception(
                            "완료된 VOD 재생목록이 아닙니다. " +
                            "실시간 스트림은 지원하지 않습니다."
                    );
                }

                if (playlist.contains("#EXT-X-MAP")) {
                    throw new Exception(
                            "이 재생목록은 분할 MP4 형식입니다. " +
                            "현재 버전은 TS 형식만 지원합니다."
                    );
                }

                if (playlist.contains("#EXT-X-BYTERANGE")) {
                    throw new Exception(
                            "부분 범위 재생목록은 현재 지원하지 않습니다."
                    );
                }

                if (hasUnsupportedEncryption(playlist)) {
                    throw new Exception(
                            "암호화된 스트림은 지원하지 않습니다."
                    );
                }

                List<String> segments = new ArrayList<>();
                String[] lines = playlist.split("\\r?\\n");

                for (String line : lines) {
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    segments.add(new URL(
                            new URL(playlistUrl), line
                    ).toString());
                }

                if (segments.isEmpty()) {
                    throw new Exception("영상 조각이 없습니다.");
                }

                String filename = "SOOP_Catch_" +
                        extractVideoId(sourceUrl) + ".ts";

                OutputStream output;

                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues values = new ContentValues();
                    values.put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            filename
                    );
                    values.put(
                            MediaStore.MediaColumns.MIME_TYPE,
                            "video/mp2t"
                    );
                    values.put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                    );
                    values.put(
                            MediaStore.MediaColumns.IS_PENDING, 1
                    );

                    savedUri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            values
                    );

                    if (savedUri == null) {
                        throw new Exception("저장 위치를 만들 수 없습니다.");
                    }

                    output = getContentResolver().openOutputStream(
                            savedUri, "w"
                    );

                } else {
                    File folder = getExternalFilesDir(
                            Environment.DIRECTORY_MOVIES
                    );

                    if (folder == null) {
                        throw new Exception("저장 폴더를 찾을 수 없습니다.");
                    }

                    savedFile = new File(folder, filename);
                    output = new FileOutputStream(savedFile);
                }

                if (output == null) {
                    throw new Exception("파일을 열 수 없습니다.");
                }

                try (OutputStream out = output) {
                    byte[] buffer = new byte[32768];

                    for (int i = 0; i < segments.size(); i++) {
                        final int current = i + 1;
                        final int total = segments.size();

                        runOnUiThread(() -> status.setText(
                                "영상 다운로드 중...\n" +
                                current + " / " + total + " 조각"
                        ));

                        HttpURLConnection connection = openGet(
                                segments.get(i)
                        );

                        try {
                            int code = connection.getResponseCode();

                            if (code < 200 || code >= 300) {
                                throw new Exception(
                                        "영상 조각 다운로드 오류: HTTP " + code
                                );
                            }

                            try (InputStream in =
                                         connection.getInputStream()) {
                                int count;

                                while ((count = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, count);
                                }
                            }
                        } finally {
                            connection.disconnect();
                        }
                    }

                    out.flush();
                }

                if (savedUri != null && Build.VERSION.SDK_INT >= 29) {
                    ContentValues done = new ContentValues();
                    done.put(MediaStore.MediaColumns.IS_PENDING, 0);

                    getContentResolver().update(
                            savedUri, done, null, null
                    );
                }

                runOnUiThread(() -> {
                    status.setText(
                            "다운로드 완료!\n" +
                            "파일: " + filename + "\n" +
                            "저장 위치: Downloads\n" +
                            "파일 형식: MPEG-TS"
                    );
                });

            } catch (Exception e) {
                if (savedUri != null) {
                    getContentResolver().delete(
                            savedUri, null, null
                    );
                }

                if (savedFile != null && savedFile.exists()) {
                    savedFile.delete();
                }

                String message = e.getMessage();

                runOnUiThread(() -> status.setText(
                        "HLS 다운로드 실패\n" +
                        (message == null ? "알 수 없는 오류" : message)
                ));

            } finally {
                downloading = false;

                runOnUiThread(() -> {
                    downloadButton.setEnabled(
                            !resolvedVideoUrl.isEmpty()
                    );
                    resolveButton.setEnabled(true);
                });
            }
        }).start();
    }

    private String selectHighestVariant(
            String playlist, String baseUrl
    ) throws Exception {

        String[] lines = playlist.split("\\r?\\n");

        long bestBandwidth = -1;
        String bestUrl = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (!line.startsWith("#EXT-X-STREAM-INF:")) {
                continue;
            }

            Matcher matcher = Pattern.compile(
                    "BANDWIDTH=(\\d+)"
            ).matcher(line);

            long bandwidth = 0;

            if (matcher.find()) {
                bandwidth = Long.parseLong(matcher.group(1));
            }

            for (int j = i + 1; j < lines.length; j++) {
                String candidate = lines[j].trim();

                if (candidate.isEmpty()) continue;
                if (candidate.startsWith("#")) continue;

                if (bandwidth > bestBandwidth) {
                    bestBandwidth = bandwidth;
                    bestUrl = new URL(
                            new URL(baseUrl), candidate
                    ).toString();
                }

                break;
            }
        }

        return bestUrl;
    }

    private boolean hasUnsupportedEncryption(String playlist) {
        String[] lines = playlist.split("\\r?\\n");

        for (String line : lines) {
            String upper = line.trim().toUpperCase();

            if (upper.startsWith("#EXT-X-KEY:")) {
                if (!upper.contains("METHOD=NONE")) {
                    return true;
                }
            }
        }

        return false;
    }

    private String fetchText(String address) throws Exception {
        HttpURLConnection connection = openGet(address);

        try {
            int code = connection.getResponseCode();

            if (code < 200 || code >= 300) {
                throw new Exception(
                        "재생목록 HTTP 오류: " + code
                );
            }

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            try (InputStream in = connection.getInputStream()) {
                byte[] data = new byte[8192];
                int count;

                while ((count = in.read(data)) != -1) {
                    buffer.write(data, 0, count);
                }
            }

            return buffer.toString("UTF-8");

        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openGet(String address)
            throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", userAgent());
        connection.setRequestProperty("Referer", sourceUrl);
        connection.setRequestProperty(
                "Origin", "https://www.sooplive.co.kr"
        );

        return connection;
    }

    private String userAgent() {
        return "Mozilla/5.0 (Linux; Android 17) " +
                "AppleWebKit/537.36 Chrome/130.0.0.0 " +
                "Mobile Safari/537.36";
    }

    private String extractVideoId(String input) {
        Matcher matcher = Pattern.compile(
                "/player/(\\d+)"
        ).matcher(input);

        if (matcher.find()) return matcher.group(1);

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
}
