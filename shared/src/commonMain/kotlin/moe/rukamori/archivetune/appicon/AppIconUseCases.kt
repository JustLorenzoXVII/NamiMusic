/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.appicon


class LoadAppIconsUseCase constructor(
        private val repository: AppIconRepository,
    ) {
        suspend operator fun invoke(): AppIconCatalog = repository.loadCatalog()
    }

class SelectAppIconUseCase constructor(
        private val repository: AppIconRepository,
    ) {
        suspend operator fun invoke(iconId: String): AppIconCatalog = repository.selectIcon(iconId)
    }

class RestoreLauncherEntryUseCase constructor(
        private val repository: AppIconRepository,
    ) {
        suspend operator fun invoke() = repository.restoreLauncherEntry()
    }
