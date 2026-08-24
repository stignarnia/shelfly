package xyz.stignarnia.shelfly.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.Mode.MOVIES
import xyz.stignarnia.common.Mode.SHOWS
import xyz.stignarnia.shelfly.R
import xyz.stignarnia.shelfly.databinding.ActivityMainBinding
import xyz.stignarnia.shelfly.ui.BaseActivity
import xyz.stignarnia.shelfly.ui.main.delegates.MainTipsDelegate
import xyz.stignarnia.shelfly.ui.main.delegates.TipsDelegate
import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeState
import xyz.stignarnia.shelfly.ui.views.welcome.WelcomeView
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkResolver
import xyz.stignarnia.ui_base.common.OnShowsMoviesSyncedListener
import xyz.stignarnia.ui_base.common.OnTabReselectedListener
import xyz.stignarnia.ui_base.events.Event
import xyz.stignarnia.ui_base.events.EventsManager
import xyz.stignarnia.ui_base.events.ShowsMoviesSyncComplete
import xyz.stignarnia.ui_base.network.NetworkStatusProvider
import xyz.stignarnia.ui_base.notifications.SyncNotificationManager
import xyz.stignarnia.ui_base.sync.ShowsMoviesSyncWorker
import xyz.stignarnia.ui_base.utilities.AndroidVersion
import xyz.stignarnia.ui_base.utilities.ModeHost
import xyz.stignarnia.ui_base.utilities.MoviesStatusHost
import xyz.stignarnia.ui_base.utilities.NavigationHost
import xyz.stignarnia.ui_base.utilities.SnackbarHost
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.fadeOut
import xyz.stignarnia.ui_base.utilities.extensions.gone
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visible
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_settings.sections.backup.SettingsBackupFragment

