package xyz.stignarnia.ui_model

enum class UpcomingFilter {
  OFF,
  UPCOMING,
  RELEASED,
  ;

  fun isActive() = this != OFF
}
