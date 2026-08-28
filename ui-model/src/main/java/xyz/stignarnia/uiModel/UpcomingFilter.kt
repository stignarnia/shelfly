package xyz.stignarnia.uiModel

enum class UpcomingFilter {
  OFF,
  UPCOMING,
  RELEASED,
  ;

  fun isActive() = this != OFF
}