@AndroidEntryPoint
class MainActivity :
  BaseActivity(),
  SnackbarHost,
  NavigationHost,
  ModeHost,
  MoviesStatusHost,
  TipsDelegate by MainTipsDelegate() {

  companion object {
    private const val NAVIGATION_TRANSITION_DURATION_MS = 250L
    private const val ARG_NAVIGATION_VISIBLE = "ARG_NAVIGATION_VISIBLE"

    // Declared by the launcher shortcuts in res/xml/shortcuts.xml.
    private const val ARG_SHORTCUT_PROGRESS = "extraShortcutProgress"
    private const val ARG_SHORTCUT_DISCOVER = "extraShortcutDiscover"
    private const val ARG_SHORTCUT_COLLECTION = "extraShortcutCollection"
    private const val ARG_SHORTCUT_SEARCH = "extraShortcutSearch"
  }

  private val viewModel by viewModels<MainViewModel>()
  private lateinit var binding: ActivityMainBinding

  private var welcomeView: WelcomeView? = null
  private var isNavigationAttached = false
  private var isFirstFrameReady = false
  private var pendingIntent: Intent? = null

  private val navigationHeight by lazy { dimenToPx(R.dimen.bottomNavigationHeight) }
  private val navigationPadding by lazy { dimenToPx(R.dimen.spaceMedium) }
  private val decelerateInterpolator by lazy { DecelerateInterpolator(2F) }

  @Inject lateinit var workManager: WorkManager
  @Inject lateinit var eventsManager: EventsManager
  @Inject lateinit var deepLinkResolver: DeepLinkResolver
  @Inject lateinit var networkStatusProvider: NetworkStatusProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      // auto, not dark, so the status bar icons flip to dark glyphs under a light theme.
      // The bar itself stays transparent either way, because content scrolls under it.
      statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
      // The navigation bar keeps its translucent black scrim in every theme.
      // It sits over content rather than over a surface, so it is not the theme's to colour - the same reasoning that keeps the poster overlays fixed.
      navigationBarStyle = SystemBarStyle.dark(ContextCompat.getColor(this, R.color.colorBlackTranslucentMedium)),
    )

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    registerTipsDelegate(viewModel, binding)

    holdFirstFrame()
    setupViewModel()
    setupView()
    setupNetworkObserver()

    restoreState(savedInstanceState)
    onNewIntent(intent)
  }

  override fun onStart() {
    super.onStart()
    ShowsMoviesSyncWorker.schedule(workManager)
  }

  override fun onResume() {
    super.onResume()
    setupBackPressed()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    // Stored, so a recreation - a locale or a theme change - resumes from the intent that is actually on screen rather than the one the app was launched with.
    // Every branch below takes what it acts on off the intent as it goes, so the replay that recreation triggers finds nothing left to do.
    intent?.let { setIntent(it) }
    // Every one of these ends up at the navigation controller, and there is none while the welcome flow owns the screen.
    // The intent waits for it instead of being dropped.
    if (!isNavigationAttached) {
      pendingIntent = intent
      return
    }
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getBooleanExtra(SyncNotificationManager.EXTRA_OPEN_WEBDAV_SETTINGS, false) == true) {
      intent.removeExtra(SyncNotificationManager.EXTRA_OPEN_WEBDAV_SETTINGS)
      navigateToWebDavSetup()
      return
    }
    handleAppShortcut(intent)
    handleNotification(intent) { hideNavigation(false) }
    handleDeepLink(intent)
  }

  override fun onDestroy() {
    lifecycle.removeObserver(networkStatusProvider)
    super.onDestroy()
  }

  private fun setupViewModel() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { render(it) } }
        launch { eventsManager.events.collect { handleEvent(it) } }
      }
    }
    viewModel.initialize()
    viewModel.refreshBackupExportSchedule()
  }

  private fun setupView() {
    with(binding) {
      bottomNavigationWrapper.doOnApplyWindowInsets { _, insets, _, margins ->
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        bottomNavigationWrapper.updateLayoutParams<MarginLayoutParams> {
          bottomMargin = margins.bottom + inset
        }
      }
      bottomMenuView.isModeMenuEnabled = hasMoviesEnabled()
      bottomMenuView.onModeSelected = { setMode(it) }
      viewMask.onClick { /* NOOP */ }
    }
  }

  /**
   * Nothing is drawn until the welcome flow knows whether it has anything to show.
   * Building its queue costs a database read, and drawing before it is back puts the main UI on screen for a frame or two, only for the first step to cover it - which is what made the flow look like an overlay rather than the entry point it is.
   * The launch window stays up in the meantime.
   */
  private fun holdFirstFrame() {
    val root = binding.root
    root.viewTreeObserver.addOnPreDrawListener(
      object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
          if (!isFirstFrameReady) return false
          root.viewTreeObserver.removeOnPreDrawListener(this)
          return true
        }
      },
    )
  }

  /**
   * The navigation host is created here rather than at inflation, so that on a launch that opens with the welcome flow the main UI is never built - let alone loaded - behind it.
   *
   * On a configuration change the fragment manager has already restored it, and restoring is what [setupNavigation] expects, so it is only added when genuinely absent.
   */
  private fun attachNavigation() {
    if (isNavigationAttached) return
    isNavigationAttached = true
    if (findNavHostFragment() == null) {
      supportFragmentManager
        .beginTransaction()
        .replace(R.id.navigationHost, NavHostFragment(), null)
        .commitNow()
    }
    setupNavigation()
    // The start destination is added through the child manager on a posted commit.
    // Running it here means the frame released next has the destination in it rather than an empty container.
    findNavHostFragment()?.childFragmentManager?.executePendingTransactions()
    pendingIntent?.let {
      pendingIntent = null
      handleIntent(it)
    }
  }

  @OptIn(FlowPreview::class)
  private fun setupNetworkObserver() {
    lifecycle.addObserver(networkStatusProvider)
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          networkStatusProvider.status
            .debounce(3.seconds)
            .collect {
              binding.statusView.visibleIf(!it)
              binding.statusView.text = getString(R.string.errorNoInternetConnection)
            }
        }
      }
    }
  }

  private fun setupNavigation() {
    findNavControl()?.run {
      // Added before the graph, so the start destination lights its own tab on the first pass.
      addOnDestinationChangedListener { _, destination, _ -> syncBottomMenu(destination.id) }
      val graph = navInflater.inflate(R.navigation.navigation_graph).apply {
        val destination = when (viewModel.getMode()) {
          SHOWS -> R.id.progressMainFragment
          MOVIES -> R.id.progressMoviesMainFragment
        }
        setStartDestination(destination)
      }
      setGraph(graph, Bundle.EMPTY)
    }
    with(binding.bottomMenuView.binding.bottomNavigationView) {
      setOnItemSelectedListener { item ->
        if (menuItemFor(findNavControl()?.currentDestination?.id) == item.itemId) {
          doForFragments { (it as? OnTabReselectedListener)?.onTabReselected() }
          return@setOnItemSelectedListener true
        }

        val target = when (item.itemId) {
          R.id.menuProgress -> getMenuProgressAction()
          R.id.menuDiscover -> getMenuDiscoverAction()
          R.id.menuCollection -> getMenuCollectionAction()
          else -> throw IllegalStateException("Invalid menu item.")
        }

        navigateToTab(target)

        return@setOnItemSelectedListener true
      }
    }
  }

  /**
   * The bar's checked item follows the destination rather than driving it.
   * A tab tap, an app shortcut, a widget, a notification and the back button are then all one navigation call, and none of them has to reach into the bar and fake a tap to get the highlight to move.
   */
  private fun syncBottomMenu(destinationId: Int) {
    val itemId = menuItemFor(destinationId) ?: return
    with(binding.bottomMenuView.binding.bottomNavigationView.menu) {
      // The others are cleared first and the target set last.
      // An exclusively checkable group reads setChecked(false) as "check me instead", so only the last call can be relied on to decide the outcome - and this order lands on the same result whether the group is exclusive or not.
      forEach { if (it.itemId != itemId) it.isChecked = false }
      findItem(itemId)?.isChecked = true
    }
  }

  /** The bar item standing for [destinationId], or null where the bar has no say - search, settings, details, dialogs. */
  private fun menuItemFor(destinationId: Int?) =
    when (destinationId) {
      R.id.progressMainFragment, R.id.progressMoviesMainFragment -> R.id.menuProgress
      R.id.discoverFragment, R.id.discoverMoviesFragment -> R.id.menuDiscover
      R.id.followedShowsFragment, R.id.followedMoviesFragment, R.id.listsFragment -> R.id.menuCollection
      else -> null
    }

  /** A tab's own top level action, which already clears the stack down to it. */
  private fun navigateToTab(actionId: Int) {
    findNavControl()?.navigate(actionId)
    showNavigation(true)
  }

  /** Search draws no bottom bar of its own, so whoever opens it puts the bar away - what Discover does before handing over. */
  private fun navigateToSearch() {
    findNavControl()?.run {
      try {
        navigate(R.id.actionNavigateSearchFragment)
        hideNavigation(false)
      } catch (error: Throwable) {
      }
    }
  }

  private fun setupBackPressed() {
    onBackPressedDispatcher.addCallback(this@MainActivity) {
      if (viewModel.onWelcomeBack()) {
        return@addCallback
      }
      findNavControl()?.run {
        when (currentDestination?.id) {
          R.id.discoverFragment,
          R.id.discoverMoviesFragment,
          R.id.followedShowsFragment,
          R.id.followedMoviesFragment,
          R.id.listsFragment,
          -> {
            navigateToTab(getMenuProgressAction())
          }
          else -> {
            remove()
            onBackPressedDispatcher.onBackPressed()
          }
        }
      }
    }
  }

  override fun hideNavigation(animate: Boolean) {
    with(binding) {
      hideAllTips()
      bottomMenuView.isEnabled = false
      snackbarHost.translationY = 0F
      bottomNavigationWrapper
        .animate()
        .alpha(0F)
        .translationYBy(navigationHeight.toFloat() / 2)
        .setDuration(if (animate) NAVIGATION_TRANSITION_DURATION_MS else 0)
        .setInterpolator(decelerateInterpolator)
        .withEndAction { bottomMenuView.gone() }
        .start()
    }
  }

  override fun showNavigation(animate: Boolean) {
    with(binding) {
      showAllTips()
      bottomMenuView.visible()
      bottomMenuView.isEnabled = true
      snackbarHost.translationY = -(navigationHeight + (navigationPadding.toFloat()))
      bottomNavigationWrapper
        .animate()
        .alpha(1F)
        .translationY(0F)
        .setDuration(if (animate) NAVIGATION_TRANSITION_DURATION_MS else 0)
        .setInterpolator(decelerateInterpolator)
        .start()
    }
  }

  override fun navigateToDiscover() {
    navigateToTab(getMenuDiscoverAction())
  }

  override fun setMode(
    mode: Mode,
    force: Boolean,
  ) {
    if (force || viewModel.getMode() != mode) {
      viewModel.setMode(mode)
      // The mode menu only exists on the bar, and the bar only stands over a tab, so anywhere else there is no tab to swap.
      val target = when (menuItemFor(findNavControl()?.currentDestination?.id)) {
        R.id.menuDiscover -> getMenuDiscoverAction()
        R.id.menuCollection -> getMenuCollectionAction()
        R.id.menuProgress -> getMenuProgressAction()
        else -> return
      }
      findNavControl()?.navigate(target)
    }
  }

  override fun getMode() = viewModel.getMode()

  override fun hasMoviesEnabled() = viewModel.hasMoviesEnabled()

  private fun render(uiState: MainUiState) {
    with(binding) {
      uiState.run {
        isLoading.let {
          mainProgress.visibleIf(it)
        }
        showMask.let {
          viewMask.visibleIf(it)
        }
        renderWelcome(welcome, isWelcomeResolved)
        requestNotifications?.let {
          if (it.consume() == true) requestNotificationsPermission()
        }
        // The rest need somewhere to navigate to.
        // Left unconsumed until there is - renderWelcome attaches it as the flow ends, so a step that asked for one of these is honoured on this same pass.
        if (!isNavigationAttached) return@run
        showDiscover?.let {
          if (it.consume() == true) navigateToDiscover()
        }
        openSettings?.let {
          if (it.consume() == true) navigateToWebDavSetup()
        }
        openLink?.let { event ->
          event.consume()?.let { bundle ->
            findNavHostFragment()?.findNavController()?.let { nav ->
              bundle.show?.let {
                deepLinkResolver.resolveDestination(nav, it)
              }
              bundle.movie?.let {
                deepLinkResolver.resolveDestination(nav, it)
              }
            }
          }
        }
      }
    }
  }

  private val notificationsPermissionLauncher = registerForActivityResult(RequestPermission()) {
    viewModel.onNotificationsPermissionResult(it)
  }

  private fun requestNotificationsPermission() {
    if (!AndroidVersion.isAtLeastAndroid13) {
      viewModel.onNotificationsPermissionResult(true)
      return
    }
    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
  }

  /**
   * The WebDAV setup form already exists under Settings, so the welcome step and the sync failure notification hand the user over to it - opened, not just nearby - rather than growing a second copy of it.
   *
   * Settings has a top level action of its own, so the notification opens it from wherever the user last left the app - a show, a search, the gallery, Settings itself - without unwinding anything to get there.
   * Like search, it draws no bottom bar, so the bar goes away with the move, the same way every tab root puts it away before opening Settings.
   */
  private fun navigateToWebDavSetup() {
    findNavControl()?.run {
      try {
        navigate(
          R.id.actionNavigateSettingsFragment,
          Bundle().apply { putBoolean(SettingsBackupFragment.ARG_OPEN_WEB_DAV, true) },
        )
        hideNavigation(false)
      } catch (error: Throwable) {
      }
    }
  }

  /**
   * The step is state, not an event, so this is safe to run on every emission and after a configuration change - including the restart that applying a new locale triggers midway through the flow.
   */
  private fun renderWelcome(
    state: WelcomeState?,
    isResolved: Boolean,
  ) {
    // A null step means "nothing left to show" only once the queue exists; before that it just means the answer is still on its way.
    if (!isResolved) return
    if (state == null) {
      attachNavigation()
      welcomeView?.let { if (it.isVisible) it.fadeOut() }
    } else {
      with(welcomeView()) {
        render(state)
        // Not faded in: on the launch it opens it is the first thing on screen, and there is nothing underneath for it to arrive on top of.
        visible()
      }
    }
    isFirstFrameReady = true
  }

  /** Inflated on demand, so a launch with no steps due never builds it at all. */
  private fun welcomeView(): WelcomeView =
    welcomeView ?: (binding.welcomeViewStub.inflate() as WelcomeView).apply {
      onPrimaryClick = { viewModel.onWelcomePrimary() }
      onSecondaryClick = { viewModel.onWelcomeSecondary() }
      onBackClick = { viewModel.onWelcomeBack() }
      onApiKeyChanged = { viewModel.onApiKeyDraftChanged(it) }
      welcomeView = this
    }

  @SuppressLint("MissingSuperCall")
  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(ARG_NAVIGATION_VISIBLE, binding.bottomNavigationWrapper.translationY == 0F)
    super.onSaveInstanceState(outState)
  }

  private fun restoreState(savedInstanceState: Bundle?) {
    val isNavigationVisible = savedInstanceState?.getBoolean(ARG_NAVIGATION_VISIBLE, true) ?: true
    // Not animated.
    // The bar starts visible in the layout, so animating it away means it is on screen for the length of the transition - a flash of navigation over a screen that had none before the Activity was recreated.
    if (!isNavigationVisible) hideNavigation(false)
  }

  private fun doForFragments(action: (Fragment) -> Unit) {
    findNavControl()?.currentDestination?.id?.let {
      val navHost = supportFragmentManager.findFragmentById(R.id.navigationHost)
      navHost?.childFragmentManager?.primaryNavigationFragment?.let { action(it) }
    }
  }

  private fun handleEvent(event: Event) {
    when (event) {
      is ShowsMoviesSyncComplete -> {
        if (event.count > 0) {
          doForFragments { (it as? OnShowsMoviesSyncedListener)?.onShowsMoviesSyncFinished() }
        }
        viewModel.refreshAnnouncements()
      }
      else -> {
        Timber.d("Event ignored. Noop.")
      }
    }
  }

  private fun handleAppShortcut(intent: Intent?) {
    val extras = intent?.extras ?: return
    // The key comes off the intent rather than off the extras, which are a copy: onCreate replays the stored intent after a recreation, and a shortcut must not fire a second time.
    when {
      extras.containsKey(ARG_SHORTCUT_PROGRESS) -> {
        intent.removeExtra(ARG_SHORTCUT_PROGRESS)
        navigateToTab(getMenuProgressAction())
      }

      extras.containsKey(ARG_SHORTCUT_DISCOVER) -> {
        intent.removeExtra(ARG_SHORTCUT_DISCOVER)
        navigateToTab(getMenuDiscoverAction())
      }

      extras.containsKey(ARG_SHORTCUT_COLLECTION) -> {
        intent.removeExtra(ARG_SHORTCUT_COLLECTION)
        navigateToTab(getMenuCollectionAction())
      }

      extras.containsKey(ARG_SHORTCUT_SEARCH) -> {
        intent.removeExtra(ARG_SHORTCUT_SEARCH)
        navigateToSearch()
      }
    }
  }

  override fun handleSearchWidgetClick() = navigateToSearch()

  private fun getMenuDiscoverAction() =
    when (viewModel.getMode()) {
      SHOWS -> R.id.actionNavigateDiscoverFragment
      MOVIES -> R.id.actionNavigateDiscoverMoviesFragment
    }

  private fun getMenuCollectionAction() =
    when (viewModel.getMode()) {
      SHOWS -> R.id.actionNavigateFollowedShowsFragment
      MOVIES -> R.id.actionNavigateFollowedMoviesFragment
    }

  private fun getMenuProgressAction() =
    when (viewModel.getMode()) {
      SHOWS -> R.id.actionNavigateProgressFragment
      MOVIES -> R.id.actionNavigateProgressMoviesFragment
    }

  private fun handleDeepLink(intent: Intent?) {
    deepLinkResolver.findSource(intent)?.let {
      // Consumed for the same reason the extras are: the replay after a recreation must not open the link a second time.
      intent?.data = null
      viewModel.openDeepLink(it)
    }
  }

  override fun findNavControl() = findNavHostFragment()?.findNavController()

  override fun provideSnackbarLayout(): ViewGroup = binding.snackbarHost
}
