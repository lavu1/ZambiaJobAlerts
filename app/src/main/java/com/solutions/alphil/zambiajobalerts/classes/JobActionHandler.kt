package com.solutions.alphil.zambiajobalerts.classes

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object JobActionHandler {
    fun applyForJob(context: Context, job: Job) {
        incrementJobViewCount(context)

        val application = job.getApplication()
        if (application.isBlank()) {
            Toast.makeText(context, "Application link not available", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanApp = application.trim()
        if (cleanApp.contains("@") && cleanApp.contains(".")) {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$cleanApp")
                putExtra(Intent.EXTRA_SUBJECT, "Job Application: ${job.getTitle()}")
                putExtra(Intent.EXTRA_TEXT, createEmailBody(job))
            }

            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                offerApplicationCopy(context, cleanApp)
            }
        } else if (cleanApp.startsWith("http://") || cleanApp.startsWith("https://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanApp))
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                offerApplicationCopy(context, cleanApp)
            }
        } else {
            offerApplicationCopy(context, cleanApp)
        }
    }

    fun shareJob(context: Context, job: Job) {
        incrementJobViewCount(context)

        val playStoreUrl = "https://play.google.com/store/apps/details?id=com.solutions.alphil.zambiajobalerts"
        val download = "Check out Zambia Job Alerts app for the latest job opportunities in Zambia! Download now: $playStoreUrl"
        val shareText = "Check out this job opportunity: ${job.getTitle()}" +
            "\n\n${job.getLink().orEmpty()}" +
            "\n\nVia Zambia Job Alerts App" +
            "\n\n$download"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Job Opportunity: ${job.getTitle()}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Job via"))
    }

    fun createEmailBody(job: Job): String =
        "Dear Hiring Manager,\n\n" +
            "I am writing to apply for the position of ${job.getTitle()}" +
            " that I found through Zambia Job Alerts.\n\n" +
            "Please find my application materials attached.\n\n" +
            "Thank you for your consideration.\n\n" +
            "Sincerely,\n" +
            "[Your Name]\n" +
            "[Your Phone Number]\n" +
            "[Your Email Address]"

    private fun offerApplicationCopy(context: Context, applicationDetails: String) {
        AlertDialog.Builder(context)
            .setTitle("Application Method")
            .setMessage("Would you like to copy the application details to your clipboard?")
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Job Application", applicationDetails)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied: $applicationDetails", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun incrementJobViewCount(context: Context) {
        val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        val viewCount = prefs.getInt("job_views", 0) + 1
        prefs.edit().putInt("job_views", viewCount).apply()
    }
}
