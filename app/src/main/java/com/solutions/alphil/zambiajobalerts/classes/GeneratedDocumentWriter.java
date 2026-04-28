package com.solutions.alphil.zambiajobalerts.classes;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GeneratedDocumentWriter {

    public static final String MIME_PDF = "application/pdf";
    public static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String MIME_TEXT = "text/plain";

    public static class WrittenDocument {
        private final File file;
        private final String format;

        public WrittenDocument(File file, String format) {
            this.file = file;
            this.format = format;
        }

        public File getFile() {
            return file;
        }

        public String getFormat() {
            return format;
        }
    }

    private final Context context;

    public GeneratedDocumentWriter(Context context) {
        this.context = context.getApplicationContext();
    }

    public File getOutputDirectory() {
        File base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (base == null) {
            base = context.getFilesDir();
        }

        File directory = new File(base, "generated_documents");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public WrittenDocument write(String text, String format, String titlePrefix) throws IOException {
        String extension = getExtension(format);
        String fileName = sanitizeTitle(titlePrefix) + "_" + currentTimestamp() + "." + extension;
        File directory = getOutputDirectory();
        File outFile = new File(directory, fileName);

        switch (format) {
            case "pdf":
                writePdf(outFile, text);
                break;
            case "docx":
                writeDocx(outFile, text);
                break;
            case "txt":
            default:
                writeText(outFile, text);
                break;
        }

        return new WrittenDocument(outFile, extension);
    }

    public Uri getUriForFile(File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    public String getMimeType(String format) {
        switch (format) {
            case "pdf":
                return MIME_PDF;
            case "docx":
                return MIME_DOCX;
            case "txt":
            default:
                return MIME_TEXT;
        }
    }

    private String getExtension(String format) {
        switch (format) {
            case "pdf":
                return "pdf";
            case "docx":
                return "docx";
            default:
                return "txt";
        }
    }

    private void writeText(File file, String text) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(text == null ? "" : text);
        }
    }

    private void writePdf(File file, String text) throws IOException {
        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        try {
            final int pageWidth = 595;
            final int pageHeight = 842;
            final int margin = 40;
            final int lineHeight = 30;

            android.graphics.Paint bodyPaint = new android.graphics.Paint();
            bodyPaint.setTextSize(12);
            bodyPaint.setAntiAlias(true);
            bodyPaint.setColor(android.graphics.Color.BLACK);

            List<String> lines = splitForWidth(text == null ? "" : text,
                    pageWidth - (2 * margin), bodyPaint);
            int pageNum = 1;
            int y = margin + 24;

            android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            for (String line : lines) {
                if (y > (pageHeight - margin)) {
                    document.finishPage(page);
                    pageNum++;
                    y = margin + 24;
                    pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                }

                canvas.drawText(line, margin, y, bodyPaint);
                y += lineHeight;
            }

            document.finishPage(page);

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                document.writeTo(outputStream);
            }
        } finally {
            document.close();
        }
    }

    private List<String> splitForWidth(String text, int maxWidth, android.graphics.Paint paint) {
        List<String> lines = new ArrayList<>();
        String normalized = text.replace("\r", "");
        String[] paragraphs = normalized.split("\\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.trim().split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (currentLine.length() == 0) {
                    if (paint.measureText(word) <= maxWidth) {
                        currentLine.append(word);
                    } else {
                        lines.add(word);
                    }
                } else {
                    String candidate = currentLine + " " + word;
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine.append(" ").append(word);
                    } else {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    }
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }

        if (lines.isEmpty()) {
            lines.add("");
        }

        return lines;
    }

    private void writeDocx(File file, String text) throws IOException {
        String documentXml = buildDocxDocumentXml(text == null ? "" : text);

        String contentTypesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                "</Types>";

        String relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
                "</Relationships>";

        String docRelsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>";

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file))) {
            putZipEntry(zipOut, "[Content_Types].xml", contentTypesXml);
            putZipEntry(zipOut, "_rels/.rels", relsXml);
            putZipEntry(zipOut, "word/document.xml", documentXml);
            putZipEntry(zipOut, "word/_rels/document.xml.rels", docRelsXml);
        }
    }

    private void putZipEntry(ZipOutputStream zipOut, String entryName, String data) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zipOut.putNextEntry(entry);
        zipOut.write(data.getBytes(StandardCharsets.UTF_8));
        zipOut.closeEntry();
    }

    private String buildDocxDocumentXml(String text) {
        StringBuilder doc = new StringBuilder();
        doc.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        doc.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n");
        doc.append("<w:body>\n");

        String[] lines = text.replace("\r", "").split("\\n", -1);
        for (String rawLine : lines) {
            doc.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                    .append(escapeXml(rawLine)).append("</w:t></w:r></w:p>\n");
        }

        doc.append("</w:body></w:document>");
        return doc.toString();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String sanitizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "document";
        }
        return title.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String currentTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }
}
