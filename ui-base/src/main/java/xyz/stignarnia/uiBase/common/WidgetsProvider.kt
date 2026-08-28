package xyz.stignarnia.uiBase.common

interface WidgetsProvider {
  fun requestShowsWidgetsUpdate()

  fun requestMoviesWidgetsUpdate()

  /**
   * Repaints every widget, whatever it shows.
   * For changes that are about how a widget looks rather than what is in it - the theme, the pure black switch - where leaving the search widget out would strand it on the colours it was last drawn in.
   */
  fun requestAllWidgetsUpdate()
}
