package xyz.stignarnia.uiSettings.sections.spoilers

data class SettingsSpoilersUiState(
  val hasShowsSettingActive: Boolean = false,
  val hasMoviesSettingActive: Boolean = false,
  val hasEpisodesSettingActive: Boolean = false,
  val isTapToReveal: Boolean = false,
)
