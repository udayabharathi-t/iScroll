package app.lapse.engine

object Packages {
    const val INSTAGRAM = "com.instagram.android"
    const val INSTAGRAM_LITE = "com.instagram.lite"
    const val YOUTUBE = "com.google.android.youtube"
    const val REDDIT = "com.reddit.frontpage"

    val tracked: Set<String> = setOf(INSTAGRAM, INSTAGRAM_LITE, YOUTUBE, REDDIT)

    fun surfaceOf(packageName: String): Surface = when (packageName) {
        INSTAGRAM, INSTAGRAM_LITE -> Surface.INSTAGRAM
        YOUTUBE -> Surface.YOUTUBE
        REDDIT -> Surface.REDDIT
        else -> Surface.NONE
    }

    fun isTracked(packageName: String): Boolean = surfaceOf(packageName) != Surface.NONE
}
