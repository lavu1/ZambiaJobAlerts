package com.solutions.alphil.zambiajobalerts.classes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GeneratedDocumentWriter(context: Context) {
    private val context = context.applicationContext

    class WrittenDocument(
        val file: File,
        val format: String,
    )

    fun getOutputDirectory(): File {
        var base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (base == null) {
            base = context.filesDir
        }

        val directory = File(base, "generated_documents")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    @Throws(IOException::class)
    fun write(text: String?, format: String, titlePrefix: String?): WrittenDocument {
        val extension = getExtension(format)
        val fileName = "${sanitizeTitle(titlePrefix)}_${currentTimestamp()}.$extension"
        val directory = getOutputDirectory()
        val outFile = File(directory, fileName)

        when (format) {
            "pdf" -> writePdf(outFile, text)
            "docx" -> writeDocx(outFile, text)
            "txt" -> writeText(outFile, text)
            else -> writeText(outFile, text)
        }

        return WrittenDocument(outFile, extension)
    }

    fun getUriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    fun getMimeType(format: String): String =
        when (format) {
            "pdf" -> MIME_PDF
            "docx" -> MIME_DOCX
            "txt" -> MIME_TEXT
            else -> MIME_TEXT
        }

    private fun getExtension(format: String): String =
        when (format) {
            "pdf" -> "pdf"
            "docx" -> "docx"
            else -> "txt"
        }

    @Throws(IOException::class)
    private fun writeText(file: File, text: String?) {
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(text ?: "")
            }
        }
    }

    @Throws(IOException::class)
    private fun writePdf(file: File, text: String?) {
        val document = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40
            val lineHeight = 30

            val bodyPaint = Paint().apply {
                textSize = 12f
                isAntiAlias = true
                color = Color.BLACK
            }

            val lines = splitForWidth(text ?: "", pageWidth - (2 * margin), bodyPaint)
            var pageNum = 1
            var y = margin + 24

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            for (line in lines) {
                if (y > pageHeight - margin) {
                    document.finishPage(page)
                    pageNum++
                    y = margin + 24
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                }

                canvas.drawText(line, margin.toFloat(), y.toFloat(), bodyPaint)
                y += lineHeight
            }

            document.finishPage(page)

            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
        } finally {
            document.close()
        }
    }

    private fun splitForWidth(text: String, maxWidth: Int, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val normalized = text.replace("\r", "")
        val paragraphs = normalized.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("")
                continue
            }

            val words = paragraph.trim().split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                if (currentLine.isEmpty()) {
                    if (paint.measureText(word) <= maxWidth) {
                        currentLine.append(word)
                    } else {
                        lines.add(word)
                    }
                } else {
                    val candidate = "$currentLine $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine.append(" ").append(word)
                    } else {
                        lines.add(currentLine.toString())
                        currentLine = StringBuilder(word)
                    }
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }

        if (lines.isEmpty()) {
            lines.add("")
        }

        return lines
    }

    @Throws(IOException::class)
    private fun writeDocx(file: File, text: String?) {
        val documentXml = buildDocxDocumentXml(text ?: "")

        val contentTypesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
            "</Types>"

        val relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
            "</Relationships>"

        val docRelsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>"

        ZipOutputStream(FileOutputStream(file)).use { zipOut ->
            putZipEntry(zipOut, "[Content_Types].xml", contentTypesXml)
            putZipEntry(zipOut, "_rels/.rels", relsXml)
            putZipEntry(zipOut, "word/document.xml", documentXml)
            putZipEntry(zipOut, "word/_rels/document.xml.rels", docRelsXml)
        }
    }

    @Throws(IOException::class)
    private fun putZipEntry(zipOut: ZipOutputStream, entryName: String, data: String) {
        val entry = ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        zipOut.write(data.toByteArray(StandardCharsets.UTF_8))
        zipOut.closeEntry()
    }

    private fun buildDocxDocumentXml(text: String): String {
        val doc = StringBuilder()
        doc.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        doc.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
        doc.append("<w:body>\n")

        val lines = text.replace("\r", "").split("\n")
        for (rawLine in lines) {
            doc.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                .append(escapeXml(rawLine)).append("</w:t></w:r></w:p>\n")
        }

        doc.append("</w:body></w:document>")
        return doc.toString()
    }

    private fun escapeXml(value: String?): String =
        value.orEmpty()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun sanitizeTitle(title: String?): String {
        if (title.isNullOrBlank()) {
            return "document"
        }
        return title.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun currentTimestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        const val MIME_TEXT = "text/plain"
    }
}
