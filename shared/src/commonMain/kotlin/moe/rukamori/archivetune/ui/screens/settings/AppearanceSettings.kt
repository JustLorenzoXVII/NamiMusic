/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.rukamori.archivetune.ui.screens.settings

import moe.rukamori.archivetune.compat.contentResolver

import archivetunex.shared.generated.resources.*

import moe.rukamori.archivetune.compat.Activity
import moe.rukamori.archivetune.compat.Context
import moe.rukamori.archivetune.compat.Intent
import moe.rukamori.archivetune.compat.startActivity
import moe.rukamori.archivetune.compat.Uri
import moe.rukamori.archivetune.compat.Build
import moe.rukamori.archivetune.compat.View
import moe.rukamori.archivetune.compat.Toast
import moe.rukamori.archivetune.compat.rememberLauncherForActivityResult
import moe.rukamori.archivetune.compat.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import moe.rukamori.archivetune.compat.LocalContext
import moe.rukamori.archivetune.compat.LocalView
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rukamori.archivetune.LocalPlayerAwareWindowInsets
import moe.rukamori.archivetune.constants.AppFontPreference
import moe.rukamori.archivetune.constants.BackdropBlurAmountKey
import moe.rukamori.archivetune.constants.BackdropEnabledKey
import moe.rukamori.archivetune.constants.BlurRadiusKey
import moe.rukamori.archivetune.constants.ChipSortTypeKey
import moe.rukamori.archivetune.constants.CropThumbnailToSquareKey
import moe.rukamori.archivetune.constants.CustomFontNameKey
import moe.rukamori.archivetune.constants.CustomFontUriKey
import moe.rukamori.archivetune.constants.DarkModeKey
import moe.rukamori.archivetune.constants.DefaultLibraryFilterOrder
import moe.rukamori.archivetune.constants.DefaultLibraryFilterOrderPreference
import moe.rukamori.archivetune.constants.DefaultOpenTabKey
import moe.rukamori.archivetune.constants.DisableAnimationsKey
import moe.rukamori.archivetune.constants.DisableBlurKey
import moe.rukamori.archivetune.constants.DynamicThemeKey
import moe.rukamori.archivetune.constants.FontPreferenceKey
import moe.rukamori.archivetune.constants.ForceHighRefreshRateKey
import moe.rukamori.archivetune.constants.GridItemSize
import moe.rukamori.archivetune.constants.GridItemsSizeKey
import moe.rukamori.archivetune.constants.HidePlayerThumbnailKey
import moe.rukamori.archivetune.constants.LibraryChipOrderKey
import moe.rukamori.archivetune.constants.LibraryFilter
import moe.rukamori.archivetune.constants.LyricsBackgroundStyle
import moe.rukamori.archivetune.constants.LyricsBackgroundStyleKey
import moe.rukamori.archivetune.constants.MiniPlayerBackgroundStyle
import moe.rukamori.archivetune.constants.MiniPlayerBackgroundStyleKey
import moe.rukamori.archivetune.constants.PlayerBackgroundStyle
import moe.rukamori.archivetune.constants.PlayerBackgroundStyleKey
import moe.rukamori.archivetune.constants.PlayerButtonsStyle
import moe.rukamori.archivetune.constants.PlayerButtonsStyleKey
import moe.rukamori.archivetune.constants.PlayerDesignStyle
import moe.rukamori.archivetune.constants.PlayerDesignStyleKey
import moe.rukamori.archivetune.constants.PlaylistTagOrderKey
import moe.rukamori.archivetune.constants.PureBlackKey
import moe.rukamori.archivetune.constants.QuickPicksDisplayMode
import moe.rukamori.archivetune.constants.QuickPicksDisplayModeKey
import moe.rukamori.archivetune.constants.RandomThemeOnStartupKey
import moe.rukamori.archivetune.constants.ShowHomeCategoryChipsKey
import moe.rukamori.archivetune.constants.ShowPlayerVolumeBarKey
import moe.rukamori.archivetune.constants.ShowTagsInLibraryKey
import moe.rukamori.archivetune.constants.SliderStyle
import moe.rukamori.archivetune.constants.SliderStyleKey
import moe.rukamori.archivetune.constants.SwipeSensitivityKey
import moe.rukamori.archivetune.constants.SwipeThumbnailKey
import moe.rukamori.archivetune.constants.SwipeToSongKey
import moe.rukamori.archivetune.constants.ThumbnailCornerRadiusKey
import moe.rukamori.archivetune.constants.WallpaperExtractionFailedKey
import moe.rukamori.archivetune.constants.toLibraryFilterOrder
import moe.rukamori.archivetune.constants.toLibraryFilterPreference
import moe.rukamori.archivetune.constants.toPlaylistTagOrder
import moe.rukamori.archivetune.constants.toPlaylistTagPreference
import moe.rukamori.archivetune.ui.component.DefaultDialog
import moe.rukamori.archivetune.ui.component.EnumListPreference
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.component.LibraryChipOrderDialog
import moe.rukamori.archivetune.ui.component.ListPreference
import moe.rukamori.archivetune.ui.component.PlaylistTagOrderDialog
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.component.SwitchPreference
import moe.rukamori.archivetune.ui.component.TagsManagementDialog
import moe.rukamori.archivetune.ui.component.ThumbnailCornerRadiusSelectorButton
import moe.rukamori.archivetune.ui.player.StyledPlaybackSlider
import moe.rukamori.archivetune.ui.theme.CustomFontLoader
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.isLowRamDevice
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.viewmodels.PlaylistTagUiModel
import moe.rukamori.archivetune.viewmodels.PlaylistTagsScreenState
import moe.rukamori.archivetune.viewmodels.PlaylistTagsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(navController: NavController) {
    val playlistTagsViewModel: PlaylistTagsViewModel = koinViewModel()
    val playlistTagsState by playlistTagsViewModel.screenState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultDisableAnimations = remember(context) { context.isLowRamDevice() }
    val (dynamicTheme, onDynamicThemeChange) =
        rememberPreference(
            DynamicThemeKey,
            defaultValue = true,
        )
    val (wallpaperExtractionFailed) =
        rememberPreference(
            WallpaperExtractionFailedKey,
            defaultValue = false,
        )
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) =
        rememberPreference(
            RandomThemeOnStartupKey,
            defaultValue = false,
        )
    val (darkMode, onDarkModeChange) =
        rememberEnumPreference(
            DarkModeKey,
            defaultValue = DarkMode.AUTO,
        )
    val (playerDesignStyle, onPlayerDesignStyleChange) =
        rememberEnumPreference(
            PlayerDesignStyleKey,
            defaultValue = PlayerDesignStyle.V4,
        )
    val (showPlayerVolumeBar, onShowPlayerVolumeBarChange) =
        rememberPreference(
            ShowPlayerVolumeBarKey,
            defaultValue = true,
        )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(
            HidePlayerThumbnailKey,
            defaultValue = false,
        )
    val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) =
        rememberPreference(
            key = ThumbnailCornerRadiusKey,
            defaultValue = 16f, // default dp
        )
    val (cropThumbnailToSquare, onCropThumbnailToSquareChange) =
        rememberPreference(
            CropThumbnailToSquareKey,
            defaultValue = false,
        )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    val (configuredLyricsBackground, onLyricsBackgroundChange) =
        rememberEnumPreference(
            LyricsBackgroundStyleKey,
            defaultValue = LyricsBackgroundStyle.DEFAULT,
        )
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) =
        rememberEnumPreference(
            MiniPlayerBackgroundStyleKey,
            defaultValue = MiniPlayerBackgroundStyle.THEME,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (disableBlur, onDisableBlurChange) = rememberPreference(DisableBlurKey, defaultValue = false)
    val (disableAnimations, onDisableAnimationsChange) =
        rememberPreference(
            DisableAnimationsKey,
            defaultValue = defaultDisableAnimations,
        )
    val (forceHighRefreshRate, onForceHighRefreshRateChange) =
        rememberPreference(
            ForceHighRefreshRateKey,
            defaultValue = false,
        )
    val (blurRadius, onBlurRadiusChange) = rememberPreference(BlurRadiusKey, defaultValue = 48f)
    val (backdropEnabled, onBackdropEnabledChange) = rememberPreference(BackdropEnabledKey, defaultValue = true)
    val (backdropBlurAmount, onBackdropBlurAmountChange) = rememberPreference(BackdropBlurAmountKey, defaultValue = 60)
    val (fontPreference, onFontPreferenceChange) =
        rememberEnumPreference(
            FontPreferenceKey,
            defaultValue = AppFontPreference.DEFAULT,
        )
    val (customFontUri, onCustomFontUriChange) = rememberPreference(CustomFontUriKey, defaultValue = "")
    val (customFontName, onCustomFontNameChange) = rememberPreference(CustomFontNameKey, defaultValue = "")
    val (defaultOpenTab, onDefaultOpenTabChange) =
        rememberEnumPreference(
            DefaultOpenTabKey,
            defaultValue = NavigationTab.HOME,
        )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(
            PlayerButtonsStyleKey,
            defaultValue = PlayerButtonsStyle.DEFAULT,
        )
    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(
            SliderStyleKey,
            defaultValue = SliderStyle.Standard,
        )
    val (swipeThumbnail, onSwipeThumbnailChange) =
        rememberPreference(
            SwipeThumbnailKey,
            defaultValue = true,
        )
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(
            SwipeSensitivityKey,
            defaultValue = 0.73f,
        )
    val (gridItemSize, onGridItemSizeChange) =
        rememberEnumPreference(
            GridItemsSizeKey,
            defaultValue = GridItemSize.SMALL,
        )

    val (swipeToSong, onSwipeToSongChange) =
        rememberPreference(
            SwipeToSongKey,
            defaultValue = false,
        )

    val (showTagsInLibrary, onShowTagsInLibraryChange) =
        rememberPreference(
            ShowTagsInLibraryKey,
            defaultValue = true,
        )
    val (showHomeCategoryChips, onShowHomeCategoryChipsChange) =
        rememberPreference(
            ShowHomeCategoryChipsKey,
            defaultValue = true,
        )
    val (libraryChipOrderPreference, onLibraryChipOrderChange) =
        rememberPreference(
            LibraryChipOrderKey,
            defaultValue = DefaultLibraryFilterOrderPreference,
        )
    val libraryChipOrder =
        remember(libraryChipOrderPreference) {
            libraryChipOrderPreference.toLibraryFilterOrder()
        }
    val (playlistTagOrderPreference, onPlaylistTagOrderChange) =
        rememberPreference(
            PlaylistTagOrderKey,
            defaultValue = "",
        )
    val availablePlaylistTags =
        (playlistTagsState as? PlaylistTagsScreenState.Success)?.tags.orEmpty()
    val playlistTagOrder =
        remember(availablePlaylistTags, playlistTagOrderPreference) {
            val tagsById = availablePlaylistTags.associateBy(PlaylistTagUiModel::id)
            playlistTagOrderPreference
                .toPlaylistTagOrder(availablePlaylistTags.map(PlaylistTagUiModel::id))
                .mapNotNull { tagId -> tagsById[tagId] }
        }
    val (quickPicksDisplayMode, onQuickPicksDisplayModeChange) =
        rememberEnumPreference(
            QuickPicksDisplayModeKey,
            defaultValue = QuickPicksDisplayMode.CARD,
        )

    val customFontPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            if (!CustomFontLoader.isSupportedTtf(context, uri)) {
                Toast.makeText(context, context.getString(Res.string.custom_font_invalid), Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            if (customFontUri.isNotBlank() && customFontUri != uri.toString()) {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(customFontUri),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }

            onCustomFontUriChange(uri.toString())
            onCustomFontNameChange(CustomFontLoader.displayName(context, uri))
            onFontPreferenceChange(AppFontPreference.CUSTOM)
        }
    val pickCustomFont =
        remember(customFontPickerLauncher) {
            {
                customFontPickerLauncher.launch(CustomFontLoader.supportedMimeTypes)
            }
        }
    val onFontPreferenceSelected =
        remember(customFontUri, onFontPreferenceChange, pickCustomFont) {
            { value: AppFontPreference ->
                onFontPreferenceChange(value)
                if (value == AppFontPreference.CUSTOM && customFontUri.isBlank()) {
                    pickCustomFont()
                }
            }
        }

    val availableBackgroundStyles =
        PlayerBackgroundStyle.entries.filter {
            it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }
    val availableLyricsBackgroundStyles =
        remember {
            listOf(
                LyricsBackgroundStyle.DEFAULT,
                LyricsBackgroundStyle.FOLLOW_THEME,
                LyricsBackgroundStyle.COLORING,
            )
        }
    val lyricsBackground = configuredLyricsBackground.resolveFor(playerBackground)
    val isPlayerStyleCustomizationEnabled =
        when (playerDesignStyle) {
            PlayerDesignStyle.V7,
            PlayerDesignStyle.V8,
            PlayerDesignStyle.V9,
            PlayerDesignStyle.V10,
            -> false

            else -> true
        }
    val isVolumeBarSupported =
        playerDesignStyle == PlayerDesignStyle.V7 ||
            playerDesignStyle == PlayerDesignStyle.V8
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    val (defaultChip, onDefaultChipChange) =
        rememberEnumPreference(
            key = ChipSortTypeKey,
            defaultValue = LibraryFilter.LIBRARY,
        )
    val supportedHighestFps = rememberSupportedHighestFps()
    val isHighRefreshRateSupported = supportedHighestFps > HIGH_REFRESH_RATE_THRESHOLD_FPS

    ApplyRefreshRate(
        isEnabled = forceHighRefreshRate && isHighRefreshRateSupported,
        targetFps = supportedHighestFps,
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showLibraryChipOrderDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showPlaylistTagOrderDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showTagsManagementDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(isPlayerStyleCustomizationEnabled, playerBackground) {
        if (!isPlayerStyleCustomizationEnabled && playerBackground != PlayerBackgroundStyle.DEFAULT) {
            onPlayerBackgroundChange(PlayerBackgroundStyle.DEFAULT)
        }
    }

    LaunchedEffect(isPlayerStyleCustomizationEnabled) {
        if (!isPlayerStyleCustomizationEnabled) {
            showSliderOptionDialog = false
        }
    }

    if (showSliderOptionDialog && isPlayerStyleCustomizationEnabled) {
        val sliderStyles =
            remember {
                listOf(
                    SliderStyle.Standard,
                    SliderStyle.Wavy,
                    SliderStyle.Thick,
                    SliderStyle.Circular,
                    SliderStyle.Simple,
                )
            }
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(Res.string.system_cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sliderStyles.chunked(3).forEach { styleRow ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        styleRow.forEach { style ->
                            SliderStyleOptionCard(
                                sliderStyle = style,
                                selected = sliderStyle == style,
                                onClick = {
                                    onSliderStyleChange(style)
                                    showSliderOptionDialog = false
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - styleRow.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showLibraryChipOrderDialog) {
        LibraryChipOrderDialog(
            initialOrder = libraryChipOrder,
            onDismiss = { showLibraryChipOrderDialog = false },
            onConfirm = { newOrder ->
                onLibraryChipOrderChange(newOrder.toLibraryFilterPreference())
                showLibraryChipOrderDialog = false
            },
        )
    }

    if (showPlaylistTagOrderDialog) {
        PlaylistTagOrderDialog(
            state = playlistTagsState,
            initialOrder = playlistTagOrder,
            onDismiss = { showPlaylistTagOrderDialog = false },
            onConfirm = { newOrder ->
                onPlaylistTagOrderChange(
                    newOrder.map(PlaylistTagUiModel::id).toPlaylistTagPreference(),
                )
                showPlaylistTagOrderDialog = false
            },
        )
    }

    if (showTagsManagementDialog) {
        TagsManagementDialog(
            onDismiss = { showTagsManagementDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.appearance)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(Res.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val topPadding = innerPadding.calculateTopPadding()

        Column(
            Modifier
                .padding(top = topPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(bottom = SettingsDimensions.ScreenBottomPadding),
        ) {
            PreferenceGroup(title = stringResource(Res.string.theme)) {
                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.enable_dynamic_theme)) },
                        icon = { Icon(painterResource(Res.drawable.palette), null) },
                        checked = dynamicTheme,
                        onCheckedChange = onDynamicThemeChange,
                    )
                }

                item(visible = dynamicTheme && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && wallpaperExtractionFailed) {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.wallpaper_permission)) },
                        description = stringResource(Res.string.wallpaper_permission_desc),
                        icon = { Icon(painterResource(Res.drawable.storage), null) },
                        onClick = {
                            val intent =
                                android.content.Intent(
                                    moe.rukamori.archivetune.compat.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null),
                                )
                            context.startActivity(intent)
                        },
                    )
                }

                item(visible = !dynamicTheme) {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.random_theme_on_startup)) },
                        description = stringResource(Res.string.random_theme_on_startup_desc),
                        icon = { Icon(painterResource(Res.drawable.shuffle), null) },
                        checked = randomThemeOnStartup,
                        onCheckedChange = onRandomThemeOnStartupChange,
                    )
                }

                item(visible = !dynamicTheme || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.color_palette)) },
                        description = stringResource(Res.string.customize_theme_colors),
                        icon = { Icon(painterResource(Res.drawable.format_paint), null) },
                        onClick = { navController.navigate("settings/appearance/palette_picker") },
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.app_icon)) },
                        description = stringResource(Res.string.app_icon_description),
                        icon = { Icon(painterResource(Res.drawable.app_icon_small), null) },
                        onClick = { navController.navigate("settings/appearance/icon") },
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.dark_theme)) },
                        icon = { Icon(painterResource(Res.drawable.dark_mode), null) },
                        selectedValue = darkMode,
                        onValueSelected = onDarkModeChange,
                        valueText = {
                            when (it) {
                                DarkMode.ON -> stringResource(Res.string.dark_theme_on)
                                DarkMode.OFF -> stringResource(Res.string.dark_theme_off)
                                DarkMode.AUTO -> stringResource(Res.string.dark_theme_follow_system)
                            }
                        },
                    )
                }

                item(visible = useDarkTheme) {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.pure_black)) },
                        icon = { Icon(painterResource(Res.drawable.contrast), null) },
                        checked = pureBlack,
                        onCheckedChange = onPureBlackChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.disable_blur)) },
                        description = stringResource(Res.string.disable_blur_desc),
                        icon = { Icon(painterResource(Res.drawable.blur_off), null) },
                        checked = disableBlur,
                        onCheckedChange = onDisableBlurChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.disable_animations)) },
                        description = stringResource(Res.string.disable_animations_desc),
                        icon = { Icon(painterResource(Res.drawable.animation), null) },
                        checked = disableAnimations,
                        onCheckedChange = onDisableAnimationsChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.force_high_refresh_rate)) },
                        description =
                            stringResource(
                                Res.string.max_supported_refresh_rate,
                                supportedHighestFps.roundToInt(),
                            ),
                        icon = { Icon(painterResource(Res.drawable.speed), null) },
                        checked = forceHighRefreshRate,
                        onCheckedChange = onForceHighRefreshRateChange,
                        isEnabled = isHighRefreshRateSupported,
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.blur_intensity)) },
                        description = stringResource(Res.string.blur_intensity_value, blurRadius.roundToInt()),
                        icon = { Icon(painterResource(Res.drawable.blur_on), null) },
                        isEnabled = !disableBlur,
                        content = {
                            Spacer(modifier = Modifier.height(10.dp))
                            Slider(
                                value = blurRadius,
                                onValueChange = onBlurRadiusChange,
                                valueRange = 0f..64f,
                                steps = 63,
                                enabled = !disableBlur,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.album_backdrop)) },
                        description = stringResource(Res.string.album_backdrop_desc),
                        icon = { Icon(painterResource(Res.drawable.blur_on), null) },
                        checked = backdropEnabled,
                        onCheckedChange = onBackdropEnabledChange,
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.backdrop_blur_amount)) },
                        description = stringResource(Res.string.backdrop_blur_amount_value, backdropBlurAmount),
                        icon = { Icon(painterResource(Res.drawable.blur_on), null) },
                        isEnabled = backdropEnabled,
                        content = {
                            Spacer(modifier = Modifier.height(10.dp))
                            Slider(
                                value = backdropBlurAmount.toFloat(),
                                onValueChange = { onBackdropBlurAmountChange(it.roundToInt()) },
                                valueRange = 0f..100f,
                                steps = 19,
                                enabled = backdropEnabled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.font_preference)) },
                        description = stringResource(Res.string.font_preference_desc),
                        icon = { Icon(painterResource(Res.drawable.text_fields), null) },
                        selectedValue = fontPreference,
                        onValueSelected = onFontPreferenceSelected,
                        valueText = {
                            when (it) {
                                AppFontPreference.DEFAULT -> stringResource(Res.string.font_preference_default)
                                AppFontPreference.SYSTEM -> stringResource(Res.string.font_preference_system)
                                AppFontPreference.CUSTOM -> stringResource(Res.string.font_preference_custom)
                            }
                        },
                    )
                }

                item(visible = fontPreference == AppFontPreference.CUSTOM) {
                    val customFontDescription =
                        if (customFontName.isNotBlank()) {
                            customFontName
                        } else if (customFontUri.isBlank()) {
                            stringResource(Res.string.custom_font_desc)
                        } else {
                            customFontUri
                        }
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.custom_font)) },
                        description = customFontDescription,
                        icon = { Icon(painterResource(Res.drawable.text_fields), null) },
                        onClick = pickCustomFont,
                    )
                }
            }

            PreferenceGroup(title = stringResource(Res.string.player)) {
                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.player_design_style)) },
                        icon = { Icon(painterResource(Res.drawable.palette), null) },
                        selectedValue = playerDesignStyle,
                        onValueSelected = onPlayerDesignStyleChange,
                        valueText = {
                            when (it) {
                                PlayerDesignStyle.V1 -> stringResource(Res.string.player_design_v1)
                                PlayerDesignStyle.V2 -> stringResource(Res.string.player_design_v2)
                                PlayerDesignStyle.V3 -> stringResource(Res.string.player_design_v3)
                                PlayerDesignStyle.V4 -> stringResource(Res.string.player_design_v4)
                                PlayerDesignStyle.V5 -> stringResource(Res.string.player_design_v5)
                                PlayerDesignStyle.V6 -> stringResource(Res.string.player_design_v6)
                                PlayerDesignStyle.V7 -> stringResource(Res.string.player_design_v7)
                                PlayerDesignStyle.V8 -> stringResource(Res.string.player_design_v8)
                                PlayerDesignStyle.V9 -> stringResource(Res.string.player_design_v9)
                                PlayerDesignStyle.V10 -> stringResource(Res.string.player_design_v10)
                            }
                        },
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.show_player_volume_bar)) },
                        description =
                            if (isVolumeBarSupported) {
                                null
                            } else {
                                stringResource(Res.string.player_volume_bar_v7_v8_only)
                            },
                        icon = { Icon(painterResource(Res.drawable.volume_up), null) },
                        checked = showPlayerVolumeBar,
                        onCheckedChange = onShowPlayerVolumeBarChange,
                        isEnabled = isVolumeBarSupported,
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.player_background_style)) },
                        description =
                            if (isPlayerStyleCustomizationEnabled) {
                                null
                            } else {
                                stringResource(Res.string.player_background_style_v8_v9_desc)
                            },
                        icon = { Icon(painterResource(Res.drawable.gradient), null) },
                        selectedValue = playerBackground,
                        onValueSelected = { selectedBackground ->
                            onPlayerBackgroundChange(selectedBackground)
                            when {
                                selectedBackground == PlayerBackgroundStyle.CUSTOM -> {
                                    onLyricsBackgroundChange(LyricsBackgroundStyle.CUSTOM)
                                }

                                configuredLyricsBackground == LyricsBackgroundStyle.CUSTOM -> {
                                    onLyricsBackgroundChange(LyricsBackgroundStyle.DEFAULT)
                                }
                            }
                        },
                        isEnabled = isPlayerStyleCustomizationEnabled,
                        valueText = {
                            when (it) {
                                PlayerBackgroundStyle.DEFAULT -> stringResource(Res.string.follow_theme)
                                PlayerBackgroundStyle.GRADIENT -> stringResource(Res.string.gradient)
                                PlayerBackgroundStyle.CUSTOM -> stringResource(Res.string.custom)
                                PlayerBackgroundStyle.BLUR -> stringResource(Res.string.player_background_blur)
                                PlayerBackgroundStyle.COLORING -> stringResource(Res.string.coloring)
                                PlayerBackgroundStyle.BLUR_GRADIENT -> stringResource(Res.string.blur_gradient)
                                PlayerBackgroundStyle.GLOW -> stringResource(Res.string.glow)
                                PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow Animated"
                            }
                        },
                    )
                }

                item {
                    ListPreference(
                        title = { Text(stringResource(Res.string.lyrics_background_style)) },
                        icon = { Icon(painterResource(Res.drawable.lyrics), null) },
                        selectedValue = lyricsBackground,
                        values = availableLyricsBackgroundStyles,
                        onValueSelected = onLyricsBackgroundChange,
                        isEnabled = playerBackground != PlayerBackgroundStyle.CUSTOM,
                        valueText = {
                            when (it) {
                                LyricsBackgroundStyle.DEFAULT -> stringResource(Res.string.lyrics_background_default)
                                LyricsBackgroundStyle.FOLLOW_THEME -> stringResource(Res.string.follow_theme)
                                LyricsBackgroundStyle.COLORING -> stringResource(Res.string.coloring)
                                LyricsBackgroundStyle.CUSTOM -> stringResource(Res.string.custom)
                            }
                        },
                    )
                }

                item(visible = playerBackground == PlayerBackgroundStyle.CUSTOM) {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.customized_background)) },
                        icon = { Icon(painterResource(Res.drawable.image), null) },
                        onClick = { navController.navigate("customize_background") },
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.mini_player_background_style)) },
                        icon = { Icon(painterResource(Res.drawable.gradient), null) },
                        selectedValue = miniPlayerBackground,
                        onValueSelected = onMiniPlayerBackgroundChange,
                        valueText = {
                            when (it) {
                                MiniPlayerBackgroundStyle.THEME -> stringResource(Res.string.follow_theme)
                                MiniPlayerBackgroundStyle.GRADIENT -> stringResource(Res.string.gradient)
                                MiniPlayerBackgroundStyle.GLOW -> stringResource(Res.string.glow)
                            }
                        },
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.hide_player_thumbnail)) },
                        description = stringResource(Res.string.hide_player_thumbnail_desc),
                        icon = { Icon(painterResource(Res.drawable.hide_image), null) },
                        checked = hidePlayerThumbnail,
                        onCheckedChange = onHidePlayerThumbnailChange,
                    )
                }

                item {
                    ThumbnailCornerRadiusSelectorButton(
                        onRadiusSelected = {},
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.crop_thumbnail_to_square)) },
                        description = stringResource(Res.string.crop_thumbnail_to_square_desc),
                        icon = { Icon(painterResource(Res.drawable.image), null) },
                        checked = cropThumbnailToSquare,
                        onCheckedChange = onCropThumbnailToSquareChange,
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.aod_customize_title)) },
                        description = stringResource(Res.string.aod_customize_entry_desc),
                        icon = { Icon(painterResource(Res.drawable.bedtime), null) },
                        onClick = { navController.navigate("settings/appearance/aod_customized") },
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.player_buttons_style)) },
                        description =
                            if (isPlayerStyleCustomizationEnabled) {
                                null
                            } else {
                                stringResource(Res.string.player_background_style_v8_v9_desc)
                            },
                        icon = { Icon(painterResource(Res.drawable.palette), null) },
                        selectedValue = playerButtonsStyle,
                        onValueSelected = onPlayerButtonsStyleChange,
                        isEnabled = isPlayerStyleCustomizationEnabled,
                        valueText = {
                            when (it) {
                                PlayerButtonsStyle.DEFAULT -> stringResource(Res.string.default_style)
                                PlayerButtonsStyle.SECONDARY -> stringResource(Res.string.secondary_color_style)
                            }
                        },
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.player_slider_style)) },
                        description = sliderStyleLabel(sliderStyle),
                        icon = { Icon(painterResource(Res.drawable.sliders), null) },
                        onClick = {
                            showSliderOptionDialog = true
                        },
                        isEnabled = isPlayerStyleCustomizationEnabled,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.enable_swipe_thumbnail)) },
                        icon = { Icon(painterResource(Res.drawable.swipe), null) },
                        checked = swipeThumbnail,
                        onCheckedChange = onSwipeThumbnailChange,
                    )
                }

                item(visible = swipeThumbnail) {
                    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

                    if (showSensitivityDialog) {
                        var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

                        DefaultDialog(
                            onDismiss = {
                                tempSensitivity = swipeSensitivity
                                showSensitivityDialog = false
                            },
                            buttons = {
                                TextButton(
                                    onClick = {
                                        tempSensitivity = 0.73f
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                ) {
                                    Text(stringResource(Res.string.reset))
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                TextButton(
                                    onClick = {
                                        tempSensitivity = swipeSensitivity
                                        showSensitivityDialog = false
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                ) {
                                    Text(stringResource(Res.string.system_cancel))
                                }
                                TextButton(
                                    onClick = {
                                        onSwipeSensitivityChange(tempSensitivity)
                                        showSensitivityDialog = false
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                ) {
                                    Text(stringResource(Res.string.system_ok))
                                }
                            },
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.swipe_sensitivity),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                )

                                Text(
                                    text = stringResource(Res.string.sensitivity_percentage, (tempSensitivity * 100).roundToInt()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                )

                                Slider(
                                    value = tempSensitivity,
                                    onValueChange = { tempSensitivity = it },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.swipe_sensitivity)) },
                        description = stringResource(Res.string.sensitivity_percentage, (swipeSensitivity * 100).roundToInt()),
                        icon = { Icon(painterResource(Res.drawable.tune), null) },
                        onClick = { showSensitivityDialog = true },
                    )
                }
            }

            PreferenceGroup(title = stringResource(Res.string.misc)) {
                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.quick_picks_display_mode)) },
                        icon = { Icon(painterResource(Res.drawable.grid_view), null) },
                        selectedValue = quickPicksDisplayMode,
                        onValueSelected = onQuickPicksDisplayModeChange,
                        valueText = {
                            when (it) {
                                QuickPicksDisplayMode.CARD -> stringResource(Res.string.quick_picks_display_mode_card)
                                QuickPicksDisplayMode.LIST -> stringResource(Res.string.quick_picks_display_mode_list)
                            }
                        },
                    )
                }

                item {
                    EnumListPreference(
                        title = { Text(stringResource(Res.string.default_open_tab)) },
                        icon = { Icon(painterResource(Res.drawable.nav_bar), null) },
                        selectedValue = defaultOpenTab,
                        onValueSelected = onDefaultOpenTabChange,
                        valueText = {
                            when (it) {
                                NavigationTab.HOME -> stringResource(Res.string.home)
                                NavigationTab.SEARCH -> stringResource(Res.string.search)
                                NavigationTab.LIBRARY -> stringResource(Res.string.filter_library)
                            }
                        },
                    )
                }

                item {
                    ListPreference(
                        title = { Text(stringResource(Res.string.default_lib_chips)) },
                        icon = { Icon(painterResource(Res.drawable.tab), null) },
                        selectedValue = defaultChip,
                        values = DefaultLibraryFilterOrder,
                        valueText = {
                            when (it) {
                                LibraryFilter.SONGS -> stringResource(Res.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(Res.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(Res.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(Res.string.playlists)
                                LibraryFilter.PODCASTS -> stringResource(Res.string.podcast)
                                LibraryFilter.SPOTIFY -> stringResource(Res.string.spotify_playlists)
                                LibraryFilter.LIBRARY -> stringResource(Res.string.filter_library)
                            }
                        },
                        onValueSelected = onDefaultChipChange,
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.arrange_library_chips)) },
                        description = stringResource(Res.string.arrange_library_chips_desc),
                        icon = { Icon(painterResource(Res.drawable.tab), null) },
                        onClick = { showLibraryChipOrderDialog = true },
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.arrange_playlist_tags)) },
                        description = stringResource(Res.string.arrange_playlist_tags_desc),
                        icon = { Icon(painterResource(Res.drawable.style), null) },
                        onClick = { showPlaylistTagOrderDialog = true },
                    )
                }

                item {
                    PreferenceEntry(
                        title = { Text(stringResource(Res.string.manage_playlist_tags)) },
                        description = stringResource(Res.string.manage_playlist_tags_desc),
                        icon = { Icon(painterResource(Res.drawable.style), null) },
                        onClick = { showTagsManagementDialog = true },
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.show_home_category_chips)) },
                        description = stringResource(Res.string.show_home_category_chips_desc),
                        icon = { Icon(painterResource(Res.drawable.home_outlined), null) },
                        checked = showHomeCategoryChips,
                        onCheckedChange = onShowHomeCategoryChipsChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.show_tags_in_library)) },
                        description = stringResource(Res.string.show_tags_in_library_desc),
                        icon = { Icon(painterResource(Res.drawable.filter_alt), null) },
                        checked = showTagsInLibrary,
                        onCheckedChange = onShowTagsInLibraryChange,
                    )
                }

                item {
                    SwitchPreference(
                        title = { Text(stringResource(Res.string.swipe_song_to_add)) },
                        icon = { Icon(painterResource(Res.drawable.swipe), null) },
                        checked = swipeToSong,
                        onCheckedChange = onSwipeToSongChange,
                    )
                }
            }
        }
    }
}

