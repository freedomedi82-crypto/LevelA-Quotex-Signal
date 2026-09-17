package com.levela.quotexsignal;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_CSV = 1001;
    private TextView report;
    private EditText priceInput;
    private LevelADataCapture capture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        capture = new LevelADataCapture(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 40, 32, 32);

        TextView title = new TextView(this);
        title.setText("LEVEL A SIGNAL");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView mode = new TextView(this);
        mode.setText("RESEARCH / VALIDATION MODE\nNo automatic order execution");
        mode.setTextSize(15);
        mode.setGravity(Gravity.CENTER);
        mode.setPadding(0, 12, 0, 20);
        root.addView(mode, new LinearLayout.LayoutParams(-1, -2));

        Button sample = new Button(this);
        sample.setText("RUN SAMPLE VALIDATION");
        sample.setOnClickListener(v -> runSample());
        root.addView(sample, new LinearLayout.LayoutParams(-1, -2));

        Button load = new Button(this);
        load.setText("LOAD CSV DATA");
        load.setOnClickListener(v -> pickCsv());
        root.addView(load, new LinearLayout.LayoutParams(-1, -2));

        TextView captureLabel = new TextView(this);
        captureLabel.setText("\nDATA CAPTURE (manual price input)");
        captureLabel.setTextSize(16);
        root.addView(captureLabel, new LinearLayout.LayoutParams(-1, -2));

        priceInput = new EditText(this);
        priceInput.setHint("Price");
        priceInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(priceInput, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout captureRow = new LinearLayout(this);
        captureRow.setOrientation(LinearLayout.HORIZONTAL);
        Button start = new Button(this);
        start.setText("START");
        start.setOnClickListener(v -> { capture.start(true); report.setText("CAPTURE ACTIVE\n\nEnter a price and press CAPTURE PRICE.\nDataset: " + capture.pointCount() + " points"); });
        captureRow.addView(start, new LinearLayout.LayoutParams(0, -2, 1));
        Button add = new Button(this);
        add.setText("CAPTURE PRICE");
        add.setOnClickListener(v -> capturePrice());
        captureRow.addView(add, new LinearLayout.LayoutParams(0, -2, 1));
        Button stop = new Button(this);
        stop.setText("STOP");
        stop.setOnClickListener(v -> { capture.stop(); report.setText("CAPTURE STOPPED\n\nCaptured points: " + capture.pointCount() + "\nFile: " + capture.filePath()); });
        captureRow.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(captureRow, new LinearLayout.LayoutParams(-1, -2));

        Button validateCapture = new Button(this);
        validateCapture.setText("VALIDATE CAPTURED DATA");
        validateCapture.setOnClickListener(v -> validateCaptured());
        root.addView(validateCapture, new LinearLayout.LayoutParams(-1, -2));

        report = new TextView(this);
        report.setText("Status: READY\n\nCSV format: timestamp,price\nMinimum for calibration: 20 valid points\nRecommended: >=100 valid points with regular sampling.");
        report.setTextSize(15);
        report.setTextColor(Color.DKGRAY);
        report.setPadding(0, 24, 0, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(report);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void capturePrice() {
        try {
            double price = Double.parseDouble(priceInput.getText().toString().trim());
            if (!capture.capture(price)) throw new IllegalStateException("Capture belum aktif atau harga tidak valid");
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

    private void pickCsv() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, PICK_CSV);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_CSV || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream input = getContentResolver().openInputStream(data.getData())) {
            if (input == null) throw new IllegalStateException("Tidak dapat membuka file");
            String text = new String(readAll(input), StandardCharsets.UTF_8);
            ReplayCsvLoader.Result parsed = ReplayCsvLoader.INSTANCE.parse(text);
            LevelAIntegrityReport r = LevelAResearchEngine.INSTANCE.evaluate(parsed.getPoints(), 60);
            report.setText(formatReport("CSV", r, parsed.getErrors()));
        } catch (Exception e) {
            report.setText("LOAD ERROR\n\n" + e.getMessage());
        }
    }

    private void runSample() {
        ArrayList<ReplayPoint> points = new ArrayList<>();
        long t = 1_000_000L;
        double p = 100.0;
        for (int i = 0; i < 120; i++) {
            p += (i % 11 == 0 ? 0.03 : (i % 2 == 0 ? 0.012 : -0.007));
            points.add(new ReplayPoint(t, p));
            t += 5000L;
        }
        LevelAIntegrityReport r = LevelAResearchEngine.INSTANCE.evaluate(points, 60);
        report.setText(formatReport("SAMPLE", r, new ArrayList<>()));
    }

    private String formatReport(String source, LevelAIntegrityReport r, List<String> parseErrors) {
        StringBuilder s = new StringBuilder();
        s.append("VALIDATION: ").append(source).append("\n\n");
        s.append("VERDICT: ").append(r.getVerdict()).append("\n");
        s.append("Points: ").append(r.getPoints()).append("\n");
        s.append("Valid points: ").append(r.getValidPoints()).append("\n");
        s.append("Duplicate timestamps: ").append(r.getDuplicateTimestamps()).append("\n");
        s.append("Invalid prices: ").append(r.getInvalidPrices()).append("\n");
        s.append("Non-monotonic timestamps: ").append(r.getNonMonotonicTimestamps()).append("\n");
        s.append("Median interval: ").append(r.getMedianIntervalMs()).append(" ms\n");
        s.append("Interval CV: ").append(String.format(java.util.Locale.US, "%.3f", r.getIntervalCv())).append("\n");
        s.append("Coverage score: ").append(String.format(java.util.Locale.US, "%.0f%%", r.getCoverageScore() * 100.0)).append("\n");
        s.append("Suggested expiry: ").append(r.getSuggestedExpirySeconds()).append(" s / ").append(r.getSuggestedExpirySteps()).append(" steps\n");
        s.append("\n").append(r.getNote()).append("\n");
        if (!parseErrors.isEmpty()) s.append("\nCSV parse warnings: ").append(parseErrors.size()).append("\n");
        if (!r.getIssues().isEmpty()) s.append("Issues: ").append(r.getIssues()).append("\n");
        s.append("\nCalibration remains blocked unless the dataset passes the integrity gates.");
        return s.toString();
    }

    private byte[] readAll(InputStream input) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
        return out.toByteArray();
    }
}
