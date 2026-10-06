# Porting guide: iPhone (SwiftUI) → Android (Kotlin + Jetpack Compose)

The Android app must look and behave **exactly** like the iPhone app in
`/home/claude/kazushiki-pilates/ios/KazushikiPilates` (read-only reference). Same screens, same
copy (every string word-for-word), same layout order, same colors, same data, same logic.
Only platform plumbing changes (StoreKit → Play Billing, UserDefaults → SharedPreferences,
UNUserNotificationCenter → AlarmManager + notifications, AVSpeechSynthesizer → TextToSpeech).

Nothing can be compiled locally (no Android SDK, no network). CI on GitHub builds it. So:
**write conservative, obviously-correct Kotlin**, use only the dependencies in
`app/build.gradle.kts`, double-check imports, and never reference an API you're not sure exists.

## Package layout (`app/src/main/java/com/kazushiki/pilates/`)

| Package | What |
|---|---|
| `figure` | `Pt`, `FigurePose`, `ArmPose`, `LegPose`, `FigureProp`, `FigureStage`, `ReformerGeometry`, `FigureSolver`, `FigureJoints` (DONE) · `FigureView`, `LoopingFigureView` (agent A) |
| `exercises` | `Equipment`, `BodyArea`, `ExerciseLevel`, `Exercise`, `ExerciseStep`, `ExerciseVariation`, `ExerciseCounter` (DONE) · `ExerciseLibrary` (agent A) |
| `player` | `ExercisePlayback`, `PlaybackFrame` (DONE) |
| `model` | `KPCalendar`, `Workout`, `WorkoutSlot`, `WorkoutTimeline`, `WorkoutSegment`, `ExerciseLibrary.resolve`, `WorkoutSession`, `SessionRating`, `ManualActivity`, `UserProfile`, `PilatesGoal`, `PlanSuggestion`, `Plan`, `PlanDay`, `PlanLibrary` (incl. all challenges), `QuickWorkoutBuilder`, `WeeklyStreak`, `Achievement*`, `Encouragement`, `CelebrationSummary` (DONE) · `WorkoutLibrary`, `ProgramCategory`, `ProgramFocus`, `ProgramLibrary` (agent B) |
| `data` | `KeyValueStore`, `SharedPrefsStore`, `MemoryStore`, `AppJson`, `ActivityStore`, `ProfileStore` (DONE) |
| `services` | `VoiceScript`, `VoicePrompt` (DONE) · `VoiceCoach`, `SubscriptionStore`, `ReminderScheduler`, `ReminderReceiver`, `BootReceiver` (agent E) |
| `ui` | `AppState.kt` (`AppTab`, `Route`, `AppRouter`, `Local*` composition locals), `MainTabs.kt`, `Icons.kt` (`sfIcon`) (DONE) |
| `ui.theme` | `KPColors`, `KP.colors`, `KP.cornerRadius` (18dp), `KP.compactCornerRadius` (12dp), `KP.cardRadius` (22dp), `KazushikiTheme`, `Modifier.kpCard(padding, radius)` (DONE) |
| `ui.components` | shared components (agent C) |
| `ui.screens` | screens (agents C, D, E) |

Read the DONE files before writing code that uses them; their APIs are the source of truth.

## Translation rules

- **Swift → Kotlin names.** Types keep their names. Enum cases become UPPER_SNAKE
  (`.coreStrength` → `PilatesGoal.CORE_STRENGTH`, `.fullBody` → `BodyArea.FULL_BODY`,
  `.upperBody` → `ProgramFocus.UPPER_BODY`). Static members of Swift enums used as namespaces
  become `object` members with the same camelCase names (`WorkoutLibrary.coreWakeUp`,
  `ExerciseLibrary.gluteBridge`). `allCases` → `entries`.
