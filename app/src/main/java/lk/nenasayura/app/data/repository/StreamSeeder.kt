package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject

object StreamSeeder {

    /** Streams ekapara hadala iwara unama meka false karanna. */
    const val ENABLED = true

    /** true nam thiyena docs overwrite karanawa. */
    private const val OVERWRITE = true

    private const val COLLECTION = "streams"

    private fun id(name: String) =
        name.lowercase().replace("&", "and").replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private class Seed(
        val docId: String,
        val name: String,
        val order: Int,
        val subjects: List<String>,
        val combinations: List<List<String>> = emptyList(),
        val groups: List<List<String>> = emptyList()
    ) {
        fun toMap(): Map<String, Any> {
            val data = mutableMapOf<String, Any>(
                "name" to name,
                "order" to order,
                "subjects" to subjects.associate { id(it) to it }
            )
            if (combinations.isNotEmpty()) {
                data["combinations"] = combinations.map { combo -> combo.joinToString(",") { id(it) } }
            }
            if (groups.isNotEmpty()) {
                data["groups"] = groups.map { slot -> slot.joinToString("|") { id(it) } }
            }
            return data
        }
    }

    private val seeds = listOf(
        // Maths: Combined Mathematics + Physics compulsory, 3weni eka Chemistry / ICT / Higher Maths
        Seed(
            docId = "maths",
            name = "Maths Stream",
            order = 1,
            subjects = listOf(
                "Combined Mathematics", "Physics", "Chemistry",
                "Information & Communication Technology", "Higher Mathematics"
            ),
            groups = listOf(
                listOf("Combined Mathematics"),
                listOf("Physics"),
                listOf("Chemistry", "Information & Communication Technology", "Higher Mathematics")
            )
        ),

        // Biology: Biology + Chemistry compulsory, 3weni eka Physics / Agriculture
        Seed(
            docId = "bio",
            name = "Biology Stream",
            order = 2,
            subjects = listOf("Biology", "Chemistry", "Physics", "Agriculture"),
            groups = listOf(
                listOf("Biology"),
                listOf("Chemistry"),
                listOf("Physics", "Agriculture")
            )
        ),

        // Commerce: Accounting compulsory, Business Studies, Economics, ICT
        Seed(
            docId = "commerce",
            name = "Commerce Stream",
            order = 3,
            subjects = listOf(
                "Accounting", "Business Studies", "Economics",
                "Information & Communication Technology"
            ),
            groups = listOf(
                listOf("Accounting"),
                listOf("Business Studies", "Economics", "Information & Communication Technology"),
                listOf("Business Studies", "Economics", "Information & Communication Technology")
            )
        ),

        // Technology: Science for Technology compulsory
        Seed(
            docId = "technology",
            name = "Technology Stream",
            order = 4,
            subjects = listOf(
                "Bio Systems Technology",
                "Engineering Technology",
                "Science for Technology",
                "Agriculture",
                "Information & Communication Technology"
            ),
            groups = listOf(
                listOf("Engineering Technology", "Bio Systems Technology"),
                listOf("Science for Technology"),
                listOf("Agriculture", "Information & Communication Technology")
            )
        ),

        // Arts: slots 3k, hama slot ekakin ekak
        Seed(
            docId = "arts",
            name = "Arts Stream",
            order = 5,
            subjects = listOf(
                "Sinhala", "Japanese", "Logic", "English Literature",
                "Geography", "Buddhist Civilization", "Home Science",
                "Information & Communication Technology",
                "Media Studies", "Agriculture", "Political Science",
                "History", "Economics", "Art", "Dancing", "Music"
            ),
            groups = listOf(
                listOf("Sinhala", "Japanese", "Logic", "English Literature"),
                listOf(
                    "Geography", "Buddhist Civilization", "Home Science",
                    "Information & Communication Technology"
                ),
                listOf(
                    "Media Studies", "Agriculture", "Political Science",
                    "History", "Economics", "Art", "Dancing", "Music"
                )
            )
        )
    )

    /** Firestore eke naththa streams witharak hadanawa / update karanawa. */
    suspend fun seedMissing() {
        val db = FirebaseFirestore.getInstance()
        for (seed in seeds) {
            val ref = db.collection(COLLECTION).document(seed.docId)
            if (OVERWRITE || !ref.get().await().exists()) {
                ref.set(seed.toMap()).await()
            }
        }
    }

    /** Returns fallback streams for offline/instant display. */
    fun getFallbackStreams(): List<StudyStream> {
        return seeds.map { seed ->
            val subjects = seed.subjects.map { Subject(id = id(it), name = it) }
            val byId = subjects.associateBy { it.id }
            val groups = seed.groups.map { slot ->
                slot.mapNotNull { byId[id(it)] }
            }.filter { it.isNotEmpty() }

            StudyStream(
                id = seed.docId,
                name = seed.name,
                subjects = subjects,
                groups = groups
            )
        }
    }
}
