package com.android.customization.testing

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.customization.model.theme.OverlayManagerCompat
import com.android.customization.model.theme.ThemeBundleProvider
import com.android.customization.model.theme.ThemeManager
import com.android.customization.module.CustomizationInjector
import com.android.customization.module.CustomizationPreferences
import com.android.customization.module.ThemesUserEventLogger
import com.android.customization.picker.clock.data.repository.ClockRegistryProvider
import com.android.customization.picker.clock.data.repository.FakeClockPickerRepository
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.viewmodel.ClockCarouselViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSectionViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.color.data.repository.ColorPickerRepositoryImpl
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.ui.viewmodel.ColorPickerViewModel
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordanceSnapshotRestorer
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.WallpaperColorsViewModel
import com.android.wallpaper.module.DrawableLayerResolver
import com.android.wallpaper.module.PackageStatusNotifier
import com.android.wallpaper.module.UserEventLogger
import com.android.wallpaper.picker.undo.domain.interactor.SnapshotRestorer
import com.android.wallpaper.testing.TestInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

/** Test implementation of the dependency injector. */
class TestCustomizationInjector : TestInjector(), CustomizationInjector {
    private var customizationPreferences: CustomizationPreferences? = null
    private var themeManager: ThemeManager? = null
    private var packageStatusNotifier: PackageStatusNotifier? = null
    private var drawableLayerResolver: DrawableLayerResolver? = null
    private var userEventLogger: UserEventLogger? = null
    private var keyguardQuickAffordancePickerInteractor: KeyguardQuickAffordancePickerInteractor? =
        null
    private var flags: BaseFlags? = null
    private var customizationProviderClient: CustomizationProviderClient? = null
    private var keyguardQuickAffordanceSnapshotRestorer: KeyguardQuickAffordanceSnapshotRestorer? =
        null
    private var clockRegistryProvider: ClockRegistryProvider? = null
    private var clockPickerInteractor: ClockPickerInteractor? = null
    private var clockSectionViewModel: ClockSectionViewModel? = null
    private var clockViewFactory: ClockViewFactory? = null
    private var colorPickerInteractor: ColorPickerInteractor? = null
    private var colorPickerViewModelFactory: ColorPickerViewModel.Factory? = null
    private var clockCarouselViewModel: ClockCarouselViewModel? = null
    private var clockSettingsViewModelFactory: ClockSettingsViewModel.Factory? = null

    override fun getCustomizationPreferences(context: Context): CustomizationPreferences {
        return customizationPreferences
            ?: TestDefaultCustomizationPreferences(context).also { customizationPreferences = it }
    }

    override fun getThemeManager(
        provider: ThemeBundleProvider,
        activity: FragmentActivity,
        overlayManagerCompat: OverlayManagerCompat,
        logger: ThemesUserEventLogger
    ): ThemeManager {
        return themeManager
            ?: TestThemeManager(provider, activity, overlayManagerCompat, logger).also {
                themeManager = it
            }
    }

    override fun getPackageStatusNotifier(context: Context): PackageStatusNotifier {
        return packageStatusNotifier
            ?: TestPackageStatusNotifier().also {
                packageStatusNotifier = TestPackageStatusNotifier()
            }
    }

    override fun getDrawableLayerResolver(): DrawableLayerResolver {
        return drawableLayerResolver
            ?: TestDrawableLayerResolver().also { drawableLayerResolver = it }
    }

    override fun getUserEventLogger(context: Context): UserEventLogger {
        return userEventLogger ?: TestThemesUserEventLogger().also { userEventLogger = it }
    }

    override fun getKeyguardQuickAffordancePickerInteractor(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        return keyguardQuickAffordancePickerInteractor
            ?: createCustomizationProviderClient(context).also {
                keyguardQuickAffordancePickerInteractor = it
            }
    }

    private fun createCustomizationProviderClient(
        context: Context
    ): KeyguardQuickAffordancePickerInteractor {
        val client: CustomizationProviderClient =
            CustomizationProviderClientImpl(context, Dispatchers.IO)
        return KeyguardQuickAffordancePickerInteractor(
            KeyguardQuickAffordancePickerRepository(client, Dispatchers.IO),
            client
        ) { getKeyguardQuickAffordanceSnapshotRestorer(context) }
    }

