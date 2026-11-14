
package com.soop.catchsaver;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
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
        urlInput.setHint("SOOP 캐치 URL을 붙여넣으세요");
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_URI);

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        -1, -2);
        inputParams.topMargin = 55;
        layout.addView(urlInput, inputParams);

        Button checkButton = new Button(this);
        checkButton.setText("영상 주소 확인");
        layout.addView(checkButton);

        status = new TextView(this);
        status.setText("캐치 영상 URL을 입력해주세요.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setPadding(0, 35, 0, 35);
        layout.addView(status);

        Button openButton = new Button(this);
        openButton.setText("SOOP 영상 열기");
        layout.addView(openButton);

        checkButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            if (url.isEmpty()) {
                status.setText("영상 URL을 입력해주세요.");
                return;
            }

            Uri uri = Uri.parse(url);
            String host = uri.getHost();

            if (uri.getScheme() != null &&
                    (uri.getScheme().equals("http") ||
                     uri.getScheme().equals("https")) &&
                    host != null &&
                    (host.equals("sooplive.co.kr") ||
                     host.endsWith(".sooplive.co.kr") ||
                     host.equals("sooplive.com") ||
                     host.endsWith(".sooplive.com"))) {

                status.setText("SOOP 주소 확인 완료!\n영상 접근 가능 여부를 확인해야 합니다.");

            } else {
                status.setText("올바른 SOOP 영상 주소를 입력해주세요.");
            }
        });

        openButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            try {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();

                if (host != null &&
                        (host.equals("sooplive.co.kr") ||
                         host.endsWith(".sooplive.co.kr") ||
                         host.equals("sooplive.com") ||
                         host.endsWith(".sooplive.com"))) {

                    Intent intent = new Intent(
                            Intent.ACTION_VIEW, uri);
                    startActivity(intent);

                } else {
                    status.setText("먼저 올바른 SOOP 주소를 입력해주세요.");
                }

            } catch (Exception e) {
                status.setText("주소를 확인해주세요.");
            }
        });

        setContentView(layout);
    }
}