@Composable
fun ApplyRefreshRate(
    isEnabled: Boolean,
    targetFps: Float,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val requestedFps = if (isEnabled) targetFps else DEFAULT_REFRESH_RATE_REQUEST

    DisposableEffect(view, activity, requestedFps) {
        applyRefreshRate(
            view = view,
            activity = activity,
            requestedFps = requestedFps,
        )

        onDispose {
            applyRefreshRate(
                view = view,
                activity = activity,
                requestedFps = DEFAULT_REFRESH_RATE_REQUEST,
            )
        }
    }
}

@Composable
private fun rememberSupportedHighestFps(): Float {
    val view = LocalView.current

    return remember(view) {
        val display = view.display
        display
            ?.supportedModes
            ?.maxOfOrNull { mode -> mode.refreshRate }
            ?: display?.refreshRate
            ?: DEFAULT_STANDARD_REFRESH_RATE_FPS
    }
}

private fun applyRefreshRate(
    view: View,
    activity: Activity?,
    requestedFps: Float,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        view.setRequestedFrameRate(requestedFps)
        return
    }

    activity?.window?.let { window ->
        val attributes = window.attributes
        if (attributes.preferredRefreshRate != requestedFps) {
            attributes.preferredRefreshRate = requestedFps
            window.attributes = attributes
        }
    }
}