- **Object initialization order (critical).** Inside a Kotlin `object`, property initializers
  run top to bottom. A list like `val all = listOf(coreWakeUp, ...)` declared *before*
  `val coreWakeUp = ...` will contain nulls and crash. Declare aggregate lists **after** the
  items they list, or make them `val all: List<X> by lazy { ... }`. Same for maps of poses
  that reference other vals, and across files: an `object` split over several files (e.g. via
  extension properties) must not read another file's vals during its own init — prefer one
  `object` per file and `by lazy` for cross-file aggregates.
- **SF Symbols** stay as strings in models (`systemImage = "figure.pilates"`). In UI, draw them
  with `Icon(sfIcon(name), contentDescription = …)` from `com.kazushiki.pilates.ui.sfIcon`.
  If a symbol you need isn't mapped, add a case to `ui/Icons.kt` (only icons from
  `androidx.compose.material.icons.filled/outlined/automirrored` in material-icons-extended).
- **Colors**: only `KP.colors.*` (same tokens as `KPTheme` on iPhone). Gold = `KP.colors.prop`.
- **Fonts**: SwiftUI → Compose sizes: `.largeTitle` 34sp, `.title` 28sp, `.title2` 22sp,
  `.title3` 20sp, `.headline` 17sp SemiBold, `.body` 17sp, `.subheadline` 15sp, `.footnote` 13sp,
  `.caption` 12sp, `.caption2` 11sp. Weights: `.heavy` → `FontWeight.ExtraBold`,
  `.black` → `FontWeight.Black`, `.bold` → Bold, `.semibold` → SemiBold, `.medium` → Medium.
  `.system(size: 34, weight: .heavy)` → `fontSize = 34.sp, fontWeight = FontWeight.ExtraBold`.
  `.tracking(2)` → `letterSpacing = 2.sp`. Use `Text(…, style = TextStyle(...))` or params.
- **Layout**: `VStack(spacing: n)` → `Column(verticalArrangement = Arrangement.spacedBy(n.dp))`,
  `HStack` → `Row`, `ZStack` → `Box`, `ScrollView` → `Column(Modifier.verticalScroll(rememberScrollState()))`,
  `LazyVGrid` with fixed small item counts → rows of `Row`s (avoid nested lazy layouts inside
  scrolling columns), `.padding(16)` → `.padding(16.dp)`, `Spacer()` → `Spacer(Modifier.weight(1f))`
  in a Row/Column. `.frame(maxWidth: .infinity)` → `Modifier.fillMaxWidth()`.
- **Buttons**: `.borderedProminent` + `.controlSize(.large)` → `Button` with accent container,
  12dp rounded shape, ~52dp min height (use `KPPrimaryButton`). `.bordered` → `KPSecondaryButton`
  (accentSoft container, accent text). Plain-styled tappable cards → `Modifier.clickable`.
- **Navigation**: `NavigationLink { Destination }` → `LocalRouter.current.push(Route.X(...))`.
  Every pushed screen starts with `KPTopBar(title = …, onBack = { router.pop() })` (iPhone's
  back chevron + inline title). Tab roots that hide the nav bar on iPhone have no top bar.
  `router.openChallenges(route)` switches to the Challenges tab (Home tiles use it).
- **Sheets** (`.sheet`) → `KPBottomSheet(onDismiss) { … }` (full-height modal bottom sheet).
  **Full-screen covers** (`.fullScreenCover`: workout player, paywall) → `FullScreenDialog(onDismiss) { … }`.
  `.confirmationDialog` / alerts → Material3 `AlertDialog` with the same title, message and
  button labels (destructive button text in `Color(0xFFD32F2F)`).
- **State**: stores are Compose-state classes provided via `LocalProfileStore`,
  `LocalActivityStore`, `LocalSubscriptionStore`, `LocalRouter`. Read properties directly; they
  recompose automatically. Profile edits: `profileStore.update { it.copy(...) }`.
  `@State` → `remember { mutableStateOf(...) }` (use `rememberSaveable` for simple values).
- **Time**: `java.time.Instant` everywhere (`Date`), `KPCalendar` for calendar math
  (weekday numbers 1 = Sunday … 7 = Saturday, same as Swift). `TimelineView(.animation)` →
  `LaunchedEffect` loop with `withFrameNanos` updating a time state.
