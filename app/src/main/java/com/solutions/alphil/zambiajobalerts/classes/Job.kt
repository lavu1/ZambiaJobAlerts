package com.solutions.alphil.zambiajobalerts.classes

import com.google.gson.annotations.SerializedName
import com.solutions.alphil.zambiajobalerts.shared.JobTypeMapper

class Job {
    @SerializedName("id")
    private var id: Int = 0

    @SerializedName("title")
    private var title: RenderedTitle? = null

    @SerializedName("excerpt")
    private var excerpt: RenderedExcerpt? = null

    @SerializedName("content")
    private var content: RenderedContent? = null

    @SerializedName("date")
    private var date: String? = null

    @SerializedName("link")
    private var link: String? = null

    @SerializedName("_embedded")
    private var embedded: Embedded? = null

    @SerializedName("meta")
    private var meta: Map<String, Any>? = null

    @SerializedName("job-categories")
    private var jobCategories: List<Int>? = null

    @SerializedName("job-types")
    private var jobTypes: List<Int>? = null

    @SerializedName("uagb_excerpt")
    private var uagbExcerpt: String? = null

    private var cachedCompany: String? = null
    private var cachedLocation: String? = null
    private var cachedJobType: String? = null
    private var cachedApplication: String? = null

    fun getId(): Int = id

    fun getTitle(): String = title?.rendered.orEmpty()

    fun getContent(): String = content?.rendered.orEmpty()

    fun getExcerpt(): String {
        if (!uagbExcerpt.isNullOrEmpty()) return uagbExcerpt.orEmpty()
        return excerpt?.rendered.orEmpty()
    }

    fun getDate(): String? = date

    fun getLink(): String? = link

    fun getFeaturedImage(): String =
        embedded?.featuredMedia?.firstOrNull()?.sourceUrl.orEmpty()

    fun getCompany(): String {
        cachedCompany?.let { return it }
        return meta?.get("_company_name") as? String ?: ""
    }

    fun getLocation(): String {
        cachedLocation?.let { return it }
        return meta?.get("_job_location") as? String ?: ""
    }

    fun getApplication(): String {
        cachedApplication?.let { return it }
        return meta?.get("_application") as? String ?: ""
    }

    fun getJobType(): String {
        cachedJobType?.let { return it }
        val jobTypeId = jobTypes?.firstOrNull() ?: return ""
        val mappedType = JobTypeMapper.nameForId(jobTypeId)
        if (mappedType.isNotEmpty()) return mappedType

        embedded?.terms?.forEach { termList ->
            termList.forEach { term ->
                if (term.taxonomy == "job_listing_type" && term.id == jobTypeId) {
                    return term.name.orEmpty()
                }
            }
        }

        return ""
    }

    fun getFormattedDate(): String? =
        if (date != null && date.orEmpty().length >= 10) date.orEmpty().substring(0, 10) else date

    class RenderedTitle {
        @SerializedName("rendered")
        var rendered: String? = null

        constructor()

        constructor(rendered: String?) {
            this.rendered = rendered
        }
    }

    class RenderedExcerpt {
        @SerializedName("rendered")
        var rendered: String? = null

        constructor()

        constructor(rendered: String?) {
            this.rendered = rendered
        }
    }

    class RenderedContent {
        @SerializedName("rendered")
        var rendered: String? = null

        constructor()

        constructor(rendered: String?) {
            this.rendered = rendered
        }
    }

    class Embedded {
        @SerializedName("wp:featuredmedia")
        var featuredMedia: MutableList<Media>? = null

        @SerializedName("author")
        var authors: List<Author>? = null

        @SerializedName("wp:term")
        var terms: List<List<Term>>? = null
    }

    class Media {
        @SerializedName("source_url")
        var sourceUrl: String? = null

        constructor()

        constructor(sourceUrl: String?) {
            this.sourceUrl = sourceUrl
        }
    }

    class Author {
        @SerializedName("name")
        var name: String? = null
    }

    class Term {
        @SerializedName("id")
        var id: Int = 0

        @SerializedName("name")
        var name: String? = null

        @SerializedName("taxonomy")
        var taxonomy: String? = null
    }

    companion object {
        @JvmStatic
        fun fromEntity(entity: JobEntity): Job {
            val job = Job()
            job.id = entity.id
            job.title = RenderedTitle(entity.title)
            job.excerpt = RenderedExcerpt(entity.excerpt)
            job.content = RenderedContent(entity.content)
            job.date = entity.date
            job.link = entity.link
            job.embedded = Embedded().apply {
                featuredMedia = mutableListOf(Media(entity.featuredImage))
            }
            job.cachedCompany = entity.company
            job.cachedLocation = entity.location
            job.cachedJobType = entity.jobType
            job.cachedApplication = entity.applicationLink
            return job
        }
    }
}