private fun Context.findActivity(): Activity? = this as? Activity

private const val HIGH_REFRESH_RATE_THRESHOLD_FPS = 60.5f
private const val DEFAULT_STANDARD_REFRESH_RATE_FPS = 60f
private const val DEFAULT_REFRESH_RATE_REQUEST = 0f

@Composable
private fun SliderStyleOptionCard(
    sliderStyle: SliderStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember {
        mutableFloatStateOf(0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp),
                ).clickable(onClick = onClick)
                .padding(16.dp),
    ) {
        StyledPlaybackSlider(
            sliderStyle = sliderStyle,
            value = sliderValue,
            valueRange = 0f..1f,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {},
            activeColor = MaterialTheme.colorScheme.primary,
            isPlaying = true,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        )

        Text(
            text = sliderStyleLabel(sliderStyle),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun sliderStyleLabel(sliderStyle: SliderStyle): String =
    when (sliderStyle) {
        SliderStyle.Standard -> stringResource(Res.string.slider_style_standard)
        SliderStyle.Wavy -> stringResource(Res.string.slider_style_wavy)
        SliderStyle.Thick -> stringResource(Res.string.slider_style_thick)
        SliderStyle.Circular -> stringResource(Res.string.slider_style_circular)
        SliderStyle.Simple -> stringResource(Res.string.slider_style_simple)
    }

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}
