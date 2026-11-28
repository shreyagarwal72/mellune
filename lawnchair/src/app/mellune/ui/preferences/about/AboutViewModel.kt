package app.mellune.ui.preferences.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.mellune.preferences.PreferenceManager
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.firstCached
import com.android.launcher3.BuildConfig
import com.android.launcher3.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.create

class AboutViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val api: GitHubService = gitHubApiRetrofit.create()
    private val prefs: PreferenceManager = PreferenceManager.getInstance(application)
    private val prefs2: PreferenceManager2 = PreferenceManager2.getInstance(application)

    private val nightlyBuildsRepository = NightlyBuildsRepository(
        applicationContext = application,
        api = api,
    )

    val uiState: StateFlow<AboutUiState>
        field = MutableStateFlow(AboutUiState())

    val updateState = nightlyBuildsRepository.updateState

    init {
        uiState.update {
            it.copy(
                versionName = if (prefs.hideVersionInfo.get()) {
                    prefs.pseudonymVersion.get() + " (pseudonym)"
                } else {
                    BuildConfig.VERSION_NAME
                },
                commitHash = BuildConfig.COMMIT_HASH,
                maintainers = forkMaintainers,
                highlights = featureHighlights,
                coreTeam = team,
                supportAndPr = supportAndPr,
                topLinks = topLinks,
                bottomLinks = bottomLinks,
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            val activeContributors = fetchActiveContributors()
            val updatedMaintainers = uiState.value.maintainers.map { member ->
                val isActive = member.githubUsername != null &&
                    activeContributors.contains(member.githubUsername.lowercase())
                member.copy(status = if (isActive) ContributorStatus.Active else ContributorStatus.Idle)
            }
            val updatedCoreTeam = uiState.value.coreTeam.map { member ->
                val status = if (member.githubUsername != null && activeContributors.contains(member.githubUsername.lowercase())) ContributorStatus.Active else ContributorStatus.Idle
                member.copy(status = status)
            }
            uiState.update {
                it.copy(
                    maintainers = updatedMaintainers,
                    coreTeam = updatedCoreTeam,
                )
            }
        }

        // Check if the build variant is Nightly
        // AND check if user has enabled auto updater (available to Nightly variant)
        // OR check if user has overridden it in debug flags (available to All variant)
        if (BuildConfig.APPLICATION_ID.contains("nightly") && prefs2.autoUpdaterNightly.firstCached()) {
            nightlyBuildsRepository.checkForUpdate()
            viewModelScope.launch {
                nightlyBuildsRepository.updateState.collect { state ->
                    uiState.update { it.copy(updateState = state) }
                }
            }
        }
    }

    fun downloadUpdate() {
        nightlyBuildsRepository.downloadUpdate()
    }

    fun installUpdate(file: File, forceInstall: Boolean = false) {
        nightlyBuildsRepository.installUpdate(file, forceInstall)
    }

    fun resetToDownloaded(file: File) {
        nightlyBuildsRepository.resetToDownloaded(file)
    }

    private suspend fun fetchActiveContributors(): Set<String> {
        return runCatching {
            nightlyBuildsRepository.api.getRepositoryEvents("shreyagarwal72", "mellune")
                .map { it.actor.login.lowercase() }
                .toSet()
        }.getOrDefault(emptySet())
    }

    companion object {
        private val forkMaintainers = listOf(
            TeamMember(
                name = "Shrey Agarwal",
                role = Role.Development,
                photoUrl = "https://avatars.githubusercontent.com/shreyagarwal72",
                socialUrl = "https://github.com/shreyagarwal72",
                githubUsername = "shreyagarwal72",
            ),
        )

        private val featureHighlights = listOf(
            FeatureHighlight(
                iconResId = R.drawable.ic_about,
                titleResId = R.string.about_highlight_m3e_title,
                descriptionResId = R.string.about_highlight_m3e_desc,
            ),
            FeatureHighlight(
                iconResId = R.drawable.ic_gestures,
                titleResId = R.string.about_highlight_spring_title,
                descriptionResId = R.string.about_highlight_spring_desc,
            ),
            FeatureHighlight(
                iconResId = R.drawable.ic_apps,
                titleResId = R.string.about_highlight_search_title,
                descriptionResId = R.string.about_highlight_search_desc,
            ),
            FeatureHighlight(
                iconResId = R.drawable.ic_home_screen,
                titleResId = R.string.about_highlight_workspace_title,
                descriptionResId = R.string.about_highlight_workspace_desc,
            ),
            FeatureHighlight(
                iconResId = R.drawable.ic_quickstep,
                titleResId = R.string.about_highlight_widgets_title,
                descriptionResId = R.string.about_highlight_widgets_desc,
            ),
        )

        private val team = emptyList<TeamMember>()

        private val topLinks = listOf(
            Link(
                iconResId = R.drawable.ic_new_releases,
                labelResId = R.string.about_releases,
                url = "https://github.com/shreyagarwal72/mellune/releases",
            ),
            Link(
                iconResId = R.drawable.ic_github,
                labelResId = R.string.github,
                url = "https://github.com/shreyagarwal72/mellune",
            ),
            Link(
                iconResId = R.drawable.ic_bug_notification,
                labelResId = R.string.about_issues,
                url = "https://github.com/shreyagarwal72/mellune/issues",
            ),
        )

        private val bottomLinks = emptyList<Link>()

        private val supportAndPr = emptyList<TeamMember>()
    }
}
