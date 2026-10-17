package com.example.crconnect;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CustomToast {

    public static void show(Context context, String message) {
        if (context == null || message == null || message.trim().isEmpty()) return;
        try {
            Context appContext = context.getApplicationContext();
            Toast toast = new Toast(appContext);

            LinearLayout layout = new LinearLayout(appContext);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER);

            GradientDrawable shape = new GradientDrawable();
            shape.setColor(Color.parseColor("#0F172A"));
            shape.setCornerRadius(dpToPx(appContext, 12));
            layout.setBackground(shape);

            int paddingH = dpToPx(appContext, 20);
            int paddingV = dpToPx(appContext, 12);
            layout.setPadding(paddingH, paddingV, paddingH, paddingV);

            TextView textView = new TextView(appContext);
            textView.setText(message);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            textView.setGravity(Gravity.CENTER);

            layout.addView(textView);

            toast.setView(layout);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dpToPx(appContext, 64));
            toast.show();
        } catch (Exception e) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
