package xyz.stignarnia.uiModel

data class Translation(
  val title: String,
  val overview: String,
  val language: String,
) {
  companion object {
    val EMPTY = Translation("", "", "")
  }

  val hasTitle = title.isNotBlank()
}
