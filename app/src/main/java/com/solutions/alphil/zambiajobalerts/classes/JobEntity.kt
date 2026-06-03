package com.solutions.alphil.zambiajobalerts.classes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey
    var id: Int = 0,
    var title: String? = null,
    var excerpt: String? = null,
    var content: String? = null,
    var date: String? = null,
    var link: String? = null,
    var featuredImage: String? = null,
    var company: String? = null,
    var location: String? = null,
    var jobType: String? = null,
    var applicationLink: String? = null,
)
