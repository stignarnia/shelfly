package xyz.stignarnia.uiModel

enum class ImageSource(
  val key: String,
) {
  TMDB("tmdb"),
  CUSTOM("custom"),
  ;

  companion object {
    fun fromKey(key: String) = entries.first { it.key == key }
  }
}
