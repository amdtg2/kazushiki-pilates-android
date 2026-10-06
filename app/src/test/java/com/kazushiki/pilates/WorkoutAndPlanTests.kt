package com.kazushiki.pilates

import com.kazushiki.pilates.data.ActivityStore
import com.kazushiki.pilates.data.AppJson
import com.kazushiki.pilates.data.MemoryStore
import com.kazushiki.pilates.exercises.BodyArea
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.ExerciseLevel
import com.kazushiki.pilates.exercises.ExerciseLibrary
import com.kazushiki.pilates.model.KPCalendar
import com.kazushiki.pilates.model.ManualActivity
import com.kazushiki.pilates.model.PilatesGoal
import com.kazushiki.pilates.model.PlanLibrary
import com.kazushiki.pilates.model.ProgramCategory
import com.kazushiki.pilates.model.ProgramFocus
import com.kazushiki.pilates.model.ProgramLibrary
import com.kazushiki.pilates.model.QuickWorkoutBuilder
import com.kazushiki.pilates.model.SessionRating
import com.kazushiki.pilates.model.UserProfile
import com.kazushiki.pilates.model.WorkoutLibrary
import com.kazushiki.pilates.model.WorkoutSegment
import com.kazushiki.pilates.model.WorkoutSession
import com.kazushiki.pilates.model.WorkoutSlot
import com.kazushiki.pilates.model.WorkoutTimeline
import com.kazushiki.pilates.model.resolve
import com.kazushiki.pilates.ui.screens.loggedSession
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class WorkoutAndPlanTests {
    private fun freshStore(): ActivityStore = ActivityStore(MemoryStore())

    private val testCalendar = KPCalendar(zone = ZoneId.of("America/Chicago"), firstWeekday = 1, locale = Locale.US)

    /** The store saves dates as epoch milliseconds, so use millisecond precision for round trips. */
    private fun nowMillis(): Instant = Instant.ofEpochMilli(System.currentTimeMillis())

    @Test
    fun everyWorkoutSlotResolvesToAnExercise() {
        for (workout in WorkoutLibrary.all) {
            for (slot in workout.slots) {
                assertNotNull("${workout.title} has an unknown exercise", ExerciseLibrary.resolve(slot, setOf(Equipment.MAT)))
            }
        }
    }

    @Test
    fun timelinePutsRestBetweenExercisesOnly() {
        for (workout in WorkoutLibrary.all) {
            val timeline = WorkoutTimeline(workout, setOf(Equipment.MAT))
            val kinds = timeline.segments.map { it.kind }
            assertEquals(workout.slots.size, timeline.exercises.size)
            assertEquals(workout.slots.size * 2 - 1, kinds.size)
            kinds.forEachIndexed { offset, kind ->
                val expected = if (offset % 2 == 0) WorkoutSegment.Kind.EXERCISE else WorkoutSegment.Kind.REST
                assertEquals("${workout.title} segment $offset", expected, kind)
            }

            // Segments are back to back with no gaps.
            for ((a, b) in timeline.segments.zipWithNext()) {
                assertTrue(abs(a.end - b.start) < 0.0001)
            }

            val work = timeline.exercises.sumOf { it.exercise.loopDuration * it.loops }
            val rest = workout.restSeconds * (workout.slots.size - 1)
            assertTrue(abs(timeline.totalDuration - (work + rest)) < 0.0001)
        }
    }

    @Test
    fun equipmentPicksTheMatchingVersion() {
        val slot = WorkoutSlot(exerciseID = "ring-bridge", variationID = "standard", loops = 10, alternatives = listOf("ball-bridge", "glute-bridge"))
        assertEquals("glute-bridge", ExerciseLibrary.resolve(slot, setOf(Equipment.MAT))?.first?.id)
        assertEquals("ball-bridge", ExerciseLibrary.resolve(slot, setOf(Equipment.MAT, Equipment.BALL))?.first?.id)
        assertEquals("ring-bridge", ExerciseLibrary.resolve(slot, setOf(Equipment.MAT, Equipment.BALL, Equipment.RING))?.first?.id)
    }

    @Test
    fun workoutsFillMoreTimeNow() {
        for (workout in WorkoutLibrary.all) {
            val minutes = WorkoutTimeline(workout, Equipment.entries.toSet()).estimatedMinutes
            assertTrue("${workout.title} is only $minutes min", minutes >= 3)
        }
    }

    @Test
    fun timelineFindsSegmentsAndEnds() {
        val timeline = WorkoutTimeline(WorkoutLibrary.coreWakeUp, setOf(Equipment.MAT))
        assertEquals(0, timeline.segmentIndex(0.0))
        assertEquals(1, timeline.segmentIndex(timeline.segments[1].start + 1))
        assertNull(timeline.segmentIndex(timeline.totalDuration))
    }

    @Test
    fun missingEquipmentFallsBackToAlternative() {
        val slot = WorkoutSlot(exerciseID = "does-not-exist", variationID = "standard", loops = 5, alternatives = listOf("glute-bridge"))
        // Unknown first choice is dropped; the alternative is used.
        val resolved = ExerciseLibrary.resolve(slot, setOf(Equipment.MAT))
        assertEquals("glute-bridge", resolved?.first?.id)
    }

    @Test
    fun personalPlanMatchesProfile() {
        val profile = UserProfile(goals = setOf(PilatesGoal.CORE_STRENGTH), level = ExerciseLevel.INTERMEDIATE)
        val plan = PlanLibrary.personalPlan(profile)
        assertEquals(4 * 4, plan.days.size)
        assertEquals(4, plan.weeks)
        assertEquals("Core Foundations", plan.title)
        for (day in plan.days) {
            val workout = day.workout
            assertNotNull(workout)
            assertTrue(workout!!.level.rank <= ExerciseLevel.INTERMEDIATE.rank)
        }
    }

    @Test
    fun beginnersOnlyGetBeginnerWorkouts() {
        val profile = UserProfile(goals = setOf(PilatesGoal.TONE))
        val plan = PlanLibrary.personalPlan(profile)
        assertTrue(plan.days.all { it.workout?.level == ExerciseLevel.BEGINNER })
    }

    @Test
    fun planProgressAndChallengeUnlocking() {
        val store = freshStore()
        val challenge = PlanLibrary.sevenDayCore
        assertEquals(1, store.nextDay(challenge)?.number)
        assertFalse(store.isUnlocked(challenge.days[1], challenge))

        store.log(WorkoutSession(date = Instant.now(), workoutID = challenge.days[0].workoutID, workoutTitle = "", planID = challenge.id, dayNumber = 1, seconds = 120.0))
        assertEquals(2, store.nextDay(challenge)?.number)
        assertTrue(store.isUnlocked(challenge.days[1], challenge))
        assertFalse(store.isUnlocked(challenge.days[2], challenge))
    }

    @Test
    fun sessionsArePersisted() {
        val defaults = MemoryStore()
        val store = ActivityStore(defaults)
        store.log(WorkoutSession(date = nowMillis(), workoutID = "core-wake-up", workoutTitle = "Core Wake-Up", seconds = 200.0, rating = SessionRating.JUST_RIGHT))
        val reloaded = ActivityStore(defaults)
        assertEquals(store.sessions, reloaded.sessions)
    }

    @Test
    fun streakCountsConsecutiveDays() {
        val calendar = testCalendar
        val now = calendar.date(2026, 9, 30, 9)
        fun daysAgo(n: Int): Instant = calendar.addDays(now, -n.toLong())

        assertEquals(0, ActivityStore.streak(emptyList(), now, calendar))
        assertEquals(3, ActivityStore.streak(listOf(daysAgo(0), daysAgo(1), daysAgo(2)), now, calendar))
        // No workout yet today: yesterday's streak still counts.
        assertEquals(2, ActivityStore.streak(listOf(daysAgo(1), daysAgo(2)), now, calendar))
        // A gap breaks the streak.
        assertEquals(1, ActivityStore.streak(listOf(daysAgo(0), daysAgo(2)), now, calendar))
        // Two sessions on one day count once.
        assertEquals(1, ActivityStore.streak(listOf(daysAgo(0), daysAgo(0)), now, calendar))
    }

    @Test
    fun reformerWorkoutsOnlyForReformerOwners() {
        var profile = UserProfile(goals = setOf(PilatesGoal.CORE_STRENGTH))
        val matOnly = PlanLibrary.personalPlan(profile)
        assertFalse(matOnly.days.any { it.workout?.requires == Equipment.REFORMER })
        assertFalse(WorkoutLibrary.workouts(ExerciseLevel.ADVANCED).any { it.requires == Equipment.REFORMER })

        profile = profile.copy(equipment = setOf(Equipment.MAT, Equipment.REFORMER))
        val withReformer = PlanLibrary.personalPlan(profile)
        assertTrue(withReformer.days.any { it.workout?.requires == Equipment.REFORMER })
        assertEquals("core-wake-up", withReformer.days.firstOrNull()?.workoutID)
    }

    @Test
    fun everyEquipmentHasSeveralPrograms() {
        for (category in ProgramCategory.entries) {
            assertTrue("${category.title} needs more programs", ProgramLibrary.programs(category).size >= 2)
        }
        for (item in Equipment.entries) {
            if (item == Equipment.MAT) continue
            val count = ExerciseLibrary.all.count { item in it.equipment }
            assertTrue("Only $count ${item.title} exercises", count >= 3)
        }
    }

    @Test
    fun programsUseRealWorkoutsAndUniqueIDs() {
        val ids = (ProgramLibrary.all + ProgramLibrary.focusPrograms).map { it.id } +
            PlanLibrary.challenges.map { it.id } + listOf(PlanLibrary.personalPlanID)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(WorkoutLibrary.all.size, WorkoutLibrary.all.map { it.id }.toSet().size)
        assertEquals(ExerciseLibrary.all.size, ExerciseLibrary.all.map { it.id }.toSet().size)
        for (program in ProgramLibrary.all + ProgramLibrary.focusPrograms) {
            assertTrue("${program.title} has a missing workout", program.days.all { it.workout != null })
            assertNotNull(program.level)
        }
    }

    @Test
    fun matOnlyPlansNeverNeedEquipment() {
        for (level in ExerciseLevel.entries) {
            val profile = UserProfile(goals = setOf(PilatesGoal.TONE), level = level)
            for (day in PlanLibrary.personalPlan(profile).days) {
                val workout = day.workout!!
                val timeline = WorkoutTimeline(workout, setOf(Equipment.MAT))
                assertTrue("${workout.title} needs equipment", timeline.exercises.all { it.exercise.equipment == listOf(Equipment.MAT) })
            }
        }
    }

    @Test
    fun advancedWorkoutsAreBrowsableAtEveryLevel() {
        assertTrue(WorkoutLibrary.all.any { it.level == ExerciseLevel.ADVANCED })
        assertTrue(ProgramLibrary.all.any { it.level == ExerciseLevel.ADVANCED })
    }

    @Test
    fun everyBodyAreaHasProgramsThatWorkWithJustAMat() {
        for (focus in ProgramFocus.entries) {
            val programs = ProgramLibrary.programs(focus)
            assertTrue("${focus.title} needs more programs", programs.size >= 2)
            for (program in programs) {
                for (day in program.days) {
                    val timeline = WorkoutTimeline(day.workout!!, setOf(Equipment.MAT))
                    assertTrue(
                        "${program.title} needs equipment on day ${day.number}",
                        timeline.exercises.all { it.exercise.equipment == listOf(Equipment.MAT) },
                    )
                }
            }
        }
    }

    @Test
    fun goalsSuggestMatchingBodyAreas() {
        assertEquals(listOf(ProgramFocus.CORE), ProgramFocus.suggested(setOf(PilatesGoal.CORE_STRENGTH)))
        assertEquals(listOf(ProgramFocus.BACK), ProgramFocus.suggested(setOf(PilatesGoal.POSTURE, PilatesGoal.RECOVERY)))
    }

    @Test
    fun loggedWorkoutsKeepTheStreakButNotThePlan() {
        val store = freshStore()
        val calendar = testCalendar
        val now = calendar.date(2026, 9, 30, 20)
        val yesterday = calendar.addDays(now, -1)

        val logged = loggedSession(
            activity = ManualActivity.YOGA, minutes = 45, areas = setOf(BodyArea.CORE, BodyArea.BACK), rating = SessionRating.JUST_RIGHT,
            date = yesterday, now = now, calendar = calendar,
        )
        assertEquals(true, logged.isManual)
        assertEquals(45, logged.minutes)
        assertEquals(listOf(BodyArea.CORE, BodyArea.BACK), logged.bodyAreas)
        assertEquals(12, calendar.hour(logged.date))

        store.log(logged)
        store.log(loggedSession(activity = ManualActivity.PILATES, minutes = 20, areas = setOf(BodyArea.GLUTES), rating = null, date = now, now = now, calendar = calendar))
        assertEquals(2, ActivityStore.streak(store.sessions.map { it.date }, now, calendar))

        val profile = UserProfile(goals = setOf(PilatesGoal.TONE))
        assertEquals(1, store.nextDay(PlanLibrary.personalPlan(profile))?.number)

        store.delete(logged)
        assertEquals(1, store.sessions.size)
    }

    @Test
    fun sessionsSavedBeforeManualLoggingStillLoad() {
        // Android saves dates as epoch milliseconds; this is a session saved before the manual-logging fields existed.
        val old = """[{"id":"5D0C9E4A-6B2B-4A57-9E37-1E9A0B1C2D3E","date":1758000000000,"workoutID":"core-wake-up","workoutTitle":"Core Wake-Up","seconds":300}]"""
        val sessions = AppJson.decodeFromString(ListSerializer(WorkoutSession.serializer()), old)
        assertNull(sessions.firstOrNull()?.isManual)
        assertEquals(5, sessions.firstOrNull()?.minutes)
    }

    @Test
    fun joiningAndLeavingPrograms() {
        val store = freshStore()
        val profile = UserProfile()
        assertTrue(store.activePrograms(profile).isEmpty())

        val first = ProgramLibrary.all[0]
        val second = ProgramLibrary.focusPrograms[0]
        store.enroll(first.id)
        store.enroll(second.id)
        assertEquals(listOf(second.id, first.id), store.activePrograms(profile).map { it.id })

        // Doing a workout from a program brings it to the top.
        store.log(WorkoutSession(date = Instant.now(), workoutID = first.days[0].workoutID, workoutTitle = "", planID = first.id, dayNumber = 1, seconds = 300.0))
        assertEquals(first.id, store.activePrograms(profile).firstOrNull()?.id)

        // Leaving keeps her history.
        store.leave(first.id)
        assertFalse(store.isEnrolled(first.id))
        assertEquals(setOf(1), store.completedDays(first.id))
        assertEquals(listOf(second.id), store.activePrograms(profile).map { it.id })
    }

    @Test
    fun finishedProgramsLeaveHomeButAreRemembered() {
        val store = freshStore()
        val profile = UserProfile()
        val challenge = PlanLibrary.sevenDayCore
        for (day in challenge.days) {
            store.log(WorkoutSession(date = Instant.now(), workoutID = day.workoutID, workoutTitle = "", planID = challenge.id, dayNumber = day.number, seconds = 120.0))
        }
        assertTrue(store.activePrograms(profile).isEmpty())
        assertEquals(challenge.id, store.lastFinishedProgram(profile)?.id)
    }

    @Test
    fun enrollmentIsSavedAndSeededFromHistory() {
        val defaults = MemoryStore()
        val program = ProgramLibrary.all[1]
        val old = listOf(
            WorkoutSession(date = Instant.now(), workoutID = program.days[0].workoutID, workoutTitle = "", planID = program.id, dayNumber = 1, seconds = 300.0),
            WorkoutSession(date = Instant.now(), workoutID = "core-wake-up", workoutTitle = "", planID = PlanLibrary.personalPlanID, dayNumber = 1, seconds = 300.0),
        )
        defaults.putString(ActivityStore.KEY, AppJson.encodeToString(ListSerializer(WorkoutSession.serializer()), old))

        // Older installs: programs she started count as joined; the personal plan doesn't.
        val store = ActivityStore(defaults)
        assertEquals(listOf(program.id), store.enrolledPlanIDs)

        store.leave(program.id)
        assertTrue(ActivityStore(defaults).enrolledPlanIDs.isEmpty())
    }

    @Test
    fun everyPlanCanBeFoundByID() {
        val profile = UserProfile()
        for (plan in PlanLibrary.challenges + ProgramLibrary.all + ProgramLibrary.focusPrograms) {
            assertEquals(plan.id, PlanLibrary.plan(plan.id, profile)?.id)
        }
        assertNotNull(PlanLibrary.plan(PlanLibrary.personalPlanID, profile))
        assertNull(PlanLibrary.plan("nope", profile))
    }

    @Test
    fun quickWorkoutsFitTheTimeAndOnlyUsePickedEquipment() {
        val equipmentSets: List<Set<Equipment>> = listOf(
            setOf(Equipment.MAT),
            setOf(Equipment.MAT, Equipment.BAND, Equipment.WEIGHTS),
            Equipment.entries.toSet(),
        )
        for (equipment in equipmentSets) {
            for (focus in ProgramFocus.entries) {
                for (minutes in QuickWorkoutBuilder.minuteOptions) {
                    val workout = QuickWorkoutBuilder.build(equipment = equipment, focuses = listOf(focus), minutes = minutes, level = ExerciseLevel.INTERMEDIATE)
                    val label = "${focus.title} $minutes min with ${equipment.size} items"
                    assertTrue("$label is empty", workout.slots.isNotEmpty())

                    val timeline = WorkoutTimeline(workout, equipment)
                    val actual = timeline.totalDuration / 60
                    assertTrue("$label runs $actual min", actual >= minutes * 0.7 && actual <= minutes * 1.3)

                    for (slot in workout.slots) {
                        val exercise = ExerciseLibrary.exercise(slot.exerciseID)!!
                        assertTrue("$label uses ${exercise.name}", (equipment + Equipment.MAT).containsAll(exercise.equipment))
                    }
                    // No move twice in a row.
                    if (workout.slots.size > 1) {
                        for ((a, b) in workout.slots.zipWithNext()) {
                            assertTrue("$label repeats ${a.exerciseID}", a.exerciseID != b.exerciseID)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun quickWorkoutsTargetTheChosenAreas() {
        val workout = QuickWorkoutBuilder.build(equipment = Equipment.entries.toSet(), focuses = listOf(ProgramFocus.UPPER_BODY), minutes = 20, level = ExerciseLevel.BEGINNER)
        val hits = workout.slots.filter { slot ->
            val areas = ExerciseLibrary.exercise(slot.exerciseID)!!.bodyAreas
            BodyArea.ARMS in areas || BodyArea.BACK in areas
        }
        assertEquals(workout.slots.size, hits.size)
        assertEquals(listOf(BodyArea.ARMS, BodyArea.BACK), workout.focus)
        assertEquals("Quick Upper body", workout.title)

        // Picking a band means band moves show up.
        val banded = QuickWorkoutBuilder.build(equipment = setOf(Equipment.MAT, Equipment.BAND), focuses = listOf(ProgramFocus.BACK), minutes = 15, level = ExerciseLevel.BEGINNER)
        assertTrue(banded.slots.any { Equipment.BAND in ExerciseLibrary.exercise(it.exerciseID)!!.equipment })

        // Nothing picked means full body.
        assertEquals(
            listOf(BodyArea.FULL_BODY),
            QuickWorkoutBuilder.build(equipment = setOf(Equipment.MAT), focuses = emptyList(), minutes = 10, level = ExerciseLevel.BEGINNER).focus,
        )
    }

    @Test
    fun quickWorkoutsUseVersionsForHerLevel() {
        for (level in ExerciseLevel.entries) {
            val workout = QuickWorkoutBuilder.build(equipment = Equipment.entries.toSet(), focuses = listOf(ProgramFocus.CORE), minutes = 30, level = level)
            for (slot in workout.slots) {
                val exercise = ExerciseLibrary.exercise(slot.exerciseID)!!
                val variation = exercise.variations.first { it.id == slot.variationID }
                val easiest = exercise.variations.minOf { it.level.rank }
                assertTrue(variation.level.rank <= max(level.rank, easiest))
            }
        }
    }

    @Test
    fun challengesWorkWithJustAMat() {
        // Every challenge except the reformer one adapts to a mat.
        for (challenge in PlanLibrary.challenges) {
            if (challenge.category == ProgramCategory.REFORMER) continue
            assertTrue(challenge.isChallenge)
            for (day in challenge.days) {
                val workout = day.workout
                assertNotNull("${challenge.title} day ${day.number} has no workout", workout)
                val timeline = WorkoutTimeline(workout!!, setOf(Equipment.MAT))
                assertTrue(
                    "${challenge.title} day ${day.number} needs equipment",
                    timeline.exercises.all { it.exercise.equipment == listOf(Equipment.MAT) },
                )
            }
        }
        val reformer = PlanLibrary.reformer28
        assertTrue(reformer.days.any { it.workout?.requires == Equipment.REFORMER })
    }

    @Test
    fun challengesMatchTheirNamesAndRunLong() {
        for (challenge in PlanLibrary.challenges) {
            // "30-Day …" has 30 days, numbered 1…30, seven to a week.
            val length = challenge.title.takeWhile { it.isDigit() }.toIntOrNull()
            if (length != null) {
                assertEquals("${challenge.title} has ${challenge.days.size} days", length, challenge.days.size)
            }
            assertEquals((1..challenge.days.size).toList(), challenge.days.map { it.number })
            assertTrue(challenge.days.all { it.week == (it.number - 1) / 7 + 1 })
            assertTrue("${challenge.title} has a missing workout", challenge.days.all { it.workout != null })
        }
        val popular = PlanLibrary.popularChallenges
        assertTrue(popular.size >= 6)
        assertTrue(popular.any { it.days.size >= 60 })
        assertTrue(popular.count { it.days.size >= 28 } >= 4)
        assertEquals(PlanLibrary.challenges.size, PlanLibrary.challenges.map { it.id }.toSet().size)
    }

    @Test
    fun starterPlanNeverShowsUpAsAProgram() {
        val store = freshStore()
        val profile = UserProfile()
        store.enroll(PlanLibrary.personalPlanID)
        store.log(WorkoutSession(date = Instant.now(), workoutID = "core-wake-up", workoutTitle = "", planID = PlanLibrary.personalPlanID, dayNumber = 1, seconds = 300.0))
        assertTrue(store.enrolledPlanIDs.isEmpty())
        assertTrue(store.activePrograms(profile).isEmpty())

        // Installs that already had it saved drop it on launch.
        val defaults = MemoryStore()
        defaults.putString(
            ActivityStore.ENROLLED_KEY,
            AppJson.encodeToString(ListSerializer(String.serializer()), listOf(PlanLibrary.personalPlanID, PlanLibrary.sevenDayCore.id)),
        )
        assertEquals(listOf(PlanLibrary.sevenDayCore.id), ActivityStore(defaults).enrolledPlanIDs)
    }

    @Test
    fun recommendedProgramsHaveNoDuplicates() {
        val profile = UserProfile(goals = setOf(PilatesGoal.CORE_STRENGTH, PilatesGoal.TONE))
        val picks = ProgramLibrary.recommended(profile)
        assertTrue(picks.isNotEmpty())
        assertEquals(picks.size, picks.map { it.id }.toSet().size)
    }
}