    override fun getFlags(): BaseFlags {
        return flags ?: object : BaseFlags() {}.also { flags = it }
    }

    override fun getSnapshotRestorers(context: Context): Map<Int, SnapshotRestorer> {
        val restorers: MutableMap<Int, SnapshotRestorer> = HashMap()
        restorers[KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER] =
            getKeyguardQuickAffordanceSnapshotRestorer(context)
        return restorers
    }

    /** Returns the [CustomizationProviderClient]. */
    private fun getKeyguardQuickAffordancePickerProviderClient(
        context: Context
    ): CustomizationProviderClient {
        return customizationProviderClient
            ?: CustomizationProviderClientImpl(context, Dispatchers.IO).also {
                customizationProviderClient = it
            }
    }

    private fun getKeyguardQuickAffordanceSnapshotRestorer(
        context: Context
    ): KeyguardQuickAffordanceSnapshotRestorer {
        return keyguardQuickAffordanceSnapshotRestorer
            ?: KeyguardQuickAffordanceSnapshotRestorer(
                    getKeyguardQuickAffordancePickerInteractor(context),
                    getKeyguardQuickAffordancePickerProviderClient(context)
                )
                .also { keyguardQuickAffordanceSnapshotRestorer = it }
    }

    override fun getClockRegistryProvider(context: Context): ClockRegistryProvider {
        return clockRegistryProvider
            ?: ClockRegistryProvider(context, GlobalScope, Dispatchers.Main, Dispatchers.IO).also {
                clockRegistryProvider = it
            }
    }

    override fun getClockPickerInteractor(
        context: Context,
        clockRegistry: ClockRegistry
    ): ClockPickerInteractor {
        return clockPickerInteractor
            ?: ClockPickerInteractor(FakeClockPickerRepository()).also {
                clockPickerInteractor = it
            }
    }

    override fun getClockSectionViewModel(
        context: Context,
        clockRegistry: ClockRegistry
    ): ClockSectionViewModel {
        return clockSectionViewModel
            ?: ClockSectionViewModel(getClockPickerInteractor(context, clockRegistry)).also {
                clockSectionViewModel = it
            }
    }

    override fun getColorPickerInteractor(
        context: Context,
        wallpaperColorsViewModel: WallpaperColorsViewModel,
    ): ColorPickerInteractor {
        return colorPickerInteractor
            ?: ColorPickerInteractor(ColorPickerRepositoryImpl(context, wallpaperColorsViewModel))
                .also { colorPickerInteractor = it }
    }

    override fun getColorPickerViewModelFactory(
        context: Context,
        wallpaperColorsViewModel: WallpaperColorsViewModel,
    ): ColorPickerViewModel.Factory {
        return colorPickerViewModelFactory
            ?: ColorPickerViewModel.Factory(
                    context,
                    getColorPickerInteractor(context, wallpaperColorsViewModel),
                )
                .also { colorPickerViewModelFactory = it }
    }

    override fun getClockCarouselViewModel(
        context: Context,
        clockRegistry: ClockRegistry
    ): ClockCarouselViewModel {
        return clockCarouselViewModel
            ?: ClockCarouselViewModel(getClockPickerInteractor(context, clockRegistry)).also {
                clockCarouselViewModel = it
            }
    }

    override fun getClockViewFactory(
        activity: Activity,
        registry: ClockRegistry
    ): ClockViewFactory {
        return clockViewFactory
            ?: ClockViewFactory(activity, registry).also { clockViewFactory = it }
    }

    override fun getClockSettingsViewModelFactory(
        context: Context,
        registry: ClockRegistry
    ): ClockSettingsViewModel.Factory {
        return clockSettingsViewModelFactory
            ?: ClockSettingsViewModel.Factory(
                    context,
                    getClockPickerInteractor(context, registry),
                )
                .also { clockSettingsViewModelFactory = it }
    }

    companion object {
        private const val KEY_QUICK_AFFORDANCE_SNAPSHOT_RESTORER = 1
    }
}