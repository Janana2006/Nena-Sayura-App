package lk.nenasayura.app.util

/** Helpers for YouTube links a teacher pastes when uploading a video. */
object YouTubeLinks {

    private val patterns = listOf(
        // https://youtu.be/VIDEO_ID
        Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
        // https://www.youtube.com/watch?v=VIDEO_ID  (also m.youtube.com, music.youtube.com)
        Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
        // https://www.youtube.com/embed/VIDEO_ID, /shorts/VIDEO_ID, /live/VIDEO_ID
        Regex("""youtube(?:-nocookie)?\.com/(?:embed|shorts|live|v)/([A-Za-z0-9_-]{11})""")
    )

    /** The 11 character video id, or null when [url] is not a YouTube video link. */
    fun extractId(url: String): String? {
        val text = url.trim()
        if (!text.contains("youtu", ignoreCase = true)) return null
        return patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1) }
    }

    /** The normal watch page, used for the "Open in YouTube" button. */
    fun watchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"
}