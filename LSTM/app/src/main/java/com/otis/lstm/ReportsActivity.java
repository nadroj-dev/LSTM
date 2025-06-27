package com.otis.lstm;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.graphics.pdf.PdfDocument;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class ReportsActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private TextView avgTempText, avgHumidityText, highestTempText, lowestTempText, highestHumidityText, lowestHumidityText;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        setTitle("Reports");

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        avgTempText = findViewById(R.id.avgTempText);
        avgHumidityText = findViewById(R.id.avgHumidityText);
        highestTempText = findViewById(R.id.highestTempText);
        lowestTempText = findViewById(R.id.lowestTempText);
        highestHumidityText = findViewById(R.id.highestHumidityText);
        lowestHumidityText = findViewById(R.id.lowestHumidityText);

        // Fetch data from Firestore and generate report
        fetchDataAndGenerateReport();

        // Download buttons
        Button downloadCsvButton = findViewById(R.id.downloadCsvButton);
        Button downloadPdfButton = findViewById(R.id.downloadPdfButton);
        Button downloadLogsButton = findViewById(R.id.downloadLogsButton);

        downloadCsvButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                exportToCSV();
            } else {
                requestPermissions();
            }
        });

        downloadPdfButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                exportToPDF();
            } else {
                requestPermissions();
            }
        });

        downloadLogsButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                showLogsExportDialog();
            } else {
                requestPermissions();
            }
        });
    }

    // Check if permissions are granted (READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE)
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    // Request permissions using the default Android permission dialog
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No external storage permissions required for this version.", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchDataAndGenerateReport() {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        calendar.add(Calendar.MONTH, -1);
        Date lastMonth = calendar.getTime();

        firestore.collection("sensorData")
                .whereGreaterThanOrEqualTo("timestamp", new com.google.firebase.Timestamp(lastMonth))
                .whereLessThanOrEqualTo("timestamp", new com.google.firebase.Timestamp(currentDate))
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double totalTemp = 0, totalHumidity = 0;
                        double highestTemp = Double.MIN_VALUE, lowestTemp = Double.MAX_VALUE;
                        double highestHumidity = Double.MIN_VALUE, lowestHumidity = Double.MAX_VALUE;
                        int count = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double temperature = document.getDouble("temperature");
                            Double humidity = document.getDouble("humidity");

                            if (temperature != null && humidity != null) {
                                totalTemp += temperature;
                                totalHumidity += humidity;
                                count++;

                                highestTemp = Math.max(highestTemp, temperature);
                                lowestTemp = Math.min(lowestTemp, temperature);
                                highestHumidity = Math.max(highestHumidity, humidity);
                                lowestHumidity = Math.min(lowestHumidity, humidity);
                            }
                        }

                        if (count > 0) {
                            DecimalFormat df = new DecimalFormat("###.##");

                            avgTempText.setText("Average Temperature: " + df.format(totalTemp / count) + "°C");
                            avgHumidityText.setText("Average Humidity: " + df.format(totalHumidity / count) + "%");
                            highestTempText.setText("Highest Temperature: " + highestTemp + "°C");
                            lowestTempText.setText("Lowest Temperature: " + lowestTemp + "°C");
                            highestHumidityText.setText("Highest Humidity: " + highestHumidity + "%");
                            lowestHumidityText.setText("Lowest Humidity: " + lowestHumidity + "%");
                        } else {
                            Toast.makeText(ReportsActivity.this, "No data available for the selected time range", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ReportsActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Export sensor data to CSV
    private void exportToCSV() {
        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            directory = getExternalFilesDir("Reports");
        } else {
            directory = new File(Environment.getExternalStorageDirectory(), "Reports");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "report_" + System.currentTimeMillis() + ".csv";
        File file = new File(directory, fileName);

        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("Metric,Value\n");

            bufferedWriter.write("Average Temperature," + avgTempText.getText().toString().replace("Average Temperature: ", "") + "\n");
            bufferedWriter.write("Average Humidity," + avgHumidityText.getText().toString().replace("Average Humidity: ", "") + "\n");
            bufferedWriter.write("Highest Temperature," + highestTempText.getText().toString().replace("Highest Temperature: ", "") + "\n");
            bufferedWriter.write("Lowest Temperature," + lowestTempText.getText().toString().replace("Lowest Temperature: ", "") + "\n");
            bufferedWriter.write("Highest Humidity," + highestHumidityText.getText().toString().replace("Highest Humidity: ", "") + "\n");
            bufferedWriter.write("Lowest Humidity," + lowestHumidityText.getText().toString().replace("Lowest Humidity: ", "") + "\n");

            bufferedWriter.close();
            Toast.makeText(this, "CSV saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving CSV file", Toast.LENGTH_SHORT).show();
        }
    }

    // Export sensor data to PDF
    private void exportToPDF() {
        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            directory = getExternalFilesDir("Reports");
        } else {
            directory = new File(Environment.getExternalStorageDirectory(), "Reports");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "report_" + System.currentTimeMillis() + ".pdf";
        File file = new File(directory, fileName);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(16);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Weather Report", 50, 50, paint);

        float tableStartY = 100;
        canvas.drawText("Metric", 50, tableStartY, paint);
        canvas.drawText("Value", 300, tableStartY, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        float lineHeight = 30;
        float currentY = tableStartY + lineHeight;

        canvas.drawText("Average Temperature", 50, currentY, paint);
        canvas.drawText(avgTempText.getText().toString(), 300, currentY, paint);
        currentY += lineHeight;

        canvas.drawText("Average Humidity", 50, currentY, paint);
        canvas.drawText(avgHumidityText.getText().toString(), 300, currentY, paint);
        currentY += lineHeight;

        canvas.drawText("Highest Temperature", 50, currentY, paint);
        canvas.drawText(highestTempText.getText().toString(), 300, currentY, paint);
        currentY += lineHeight;

        canvas.drawText("Lowest Temperature", 50, currentY, paint);
        canvas.drawText(lowestTempText.getText().toString(), 300, currentY, paint);
        currentY += lineHeight;

        canvas.drawText("Highest Humidity", 50, currentY, paint);
        canvas.drawText(highestHumidityText.getText().toString(), 300, currentY, paint);
        currentY += lineHeight;

        canvas.drawText("Lowest Humidity", 50, currentY, paint);
        canvas.drawText(lowestHumidityText.getText().toString(), 300, currentY, paint);

        document.finishPage(page);

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF file", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    // Show dialog to choose CSV or PDF for logs export
    private void showLogsExportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Export Logs");
        builder.setMessage("Choose the export format:");
        builder.setPositiveButton("CSV", (dialog, which) -> exportLogsToCSV());
        builder.setNegativeButton("PDF", (dialog, which) -> exportLogsToPDF());
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Export logs to CSV
    private void exportLogsToCSV() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            directory = getExternalFilesDir("Reports");
        } else {
            directory = new File(Environment.getExternalStorageDirectory(), "Reports");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "logs_" + System.currentTimeMillis() + ".csv";
        File file = new File(directory, fileName);

        firestore.collection("Logs")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        try {
                            FileWriter fileWriter = new FileWriter(file);
                            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                            bufferedWriter.write("Timestamp,Event Type,Message,Details\n");

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String eventType = doc.getString("eventType");
                                String message = doc.getString("message");
                                com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                                Map<String, Object> details = (Map<String, Object>) doc.get("details");

                                String timestampStr = timestamp != null ? sdf.format(timestamp.toDate()) : "";
                                eventType = eventType != null ? eventType.replace(",", "") : "Unknown";
                                message = message != null ? message.replace(",", "") : "No message";
                                String detailsStr = details != null ? details.toString().replace(",", ";") : "{}";

                                bufferedWriter.write(String.format("%s,%s,%s,%s\n",
                                        timestampStr, eventType, message, detailsStr));
                            }

                            bufferedWriter.close();
                            Toast.makeText(this, "Logs CSV saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error saving logs CSV file", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching logs", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Export logs to PDF
    private void exportLogsToPDF() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            directory = getExternalFilesDir("Reports");
        } else {
            directory = new File(Environment.getExternalStorageDirectory(), "Reports");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "logs_" + System.currentTimeMillis() + ".pdf";
        File file = new File(directory, fileName);

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        AtomicReference<PdfDocument.Page> page = new AtomicReference<>(pdfDocument.startPage(pageInfo));

        AtomicReference<Canvas> canvas = new AtomicReference<>(page.get().getCanvas());
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);

        // Draw title
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.get().drawText("Logs Report", 50, 50, paint);

        // Table headers
        float tableStartY = 80;
        canvas.get().drawText("Timestamp", 50, tableStartY, paint);
        canvas.get().drawText("Event Type", 150, tableStartY, paint);
        canvas.get().drawText("Message", 250, tableStartY, paint);
        canvas.get().drawText("Details", 400, tableStartY, paint);

        // Collect log entries
        List<String[]> logEntries = new ArrayList<>();
        firestore.collection("Logs")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String eventType = doc.getString("eventType");
                            String message = doc.getString("message");
                            com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                            Map<String, Object> details = (Map<String, Object>) doc.get("details");

                            String timestampStr = timestamp != null ? sdf.format(timestamp.toDate()) : "";
                            eventType = eventType != null ? eventType : "Unknown";
                            message = message != null ? message : "No message";
                            String detailsStr = details != null ? formatDetails(details) : "{}";

                            logEntries.add(new String[]{timestampStr, eventType, message, detailsStr});
                        }

                        // Draw log entries
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        float lineHeight = 20;
                        float currentY = tableStartY + lineHeight;

                        for (String[] entry : logEntries) {
                            if (currentY + lineHeight > 800) {
                                pdfDocument.finishPage(page.get());
                                page.set(pdfDocument.startPage(pageInfo));
                                canvas.set(page.get().getCanvas());
                                currentY = 50;
                                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                                canvas.get().drawText("Timestamp", 50, currentY, paint);
                                canvas.get().drawText("Event Type", 150, currentY, paint);
                                canvas.get().drawText("Message", 250, currentY, paint);
                                canvas.get().drawText("Details", 400, currentY, paint);
                                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                                currentY += lineHeight;
                            }

                            canvas.get().drawText(truncate(entry[0], 20), 50, currentY, paint);
                            canvas.get().drawText(truncate(entry[1], 15), 150, currentY, paint);
                            canvas.get().drawText(truncate(entry[2], 30), 250, currentY, paint);
                            canvas.get().drawText(truncate(entry[3], 25), 400, currentY, paint);
                            currentY += lineHeight;
                        }

                        pdfDocument.finishPage(page.get());
                        try {
                            pdfDocument.writeTo(new FileOutputStream(file));
                            Toast.makeText(this, "Logs PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error saving logs PDF file", Toast.LENGTH_SHORT).show();
                        } finally {
                            pdfDocument.close();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching logs", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper method to format details map for PDF
    private String formatDetails(Map<String, Object> details) {
        StringBuilder formatted = new StringBuilder();
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            formatted.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
        }
        return formatted.length() > 0 ? formatted.substring(0, formatted.length() - 2) : "{}";
    }

    // Helper method to truncate text for PDF to fit columns
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text;
    }
}