package com.levela.quotexsignal;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private final LevelADataCapture capture = new LevelADataCapture(this);
    private EditText priceInput;
    private TextView report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("LEVEL A SIGNAL");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView mode = new TextView(this);
        mode.setText("RESEARCH / VALIDATION MODE\nNo automatic order execution");
        mode.setPadding(0, 12, 0, 20);
        root.addView(mode);

        addButton(root, "RUN SAMPLE VALIDATION", v -> runSampleValidation());
        addButton(root, "LOAD CSV DATA", v -> loadCsvData());

        priceInput = new EditText(this);
        priceInput.setHint("Price");
        priceInput.setInputType(2 | 8192);
        root.addView(priceInput);

        LinearLayout captureRow = new LinearLayout(this);
        captureRow.setOrientation(LinearLayout.HORIZONTAL);
        Button start = new Button(this);
        start.setText("START");
        start.setOnClickListener(v -> { capture.start(false); report.setText("CAPTURE ACTIVE\n\nFile: " + capture.filePath()); });
        captureRow.addView(start, new LinearLayout.LayoutParams(0, -2, 1));
        Button cap = new Button(this);
        cap.setText("CAPTURE PRICE");
        cap.setOnClickListener(v -> capturePrice());
        captureRow.addView(cap, new LinearLayout.LayoutParams(0, -2, 1));
        Button stop = new Button(this);
        stop.setText("STOP");
        stop.setOnClickListener(v -> { capture.stop(); report.setText("CAPTURE STOPPED\n\nPoints: " + capture.pointCount()); });
        captureRow.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(captureRow);

        addButton(root, "VALIDATE CAPTURED DATA", v -> validateCaptured());

        report = new TextView(this);
        report.setText("Ready. Run sample validation or start manual capture.");
        report.setTextColor(Color.DKGRAY);
        report.setPadding(0, 24, 0, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(report);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void addButton(LinearLayout root, String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(listener);
        root.addView(b);
    }

    private void capturePrice() {
        try {
            double price = Double.parseDouble(priceInput.getText().toString().trim());
            if (!capture.capture(price, System.currentTimeMillis())) throw new IllegalStateException("Capture belum aktif atau harga tidak valid");
            report.setText("CAPTURED\n\nPrice: " + price + "\nPoints: " + capture.pointCount() + "\n\nTekan VALIDATE CAPTURED DATA untuk menjalankan integrity gates.");
        } catch (Exception e) {
            report.setText("CAPTURE ERROR\n\n" + e.getMessage());
        }
    }

    private void validateCaptured() {
        ReplayCsvLoader.Result parsed = ReplayCsvLoader.INSTANCE.parse(capture.csvText());
        LevelAIntegrityReport r = LevelAResearchEngine.INSTANCE.evaluate(parsed.getPoints(), 60);
        report.setText(formatReport("CAPTURE", r, parsed.getErrors()));
    }

    private void runSampleValidation() {
        List<ReplayCsvLoader.Point> points = new ArrayList<>();
        long t = System.currentTimeMillis() - (119L * 5000L);
        double price = 100.0;
        for (int i = 0; i < 120; i++) {
            price += (i % 7 == 0 ? 0.08 : 0.02);
            points.add(new ReplayCsvLoader.Point(t + i * 5000L, price));
        }
        LevelAIntegrityReport r = LevelAResearchEngine.INSTANCE.evaluate(points, 60);
        report.setText(formatReport("SAMPLE", r, new ArrayList<String>()));
    }

    private void loadCsvData() {
        report.setText("CSV REPLAY\n\nUse the repository's ReplayCsvLoader path for CSV data with columns timestamp,price.\n\nAfter loading data, run validation before calibration.");
    }

    private String formatReport(String source, LevelAIntegrityReport r, List<String> errors) {
        return source + " VALIDATION\n\n"
                + "Verdict: " + r.getVerdict() + "\n"
                + "Points: " + r.getPointCount() + "\n"
                + "Valid points: " + r.getValidPointCount() + "\n"
                + "Duplicate timestamps: " + r.getDuplicateTimestamps() + "\n"
                + "Invalid prices: " + r.getInvalidPrices() + "\n"
                + "Non-monotonic timestamps: " + r.getNonMonotonicTimestamps() + "\n"
                + "Median interval: " + r.getMedianIntervalMs() + " ms\n"
                + "Interval CV: " + r.getIntervalCv() + "\n"
                + "Coverage score: " + r.getCoverageScore() + "\n"
                + "Suggested expiry: " + r.getSuggestedExpirySeconds() + " s\n\n"
                + "Notes: " + r.getNotes() + "\n"
                + "Parse warnings/errors: " + errors;
    }
}
