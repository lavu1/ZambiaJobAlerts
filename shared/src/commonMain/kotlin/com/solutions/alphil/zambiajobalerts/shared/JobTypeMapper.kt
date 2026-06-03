package com.solutions.alphil.zambiajobalerts.shared

object JobTypeMapper {
    private val nameToId = mapOf(
        "Freelance" to 9,
        "Full Time" to 6,
        "Internship" to 10,
        "Part Time" to 7,
        "Temporary" to 8,
        "Consultancy" to 30,
        "Contract" to 31,
        "Consultant" to 30,
        "Tender" to 32,
    )

    private val idToName = nameToId.entries
        .groupBy({ it.value }, { it.key })
        .mapValues { (_, names) -> names.first() }

    fun idForName(name: String): Int? = nameToId[name]

    fun nameForId(id: Int): String = idToName[id].orEmpty()
}
