package com.levela.quotexsignal;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("LEVEL A SIGNAL");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText("Research engine initialized\n\nV15 Vision: READY\nV29 Market Integrity: READY\n\nMode: ANALYSIS / RESEARCH ONLY\nNo automatic order execution");
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 28, 0, 0);

        TextView note = new TextView(this);
        note.setText("Data must be validated before calibration. Signals are analytical outputs and do not guarantee trading results.");
        note.setTextSize(14);
        note.setTextColor(Color.DKGRAY);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 36, 0, 0);

        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }
}
