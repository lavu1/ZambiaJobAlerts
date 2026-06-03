package com.solutions.alphil.zambiajobalerts.classes

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button as ComposeButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAd
import com.solutions.alphil.zambiajobalerts.R
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeNativeAd

class JobsAdapter(
    private var displayItems: List<Any>,
    private val listener: OnJobClickListener?,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int =
        if (displayItems[position] is NativeAd) VIEW_TYPE_AD else VIEW_TYPE_JOB

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        return if (viewType == VIEW_TYPE_AD) NativeAdComposeViewHolder(view) else JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayItems[position]
        when (holder) {
            is JobViewHolder -> holder.bind(item as Job, listener)
            is NativeAdComposeViewHolder -> holder.bind(item as NativeAd)
        }
    }

    override fun getItemCount(): Int = displayItems.size

    fun updateDisplayList(newItems: List<Any>) {
        displayItems = newItems
        notifyDataSetChanged()
    }

    fun interface OnJobClickListener {
        fun onJobClick(job: Job)
        fun onGenerateCv(job: Job) = Unit
        fun onGenerateCoverLetter(job: Job) = Unit
        fun onToggleSave(job: Job) = Unit
        fun isJobSaved(job: Job): Boolean = false
    }

    private class JobViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

        fun bind(job: Job, listener: OnJobClickListener?) {
            composeView.setContent {
                MaterialTheme {
                    JobListCard(
                        job = job,
                        onApply = { JobActionHandler.applyForJob(composeView.context, job) },
                        onShare = { JobActionHandler.shareJob(composeView.context, job) },
                        onDetails = { listener?.onJobClick(job) },
                        onGenerateCv = { listener?.onGenerateCv(job) },
                        onGenerateCoverLetter = { listener?.onGenerateCoverLetter(job) },
                        isSaved = listener?.isJobSaved(job) == true,
                        onToggleSave = { listener?.onToggleSave(job) },
                    )
                }
            }
        }
    }

    private class NativeAdComposeViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(nativeAd: NativeAd) {
            composeView.setContent {
                MaterialTheme {
                    ComposeNativeAd(nativeAd = nativeAd)
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_JOB = 0
        private const val VIEW_TYPE_AD = 1
    }
}

@Composable
fun JobListCard(
    job: Job,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onGenerateCv: () -> Unit,
    onGenerateCoverLetter: () -> Unit,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
) {
    val primary = Color(0xFF001F3F)
    val orange = Color(0xFFFF851B)
    val hasApplication = job.getApplication().isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = cleanJobText(job.getTitle()),
                color = primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            val company = cleanJobText(job.getCompany())
            val location = cleanJobText(job.getLocation())
            val jobType = cleanJobText(job.getJobType())
            if (company.isNotEmpty()) {
                Text("Company: $company", color = Color(0xFF333333), fontSize = 14.sp)
            }
            if (location.isNotEmpty()) {
                Text("Location: $location", color = Color(0xFF333333), fontSize = 14.sp)
            }
            if (jobType.isNotEmpty()) {
                Text("Type: $jobType", color = Color(0xFF333333), fontSize = 14.sp)
            }

            val excerpt = cleanJobText(job.getExcerpt())
            if (excerpt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = excerpt,
                    color = Color(0xFF333333),
                    fontSize = 14.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val date = cleanJobText(job.getFormattedDate().orEmpty())
            if (date.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(date, color = Color(0xFF666666), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ComposeButton(
                        onClick = onApply,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = orange),
                        enabled = hasApplication,
                    ) {
                        JobButtonContent(label = "Apply", iconRes = R.drawable.ic_action_apply)
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        JobButtonContent(label = "Share", iconRes = R.drawable.ic_action_share)
                    }
                    OutlinedButton(
                        onClick = onDetails,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        JobButtonContent(label = "Details", iconRes = R.drawable.ic_action_details)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ComposeButton(
                        onClick = onGenerateCv,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                    ) {
                        JobButtonContent(label = "CV", iconRes = R.drawable.ic_action_cv)
                    }
                    ComposeButton(
                        onClick = onGenerateCoverLetter,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                    ) {
                        JobButtonContent(label = "Cover", iconRes = R.drawable.ic_action_cover_letter)
                    }
                    OutlinedButton(
                        onClick = onToggleSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        JobButtonContent(
                            label = if (isSaved) "Saved" else "Save",
                            iconRes = if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JobButtonContent(label: String, iconRes: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun cleanJobText(raw: String): String =
    HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()
