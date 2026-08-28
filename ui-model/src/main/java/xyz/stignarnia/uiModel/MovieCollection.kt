package xyz.stignarnia.uiModel

data class MovieCollection(
  val id: IdTmdb,
  val name: String,
  val description: String,
  val itemCount: Int,
) {
  companion object {
    val EMPTY = MovieCollection(IdTmdb(-1), "", "", -1)
  }
}
