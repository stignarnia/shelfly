package xyz.stignarnia.shelfly.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.DecelerateInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.Mode.MOVIES
import xyz.stignarnia.common.Mode.SHOWS
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.shelfly.R
import xyz.stignarnia.shelfly.databinding.ActivityMainBinding
import xyz.stignarnia.shelfly.ui.BaseActivity
import xyz.stignarnia.shelfly.ui.main.delegates.MainTipsDelegate
import xyz.stignarnia.shelfly.ui.main.delegates.TipsDelegate
import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeState
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkResolver
import xyz.stignarnia.ui_settings.sections.backup.SettingsBackupFragment
import xyz.stignarnia.ui_base.common.OnShowsMoviesSyncedListener
import xyz.stignarnia.ui_base.common.OnTabReselectedListener
import xyz.stignarnia.ui_base.events.Event
import xyz.stignarnia.ui_base.events.EventsManager
import xyz.stignarnia.ui_base.events.ShowsMoviesSyncComplete
import xyz.stignarnia.ui_base.network.NetworkStatusProvider
import xyz.stignarnia.ui_base.sync.ShowsMoviesSyncWorker
import xyz.stignarnia.ui_base.utilities.ModeHost
import xyz.stignarnia.ui_base.utilities.MoviesStatusHost
import xyz.stignarnia.ui_base.utilities.NavigationHost
import xyz.stignarnia.ui_base.utilities.SnackbarHost
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.fadeOut
import xyz.stignarnia.ui_base.utilities.extensions.gone
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visible
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
  }

  private val viewModel by viewModels<MainViewModel>()
  private lateinit var binding: ActivityMainBinding

  private val navigationHeight by lazy { dimenToPx(R.dimen.bottomNavigationHeight) }
  private val navigationPadding by lazy { dimenToPx(R.dimen.spaceMedium) }
  private val decelerateInterpolator by lazy { DecelerateInterpolator(2F) }

  @Inject lateinit var workManager: WorkManager
  @Inject lateinit var eventsManager: EventsManager
  @Inject lateinit var deepLinkResolver: DeepLinkResolver
  @Inject lateinit var settingsRepository: SettingsRepository
  @Inject lateinit var networkStatusProvider: NetworkStatusProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(ContextCompat.getColor(this, R.color.colorBlackTranslucentMedium)),
    )

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    registerTipsDelegate(viewModel, binding)

    setupViewModel()
    setupNavigation()
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
    handleAppShortcut(intent)
    handleNotification(intent?.extras) { hideNavigation(false) }
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
      with(welcomeView) {
        onPrimaryClick = { viewModel.onWelcomePrimary() }
        onSecondaryClick = { viewModel.onWelcomeSecondary() }
        onBackClick = { viewModel.onWelcomeBack() }
        onApiKeyChanged = { viewModel.onApiKeyDraftChanged(it) }
      }
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
      val graph = navInflater.inflate(R.navigation.navigation_graph).apply {
        val destination = when (viewModel.getMode()) {
          SHOWS -> R.id.progressMainFragment
          MOVIES -> R.id.progressMoviesMainFragment
          else -> throw IllegalStateException()
        }
        setStartDestination(destination)
      }
      setGraph(graph, Bundle.EMPTY)
    }
    with(binding.bottomMenuView.binding.bottomNavigationView) {
      setOnItemSelectedListener { item ->
        if (selectedItemId == item.itemId) {
          doForFragments { (it as? OnTabReselectedListener)?.onTabReselected() }
          return@setOnItemSelectedListener true
        }

        val target = when (item.itemId) {
          R.id.menuProgress -> getMenuProgressAction()
          R.id.menuDiscover -> getMenuDiscoverAction()
          R.id.menuCollection -> getMenuCollectionAction()
          else -> throw IllegalStateException("Invalid menu item.")
        }

        findNavControl()?.navigate(target)
        showNavigation(true)

        return@setOnItemSelectedListener true
      }
    }
  }

  private fun setupBackPressed() {
    with(binding) {
      onBackPressedDispatcher.addCallback(this@MainActivity) {
        if (viewModel.onWelcomeBack()) {
          return@addCallback
        }
        if (tutorialView.isVisible) {
          tutorialView.fadeOut()
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
              bottomMenuView.binding.bottomNavigationView.selectedItemId = R.id.menuProgress
            }
            else -> {
              remove()
              super.onBackPressed()
            }
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
    with(binding) {
      bottomMenuView.isEnabled = true
      bottomMenuView.binding.bottomNavigationView.selectedItemId = R.id.menuDiscover
    }
  }

  override fun setMode(
    mode: Mode,
    force: Boolean,
  ) {
    if (force || viewModel.getMode() != mode) {
      viewModel.setMode(mode)
      val target = when (binding.bottomMenuView.binding.bottomNavigationView.selectedItemId) {
        R.id.menuDiscover -> getMenuDiscoverAction()
        R.id.menuCollection -> getMenuCollectionAction()
        R.id.menuProgress -> getMenuProgressAction()
        else -> 0
      }
      if (target != 0) {
        findNavControl()?.navigate(target)
      }
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
        renderWelcome(welcome)
        showDiscover?.let {
          if (it.consume() == true) navigateToDiscover()
        }
        requestNotifications?.let {
          if (it.consume() == true) requestNotificationsPermission()
        }
        openSettings?.let {
          if (it.consume() == true) navigateToWebDavSetup()
        }
        openLink?.let { event ->
          event.consume()?.let { bundle ->
            findNavHostFragment()?.findNavController()?.let { nav ->
              bundle.show?.let {
                deepLinkResolver.resolveDestination(nav, bottomMenuView.binding.bottomNavigationView, it)
              }
              bundle.movie?.let {
                deepLinkResolver.resolveDestination(nav, bottomMenuView.binding.bottomNavigationView, it)
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      viewModel.onNotificationsPermissionResult(true)
      return
    }
    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
  }

  /**
   * The WebDAV setup form already exists under Settings, so the welcome step
   * hands the user over to it - opened, not just nearby - rather than growing a
   * second copy of it.
   */
  private fun navigateToWebDavSetup() {
    findNavControl()?.run {
      val target = when (currentDestination?.id) {
        R.id.discoverFragment -> R.id.actionDiscoverFragmentToSettingsFragment
        R.id.discoverMoviesFragment -> R.id.actionDiscoverMoviesFragmentToSettingsFragment
        R.id.progressMainFragment -> R.id.actionProgressFragmentToSettingsFragment
        R.id.progressMoviesMainFragment -> R.id.actionProgressMoviesFragmentToSettingsFragment
        R.id.followedShowsFragment -> R.id.actionFollowedShowsFragmentToSettingsFragment
        R.id.followedMoviesFragment -> R.id.actionFollowedMoviesFragmentToSettingsFragment
        R.id.listsFragment -> R.id.actionListsFragmentToSettingsFragment
        else -> return
      }
      navigate(target, bundleOf(SettingsBackupFragment.ARG_OPEN_WEB_DAV to true))
    }
  }

  /**
   * The step is state, not an event, so this is safe to run on every emission and
   * after a configuration change - including the restart that applying a new
   * locale triggers midway through the flow.
   */
  private fun renderWelcome(state: WelcomeState?) {
    with(binding.welcomeView) {
      if (state == null) {
        if (isVisible) fadeOut()
        return
      }
      render(state)
      if (!isVisible) fadeIn()
    }
  }

  @SuppressLint("MissingSuperCall")
  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(ARG_NAVIGATION_VISIBLE, binding.bottomNavigationWrapper.translationY == 0F)
    super.onSaveInstanceState(outState)
  }

  private fun restoreState(savedInstanceState: Bundle?) {
    val isNavigationVisible = savedInstanceState?.getBoolean(ARG_NAVIGATION_VISIBLE, true) ?: true
    if (!isNavigationVisible) hideNavigation(true)
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
    when {
      intent == null -> {
        return
      }

      intent.extras?.containsKey("extraShortcutProgress") == true -> {
        binding.bottomMenuView.binding.bottomNavigationView.selectedItemId = R.id.menuProgress
      }

      intent.extras?.containsKey("extraShortcutDiscover") == true -> {
        binding.bottomMenuView.binding.bottomNavigationView.selectedItemId = R.id.menuDiscover
      }

      intent.extras?.containsKey("extraShortcutCollection") == true -> {
        binding.bottomMenuView.binding.bottomNavigationView.selectedItemId = R.id.menuCollection
      }

      intent.extras?.containsKey("extraShortcutSearch") == true -> {
        binding.bottomMenuView.binding.bottomNavigationView.selectedItemId = R.id.menuDiscover
        val action = when (viewModel.getMode()) {
          SHOWS -> R.id.actionDiscoverFragmentToSearchFragment
          MOVIES -> R.id.actionDiscoverMoviesFragmentToSearchFragment
          else -> throw IllegalStateException()
        }
        findNavControl()?.navigate(action)
      }
    }
  }

  override fun handleSearchWidgetClick(bundle: Bundle?) {
    findNavHostFragment()?.findNavController()?.run {
      try {
        when (currentDestination?.id) {
          R.id.searchFragment -> return@run
          R.id.showDetailsFragment, R.id.movieDetailsFragment -> navigateUp()
        }
        if (currentDestination?.id != R.id.discoverFragment) {
          binding.bottomMenuView.binding.bottomNavigationView.selectedItemId = R.id.menuDiscover
        }
        when (currentDestination?.id) {
          R.id.discoverFragment -> navigate(R.id.actionDiscoverFragmentToSearchFragment)
          R.id.discoverMoviesFragment -> navigate(R.id.actionDiscoverMoviesFragmentToSearchFragment)
        }
        bundle?.clear()
      } catch (error: Throwable) {
      }
    }
  }

  private fun getMenuDiscoverAction() =
    when (viewModel.getMode()) {
      SHOWS -> R.id.actionNavigateDiscoverFragment
      MOVIES -> R.id.actionNavigateDiscoverMoviesFragment
      else -> throw IllegalStateException()
    }

  private fun getMenuCollectionAction() =
    when (viewModel.getMode()) {
      SHOWS -> R.id.actionNavigateFollowedShowsFragment
      MOVIES -> R.id.actionNavigateFollowedMoviesFragment
      else -> throw IllegalStateException()
    }

  private fun getMenuProgressAction() =
    when (viewModel.getMode()) {
      SHOWS -> R.id.actionNavigateProgressFragment
      MOVIES -> R.id.actionNavigateProgressMoviesFragment
      else -> throw IllegalStateException()
    }

  private fun handleDeepLink(intent: Intent?) {
    deepLinkResolver.findSource(intent)?.let {
      viewModel.openDeepLink(it)
    }
  }

  override fun findNavControl() = findNavHostFragment()?.findNavController()

  override fun provideSnackbarLayout(): ViewGroup = binding.snackbarHost
}
