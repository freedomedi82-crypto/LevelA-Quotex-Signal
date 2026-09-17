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
    private LevelADataCapture capture;
    private EditText priceInput;
    private TextView report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        capture = new LevelADataCapture(this);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("LEVEL A SIGNAL 0.3.0");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView mode = new TextView(this);
        mode.setText("RESEARCH / VALIDATION MODE\nNo automatic order execution");
        mode.setPadding(0, 12, 0, 20);
        root.addView(mode);

        addButton(root, "RUN SAMPLE VALIDATION", v -> runSampleValidation());
        addButton(root, "RUN V15 VISION SMOKE TEST", v -> runVisionSmokeTest());

        priceInput = new EditText(this);
        priceInput.setHint("Price");
        priceInput.setInputType(2 | 8192);
        root.addView(priceInput);

        LinearLayout captureRow = new LinearLayout(this);
        captureRow.setOrientation(LinearLayout.HORIZONTAL);
        Button start = new Button(this);
        start.setText("START");
        start.setOnClickListener(v -> {
            capture.start(false);
            report.setText("CAPTURE ACTIVE\n\nFile: " + capture.filePath());
        });
        captureRow.addView(start, new LinearLayout.LayoutParams(0, -2, 1));
        Button cap = new Button(this);
        cap.setText("CAPTURE PRICE");
        cap.setOnClickListener(v -> capturePrice());
        captureRow.addView(cap, new LinearLayout.LayoutParams(0, -2, 1));
        Button stop = new Button(this);
        stop.setText("STOP");
        stop.setOnClickListener(v -> {
            capture.stop();
            report.setText("CAPTURE STOPPED\n\nPoints: " + capture.pointCount());
        });
        captureRow.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(captureRow);

        addButton(root, "VALIDATE CAPTURED DATA", v -> validateCaptured());
        addButton(root, "RUN MARKET INTEGRITY TEST", v -> runMarketIntegrityTest());
        addButton(root, "LOAD CSV DATA", v -> loadCsvData());

        report = new TextView(this);
        report.setText("Ready. Run validation before calibration.");
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
            if (!capture.capture(price, System.currentTimeMillis())) {
                throw new IllegalStateException("Capture belum aktif atau harga tidak valid");
            }
            report.setText("CAPTURED\n\nPrice: " + price + "\nPoints: " + capture.pointCount()
                    + "\n\nTekan VALIDATE CAPTURED DATA untuk menjalankan integrity gates.");
        } catch (Exception e) {
            report.setText("CAPTURE ERROR\n\n" + e.getMessage());
        }
    }

    private void validateCaptured() {
        ReplayCsvLoader.Result parsed = ReplayCsvLoader.INSTANCE.parse(capture.csvText());
        LevelAIntegrityReport r = LevelAResearchEngine.INSTANCE.evaluate(parsed.getPoints(), 60);
        MarketIntegrityReport m = MarketIntegrityEngine.INSTANCE.evaluate(parsed.getPoints());
        report.setText(formatReport("CAPTURE", r, m, parsed.getErrors()));
    }

    private void runSampleValidation() {
        List<ReplayPoint> points = new ArrayList<>();
        long t = System.currentTimeMillis() - (119L * 5000L);
        double price = 100.0;
        for (int i = 0; i < 120; i++) {
            price += (i % 7 == 0 ? 0.08 : 0.02);
            points.add(new ReplayPoint(t + i * 5000L, price));
        }
        LevelAIntegrityReport r = LevelAResearchEngine.INSTANCE.evaluate(points, 60);
        MarketIntegrityReport m = MarketIntegrityEngine.INSTANCE.evaluate(points);
        report.setText(formatReport("SAMPLE", r, m, new ArrayList<String>()));
    }

    private void runVisionSmokeTest() {
        V15VisionEngine engine = new V15VisionEngine();
        V15VisionState state = null;
        for (int i = 0; i < 10; i++) {
            state = engine.push(new V15Frame(i * 0.01, 100.0 + i * 0.01, 0.55, 0.45, 0.90,
                    System.currentTimeMillis() + i * 1000L));
        }
        report.setText("V15 VISION SMOKE TEST\n\n"
                + "Trend: " + state.getTrend() + "\n"
                + "Momentum: " + state.getMomentum() + "\n"
                + "Volatility: " + state.getVolatility() + "\n"
                + "Quality: " + state.getQuality() + "\n"
                + "Accepted frames: " + state.getAcceptedFrames() + "\n"
                + "Rejected frames: " + state.getRejectedFrames() + "\n\n"
                + "Result: " + (state.getAcceptedFrames() == 10 ? "PASS" : "CHECK"));
    }

    private void runMarketIntegrityTest() {
        List<ReplayPoint> points = new ArrayList<>();
        long t = System.currentTimeMillis() - 45000L;
        for (int i = 0; i < 10; i++) points.add(new ReplayPoint(t + i * 5000L, 100.0 + i * 0.01));
        MarketIntegrityReport m = MarketIntegrityEngine.INSTANCE.evaluate(points);
        report.setText("MARKET INTEGRITY TEST\n\n"
                + "Verdict: " + m.getVerdict() + "\n"
                + "Points: " + m.getPoints() + "\n"
                + "Valid points: " + m.getValidPoints() + "\n"
                + "Duplicate timestamps: " + m.getDuplicateTimestamps() + "\n"
                + "Non-monotonic timestamps: " + m.getNonMonotonicTimestamps() + "\n"
                + "Invalid prices: " + m.getInvalidPrices() + "\n"
                + "Stale gaps: " + m.getStaleIntervals() + "\n"
                + "Max gap: " + m.getMaxGapMs() + " ms\n"
                + "Issues: " + m.getIssues());
    }

    private void loadCsvData() {
        report.setText("CSV REPLAY\n\nExpected columns: timestamp,price.\n"
                + "Supported separators: comma, semicolon, or tab.\n\n"
                + "Data is parsed and integrity-checked before calibration.");
    }

    private String formatReport(String source, LevelAIntegrityReport r, MarketIntegrityReport m, List<String> errors) {
        return source + " VALIDATION\n\n"
                + "Market integrity: " + m.getVerdict() + "\n"
                + "Research verdict: " + r.getVerdict() + "\n"
                + "Points: " + r.getPoints() + "\n"
                + "Valid points: " + r.getValidPoints() + "\n"
                + "Duplicate timestamps: " + r.getDuplicateTimestamps() + "\n"
                + "Invalid prices: " + r.getInvalidPrices() + "\n"
                + "Non-monotonic timestamps: " + r.getNonMonotonicTimestamps() + "\n"
                + "Median interval: " + r.getMedianIntervalMs() + " ms\n"
                + "Interval CV: " + r.getIntervalCv() + "\n"
                + "Coverage score: " + r.getCoverageScore() + "\n"
                + "Suggested expiry: " + r.getSuggestedExpirySeconds() + " s\n\n"
                + "Research note: " + r.getNote() + "\n"
                + "Research issues: " + r.getIssues() + "\n"
                + "Market issues: " + m.getIssues() + "\n"
                + "Parse warnings/errors: " + errors;
    }
}
