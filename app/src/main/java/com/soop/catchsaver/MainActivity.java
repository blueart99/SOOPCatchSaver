
package com.soop.catchsaver;

import android.app.Activity;
import android.app.DownloadManager;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.text.InputType;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {

    EditText urlInput;
    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 50, 32, 32);
        layout.setBackgroundColor(Color.rgb(18, 20, 27));

        TextView title = new TextView(this);
        title.setText("SOOP Catch Saver");
        title.setTextSize(27);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("SOOP 캐치 영상 저장");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        urlInput = new EditText(this);
        urlInput.setHint("SOOP URL 또는 직접 MP4 주소 입력");
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_URI);

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(-1, -2);
        inputParams.topMargin = 40;
        layout.addView(urlInput, inputParams);

        Button checkButton = new Button(this);
        checkButton.setText("영상 주소 확인");
        layout.addView(checkButton);

        status = new TextView(this);
        status.setText("캐치 영상 URL을 입력해주세요.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setPadding(0, 25, 0, 25);
        layout.addView(status);

        Button openButton = new Button(this);
        openButton.setText("SOOP 영상 열기");
        layout.addView(openButton);

        Button downloadButton = new Button(this);
        downloadButton.setText("MP4 다운로드");
        layout.addView(downloadButton);

        checkButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            if (isSoopUrl(url)) {
                status.setText("SOOP 주소 확인 완료!\n영상 페이지를 열 수 있습니다.");
            } else {
                status.setText("올바른 SOOP 주소를 입력해주세요.");
            }
        });

        openButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            if (isSoopUrl(url)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    status.setText("영상을 열 수 없습니다.");
                }
            } else {
                status.setText("먼저 올바른 SOOP 주소를 입력해주세요.");
            }
        });

        downloadButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            try {
                Uri uri = Uri.parse(url);
                String path = uri.getPath();

                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || path == null
                        || !path.toLowerCase().endsWith(".mp4")) {
                    status.setText(
                        "다운로드할 수 있는 직접 MP4 주소를 입력해주세요.\n" +
                        "캐치 페이지 주소만으로는 다운로드할 수 없습니다."
                    );
                    return;
                }

                String filename = uri.getLastPathSegment();

                if (filename == null || filename.isEmpty()) {
                    filename = "SOOP_Catch.mp4";
                }

                DownloadManager manager =
                        (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

                DownloadManager.Request request =
                        new DownloadManager.Request(uri);

                request.setTitle(filename);
                request.setDescription("SOOP 영상 다운로드");
                request.setMimeType("video/mp4");
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                );
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        filename
                );

                manager.enqueue(request);
                status.setText("다운로드를 시작했습니다.\n알림창에서 진행 상황을 확인하세요.");

            } catch (Exception e) {
                status.setText("다운로드를 시작할 수 없습니다. 주소를 확인해주세요.");
            }
        });

        setContentView(layout);
    }

    private boolean isSoopUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();

            return ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && host != null
                    && (host.equals("sooplive.co.kr")
                    || host.endsWith(".sooplive.co.kr")
                    || host.equals("sooplive.com")
                    || host.endsWith(".sooplive.com"));

        } catch (Exception e) {
            return false;
        }
    }
}
