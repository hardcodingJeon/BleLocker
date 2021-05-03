package com.bnctech.blelocker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.app.ActionBar;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class CustomAlertWindowVersion extends DialogFragment {

    private TextView CustomAlertWindowVersion_TextView_msg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_custom_alert_window_version, null);

        CustomAlertWindowVersion_TextView_msg = view.findViewById(R.id.CustomAlertWindowVersion_TextView_msg);
        CustomAlertWindowVersion_TextView_msg.setText(SplashActivity.appVersionName);

        dialog.setContentView(view);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        getDialog().getWindow().setLayout(MainActivity.window_width - 100, ActionBar.LayoutParams.WRAP_CONTENT);
    }
}