- **Haptics**: `.sensoryFeedback` → `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`
  (or `TextHandleMove` for light selection ticks).
- **Accessibility**: keep `accessibilityLabel`s as `Modifier.semantics { contentDescription = … }`.
- Reduce Motion: ignore (Android has no simple equivalent); always animate.

## Shared components (agent C writes them; everyone else calls them with exactly these signatures)

Package `com.kazushiki.pilates.ui.components`. All take `modifier: Modifier = Modifier` as the
first optional parameter after required ones (omitted below), and a trailing `onClick`/content lambda where shown.

```kotlin
// Structure
@Composable fun KPTopBar(title: String = "", onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {})
@Composable fun FullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit)
@Composable fun KPBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit)
@Composable fun KPPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, systemImage: String? = null, modifier: Modifier = Modifier)
@Composable fun KPSecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, systemImage: String? = null, modifier: Modifier = Modifier)

// KPStyle.swift
@Composable fun KPEyebrow(text: String)                       // Theme/KPTheme.swift KPEyebrow
@Composable fun KPPageHeader(eyebrow: String, title: String, subtitle: String? = null)
@Composable fun KPSectionLabel(text: String)
@Composable fun KPIconBadge(systemImage: String, size: Dp = 52.dp)
@Composable fun KPModeCard(systemImage: String, eyebrow: String, title: String, detail: String, action: String, onClick: () -> Unit)
@Composable fun KPActionTile(systemImage: String, title: String, subtitle: String, onClick: () -> Unit)
@Composable fun KPTag(text: String)
@Composable fun KPChip(title: String, isSelected: Boolean, systemImage: String? = null, isLocked: Boolean = false, onClick: () -> Unit)
@Composable fun KPSettingCard(title: String, footnote: String? = null, content: @Composable ColumnScope.() -> Unit)

// Components.swift
@Composable fun KPCollapsible(title: String, subtitle: String? = null, count: Int? = null, dimmed: Boolean = false, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit)
@Composable fun ProgressRing(done: Int, total: Int)
@Composable fun ProgramRow(plan: Plan, needs: Equipment? = null, onClick: () -> Unit)
@Composable fun WorkoutTile(workout: Workout, onClick: () -> Unit)
@Composable fun ProgramTile(plan: Plan, onClick: () -> Unit)
@Composable fun ExerciseRow(exercise: Exercise, onClick: () -> Unit)
@Composable fun SectionHeader(title: String, onSeeAll: () -> Unit)
@Composable fun WeekdayPicker(selection: Set<Int>, onSelectionChange: (Set<Int>) -> Unit)
@Composable fun ChallengeCard(plan: Plan, isAvailable: Boolean = true, onClick: () -> Unit)

// MainTabView.swift + WorkoutPreviewView.swift
enum class WorkoutRowStatus { OPEN, NEXT, DONE, LOCKED }
@Composable fun WorkoutRow(workout: Workout, eyebrow: String? = null, status: WorkoutRowStatus = WorkoutRowStatus.OPEN, onClick: (() -> Unit)? = null)
@Composable fun StatTile(value: String, label: String)
@Composable fun Pill(text: String)

// Celebration.swift (the data class CelebrationSummary is already in model/Achievements.kt)
@Composable fun CelebrationContent(summary: CelebrationSummary)
@Composable fun WeeklyGoalCard(weekly: WeeklyStreak)
@Composable fun WeekStreakLabel(weeks: Int)
@Composable fun AchievementBadge(achievement: Achievement, isEarned: Boolean, size: Dp = 56.dp)
```

Figure (agent A, package `com.kazushiki.pilates.figure`):
```kotlin
@Composable fun FigureView(pose: FigurePose, props: List<FigureProp> = emptyList(), modifier: Modifier = Modifier)   // keeps 440:200 aspect ratio
@Composable fun LoopingFigureView(exercise: Exercise = ExerciseLibrary.gluteBridge, variationIndex: Int = 0, modifier: Modifier = Modifier)
fun previewPose(exercise: Exercise): FigurePose   // ExerciseRow.previewPose on iPhone (still thumbnail pose)
```

