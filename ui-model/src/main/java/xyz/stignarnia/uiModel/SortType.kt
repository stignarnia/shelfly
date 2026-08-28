package xyz.stignarnia.uiModel

enum class SortType(
  val slug: String,
) {
  ASCENDING("asc"),
  DESCENDING("desc"),
  ;

  companion object {
    fun fromSlug(slug: String) = SortType.values().first { it.slug == slug }
  }
}
