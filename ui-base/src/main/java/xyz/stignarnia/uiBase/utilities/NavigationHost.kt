package xyz.stignarnia.uiBase.utilities

import androidx.navigation.NavController

interface NavigationHost {
  fun findNavControl(): NavController?

  fun hideNavigation(animate: Boolean)

  fun showNavigation(animate: Boolean)

  fun navigateToDiscover()
}