Data (agent A, `exercises/ExerciseLibrary.kt`): `object ExerciseLibrary { val all: List<Exercise>; fun exercise(id: String): Exercise?; val gluteBridge; val ringBridge; … }`
Data (agent B): `object WorkoutLibrary { val all; val reformerWorkouts; fun workout(id: String): Workout?; fun workouts(upTo: ExerciseLevel, equipment: Set<Equipment> = setOf(Equipment.MAT)): List<Workout>; val coreWakeUp; … }`,
`enum class ProgramCategory(val title: String) { MAT, MIXED, BAND, RING, BALL, WEIGHTS, WALL, REFORMER; val equipment: Equipment? }`,
`enum class ProgramFocus { UPPER_BODY, CORE, LOWER_BODY, GLUTES, BACK, FULL_BODY; val rawValue: String /* "upperBody" … */; val title; val subtitle; val systemImage; companion object { fun suggested(goals: Set<PilatesGoal>): List<ProgramFocus> } }`,
`object ProgramLibrary { val all: List<Plan>; val focusPrograms: List<Plan>; fun programs(category: ProgramCategory): List<Plan>; fun programs(focus: ProgramFocus): List<Plan>; fun recommended(profile: UserProfile): List<Plan> }`.

Services (agent E, `services`):
```kotlin
class SubscriptionStore(context: Context, store: KeyValueStore) {
  val isPremium: Boolean; val isEligibleForTrial: Boolean; val hasSeenPaywall: Boolean
  val isLoading: Boolean; val loadFailed: Boolean; var testUnlocked: Boolean (setter persists)
  val monthly: SubscriptionProduct?; val annual: SubscriptionProduct?
  fun markPaywallSeen(); fun canAccess(workout: Workout): Boolean
  suspend fun start(activity: Activity); suspend fun loadProducts()
  suspend fun purchase(activity: Activity, product: SubscriptionProduct): PurchaseOutcome
  suspend fun restore()
  companion object { val testingToolsEnabled: Boolean /* BuildConfig.DEBUG */ }
}
object ReminderScheduler { fun createChannel(context: Context); fun apply(context: Context, profile: UserProfile) }
class VoiceCoach(context: Context) { fun speak(text: String); fun stop(); fun shutdown() }
```

Screens (package `com.kazushiki.pilates.ui.screens`, all `@Composable`, no params unless shown):
`HomeScreen()`, `ChallengesScreen()`, `ChallengeBrowseScreen()`, `PlanDetailScreen(plan: Plan)`,
`QuickWorkoutBuilderScreen()` (agent C) · `OnboardingScreen()`, `ProfileScreen()`, `ProgressScreen()`,
`LogWorkoutSheet(onDismiss: () -> Unit)`, `AchievementsSheet(earned: Set<String>, onDismiss: () -> Unit)`,
`ExerciseListScreen()`, `AllWorkoutsScreen()`, `ExerciseDetailScreen(exercise: Exercise)`,
`WorkoutPreviewScreen(workout: Workout, planID: String?, dayNumber: Int?)`,
`WorkoutPlayerScreen(workout: Workout, timeline: WorkoutTimeline, planID: String?, dayNumber: Int?, onClose: () -> Unit)` (agent D) ·
`PaywallScreen(onClose: () -> Unit)` (agent E).

`OptionCard` (onboarding row) lives in agent D's onboarding file. `MonthCalendar` lives in agent D's progress file.

## Tests

JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`) in `app/src/test/java/com/kazushiki/pilates/`.
Port the Swift Testing suites in `ios/KazushikiPilatesTests/` one-to-one (same test names in
camelCase, same expectations). Use `MemoryStore()` for stores and `KPCalendar(zone = ZoneId.of("America/Chicago"), firstWeekday = 1, locale = Locale.US)`
where the Swift test builds a Gregorian calendar.
