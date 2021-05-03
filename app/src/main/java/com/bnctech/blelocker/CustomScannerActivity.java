package com.bnctech.blelocker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CustomScannerActivity extends Activity implements DecoratedBarcodeView.TorchListener {
    private DecoratedBarcodeView barcodeScannerView;
    private CaptureManager capture;
    private ImageButton switchFlashlightButton;
    private ImageButton switchInputCodeButton;
    private Boolean switchFlashlightButtonCheck;
    private EditText et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_scanner);

        switchFlashlightButtonCheck = true;
        switchFlashlightButton = findViewById(R.id.Button_Scanner_Flash);
        switchFlashlightButton.setOnClickListener((v) -> switchFlashlight(v));

//        switchInputCodeButton = findViewById(R.id.Button_Scanner_InputCode);
//        switchInputCodeButton.setOnClickListener((v) -> {
//            FrameLayout container = new FrameLayout(getApplicationContext());
//            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//            params.leftMargin =  200;
//            params.rightMargin = 200;
//            et = new EditText(getApplicationContext());
//            et.setFilters(new InputFilter[] { new InputFilter.LengthFilter(5)});
//            et.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//            et.setInputType(InputType.TYPE_CLASS_NUMBER);
//            et.setLayoutParams(params);
//            container.addView(et);
//            AlertDialog.Builder ab_search = new AlertDialog.Builder(this);
//            ab_search.setTitle("바이크 번호 입력");
//            ab_search.setMessage("EBDLBA 글자 뒤 5자리 번호를 입력해주세요.");
//            ab_search.setView(container);
//            ab_search.setPositiveButton("확인", (dialog, which) -> {
//                String str = et.getText().toString();
//
//                if (str == null) {
//                    AlertDialog.Builder ab = new AlertDialog.Builder(this);
//                    ab.setTitle("바이크 번호 입력");
//                    ab.setMessage("5자리 숫자를 모두 입력해주세요.");
//                    ab.setPositiveButton("확인", null);
//                    ab.show();
//                } else if (str.length() != 5) {
//                    AlertDialog.Builder ab = new AlertDialog.Builder(this);
//                    ab.setTitle("바이크 번호 입력");
//                    ab.setMessage("5자리 숫자를 모두 입력해주세요.");
//                    ab.setPositiveButton("확인", null);
//                    ab.show();
//                } else {
//                    Intent intent = getIntent();
//                    intent.putExtra(Intents.Scan.RESULT, "EBDLBA_" + str);
//                    setResult(RESULT_OK, intent);
//                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
//                    finish();
//                }
//            });
//            ab_search.setNegativeButton("취소", null);
//            ab_search.show();
//        });

        if (!hasFlash()) {
            switchFlashlightButton.setVisibility(View.GONE);
        }

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setTorchListener(this);

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
    }

    @Override
    protected void onResume () {
        super.onResume();
        capture.onResume();
    }

    protected void onPause () {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    public void switchFlashlight(View view) {
        if (switchFlashlightButtonCheck) {
            barcodeScannerView.setTorchOn();
        } else {
            barcodeScannerView.setTorchOff();
        }
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public void onTorchOn() {
        switchFlashlightButton.setBackgroundResource(R.drawable.ic_flash_off);
        switchFlashlightButtonCheck = false;
    }

    @Override
    public void onTorchOff() {
        switchFlashlightButton.setBackgroundResource(R.drawable.ic_flash_on);
        switchFlashlightButtonCheck = true;
    }
}
