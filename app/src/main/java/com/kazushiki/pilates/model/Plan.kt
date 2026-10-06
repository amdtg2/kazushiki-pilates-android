package com.kazushiki.pilates.model

import com.kazushiki.pilates.exercises.ExerciseLevel

data class PlanDay(
    val number: Int,
    val week: Int,
    val workoutID: String,
) {
    val id: Int get() = number
    val workout: Workout? get() = WorkoutLibrary.workout(workoutID)
}

data class Plan(
    val id: String,
    val title: String,
    val summary: String,
    /** Challenges unlock one day at a time; regular plans leave every day open. */
    val isChallenge: Boolean,
    val days: List<PlanDay>,
    /** Set for programs and most challenges; the personal plan leaves it empty. */
    val level: ExerciseLevel? = null,
    val category: ProgramCategory = ProgramCategory.MAT,
    /** Set for body-area programs. */
    val focus: ProgramFocus? = null,
    /** Shown first, with a "Popular" tag, on the Challenges page. */
    val isPopular: Boolean = false,
) {
    val weeks: Int get() = days.maxOfOrNull { it.week } ?: 0

    // Plans are identified by ID (like the Swift Hashable conformance).
    override fun equals(other: Any?): Boolean = other is Plan && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

object PlanLibrary {
    const val personalPlanID = "personal"

    /** The user's own plan, built from their onboarding answers. */
    fun personalPlan(profile: UserProfile): Plan {
        val suggestion = PlanSuggestion.make(profile)
        // Mat workouts first, so day 1 is always the free Core Wake-Up.
        val rotation = WorkoutLibrary.workouts(profile.level, profile.equipment)
        val days = mutableListOf<PlanDay>()
        for (week in 1..suggestion.weeks) {
            repeat(suggestion.sessionsPerWeek) {
                val workout = rotation[days.size % rotation.size]
                days += PlanDay(number = days.size + 1, week = week, workoutID = workout.id)
            }
        }
        return Plan(
            id = personalPlanID,
            title = suggestion.title,
            summary = "${suggestion.weeks} weeks · ${suggestion.sessionsPerWeek} sessions a week",
            isChallenge = false,
            days = days,
        )
    }

    /** Finds any plan, challenge or program by ID. */
    fun plan(id: String, profile: UserProfile): Plan? {
        if (id == personalPlanID) return personalPlan(profile)
        return (challenges + ProgramLibrary.all + ProgramLibrary.focusPrograms).firstOrNull { it.id == id }
    }

    // Challenges: one workout a day, each day unlocking when the one before is done.
    // Longer challenges build week by week and end each week with a stretch day.

    /** Lays out a challenge from its weeks of workout IDs (plus any extra days at the end). */
    fun challenge(
        id: String,
        title: String,
        summary: String,
        level: ExerciseLevel? = null,
        category: ProgramCategory = ProgramCategory.MAT,
        isPopular: Boolean = false,
        weeks: List<List<String>>,
    ): Plan {
        val ids = weeks.flatten()
        return Plan(
            id = id, title = title, summary = summary, isChallenge = true,
            days = ids.mapIndexed { offset, workoutID -> PlanDay(number = offset + 1, week = offset / 7 + 1, workoutID = workoutID) },
            level = level, category = category, isPopular = isPopular,
        )
    }

    // Popular

    val thirtyDayPilates = challenge(
        "thirty-day-pilates", "30-Day Pilates Challenge",
        "The classic. Start with the basics and finish with a full flow, one session a day for a month.",
        level = ExerciseLevel.BEGINNER, isPopular = true,
        weeks = listOf(
            listOf("mat-basics", "core-wake-up", "back-care", "glute-basics", "gentle-start", "core-basics", "stretch-reset"),
            listOf("mat-basics", "core-basics", "bridge-builder", "posture-prep", "lower-body-basics", "total-body-mix", "stretch-reset"),
            listOf("rolling-core", "plank-power", "glute-sculpt", "back-care", "core-strength", "full-body-flow", "stretch-reset"),
            listOf("core-sculpt", "lower-body-burn", "posture-strength", "hundred-and-bridge", "plank-power", "full-body-flow", "stretch-reset"),
            listOf("core-sculpt", "full-body-flow"),
        ),
    )

    val wallPilates28 = challenge(
        "wall-pilates-28", "28-Day Wall Pilates Challenge",
        "The wall as your studio: bridges, lifts and core work that get stronger every week. No wall handy? Moves swap to the mat.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.WALL, isPopular = true,
        weeks = listOf(
            listOf("wall-starter", "core-wake-up", "glute-basics", "wall-starter", "stretch-reset", "wall-starter", "back-care"),
            listOf("wall-strength", "core-basics", "wall-starter", "glute-basics", "wall-strength", "plank-power", "stretch-reset"),
            listOf("wall-strength", "core-strength", "wall-strength", "glute-sculpt", "wall-starter", "lower-body-burn", "posture-prep"),
            listOf("wall-strength", "core-sculpt", "wall-strength", "glute-sculpt", "wall-strength", "lower-body-burn", "stretch-reset"),
        ),
    )

    val core21 = challenge(
        "core-21", "21-Day Core Challenge",
        "Three weeks to a stronger, steadier center, from dead bugs to the full Hundred.",
        level = ExerciseLevel.INTERMEDIATE, isPopular = true,
        weeks = listOf(
            listOf("core-wake-up", "core-basics", "back-care", "mat-basics", "core-basics", "rolling-core", "stretch-reset"),
            listOf("rolling-core", "core-strength", "plank-power", "core-sculpt", "back-care", "core-strength", "stretch-reset"),
            listOf("core-sculpt", "plank-power", "hundred-and-bridge", "core-strength", "advanced-core-burn", "core-sculpt", "posture-prep"),
        ),
    )

    val glutes30 = challenge(
        "glutes-30", "30-Day Glute Challenge",
        "A month of bridges, lifts and lower-body burners to build and lift your glutes.",
        level = ExerciseLevel.BEGINNER, isPopular = true,
        weeks = listOf(
            listOf("glute-basics", "bridge-builder", "core-basics", "lower-body-basics", "glute-basics", "wall-starter", "stretch-reset"),
            listOf("glute-sculpt", "lower-body-basics", "plank-power", "bridge-builder", "glute-basics", "total-body-mix", "stretch-reset"),
            listOf("glute-sculpt", "lower-body-burn", "core-strength", "glute-sculpt", "wall-strength", "hundred-and-bridge", "stretch-reset"),
            listOf("lower-body-burn", "glute-sculpt", "plank-power", "lower-body-burn", "wall-strength", "glute-sculpt", "stretch-reset"),
            listOf("glute-sculpt", "lower-body-burn"),
        ),
    )

    val totalBody60: Plan = run {
        val foundations = listOf("total-body-mix", "core-basics", "lower-body-basics", "posture-prep", "glute-basics", "mat-basics", "stretch-reset")
        val build = listOf("full-body-flow", "core-strength", "lower-body-burn", "upper-body-sculpt", "glute-sculpt", "plank-power", "stretch-reset")
        val peak = listOf("classical-flow", "core-sculpt", "lower-body-burn", "advanced-core-burn", "glute-sculpt", "full-body-flow", "stretch-reset")
        challenge(
            "total-body-60", "60-Day Total Body Challenge",
            "Our longest challenge. Two months that move from foundations to advanced classical work.",
            level = ExerciseLevel.INTERMEDIATE, isPopular = true,
            weeks = listOf(
                foundations, foundations, build, build, build, peak, peak, peak,
                listOf("classical-flow", "core-sculpt", "glute-sculpt", "full-body-flow"),
            ),
        )
    }

    val reformer28 = challenge(
        "reformer-28", "28-Day Reformer Challenge",
        "Four weeks on the reformer, from footwork to the full burn, with mat days in between.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.REFORMER, isPopular = true,
        weeks = listOf(
            listOf("reformer-intro", "reformer-foundations", "stretch-reset", "reformer-intro", "reformer-foundations", "core-basics", "stretch-reset"),
            listOf("reformer-foundations", "reformer-flow", "core-basics", "reformer-strength", "reformer-flow", "back-care", "stretch-reset"),
            listOf("reformer-strength", "reformer-flow", "core-strength", "reformer-strength", "reformer-flow", "glute-sculpt", "stretch-reset"),
            listOf("reformer-burn", "reformer-strength", "core-sculpt", "reformer-burn", "reformer-flow", "reformer-strength", "stretch-reset"),
        ),
    )

    val armsAndAbs14 = challenge(
        "arms-abs-14", "14-Day Arms and Abs Challenge",
        "Toned arms and a strong core in two weeks. Light weights help, but the mat works too.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.WEIGHTS, isPopular = true,
        weeks = listOf(
            listOf("weights-sculpt", "core-basics", "plank-power", "weights-sculpt", "core-wake-up", "upper-body-sculpt", "stretch-reset"),
            listOf("arms-and-abs", "core-strength", "plank-power", "arms-and-abs", "core-sculpt", "upper-body-sculpt", "stretch-reset"),
        ),
    )

    // More challenges

    val posture21 = challenge(
        "posture-21", "21-Day Posture Reset",
        "Ease desk stiffness and stand taller with three weeks of back and core work.",
        level = ExerciseLevel.BEGINNER,
        weeks = listOf(
            listOf("posture-prep", "back-care", "stretch-reset", "gentle-start", "posture-prep", "back-care", "stretch-reset"),
            listOf("posture-prep", "posture-strength", "back-care", "core-basics", "posture-strength", "gentle-start", "stretch-reset"),
            listOf("posture-strength", "plank-power", "posture-strength", "core-strength", "back-care", "posture-strength", "stretch-reset"),
        ),
    )

    val stretch14 = challenge(
        "stretch-14", "14-Day Stretch and Flexibility",
        "Gentle daily sessions to loosen your spine, hips and hamstrings.",
        level = ExerciseLevel.BEGINNER,
        weeks = listOf(
            listOf("stretch-reset", "gentle-start", "back-care", "posture-prep", "stretch-reset", "mat-basics", "back-care"),
            listOf("stretch-reset", "posture-prep", "gentle-start", "posture-strength", "stretch-reset", "back-care", "stretch-reset"),
        ),
    )

    val fourteenDayFullBody = challenge(
        "fourteen-day-full-body", "14-Day Full Body Challenge",
        "Two weeks that build from the basics to a full classical flow.",
        level = ExerciseLevel.BEGINNER,
        weeks = listOf(
            listOf("total-body-mix", "core-basics", "lower-body-basics", "mat-basics", "back-care", "glute-basics", "total-body-mix"),
            listOf("core-strength", "lower-body-burn", "plank-power", "posture-strength", "glute-sculpt", "core-sculpt", "full-body-flow"),
        ),
    )

    val sevenDayCore = challenge(
        "seven-day-core", "7-Day Core Challenge",
        "One short session a day. Each day unlocks when you finish the one before.",
        level = ExerciseLevel.BEGINNER,
        weeks = listOf(listOf("core-wake-up", "bridge-builder", "core-wake-up", "bridge-builder", "core-wake-up", "bridge-builder", "hundred-and-bridge")),
    )

    val sevenDayGlutes = challenge(
        "seven-day-glutes", "7-Day Glute Challenge",
        "A week of bridges, lifts and lower-body work. One short session a day.",
        level = ExerciseLevel.BEGINNER,
        weeks = listOf(listOf("glute-basics", "bridge-builder", "lower-body-basics", "core-wake-up", "glute-sculpt", "lower-body-burn", "glute-sculpt")),
    )

    val postureReset = challenge(
        "seven-day-posture", "7-Day Posture Reset",
        "Ease stiffness and stand taller with gentle back and core sessions.",
        level = ExerciseLevel.BEGINNER,
        weeks = listOf(listOf("posture-prep", "back-care", "stretch-reset", "gentle-start", "posture-prep", "posture-strength", "stretch-reset")),
    )

    /** Popular challenges first, then the shorter ones. (Declared after the challenges it lists.) */
    val challenges: List<Plan> = listOf(
        thirtyDayPilates, wallPilates28, core21, glutes30, totalBody60, reformer28, armsAndAbs14,
        posture21, stretch14, fourteenDayFullBody, sevenDayCore, sevenDayGlutes, postureReset,
    )

    val popularChallenges: List<Plan> get() = challenges.filter { it.isPopular }
    val otherChallenges: List<Plan> get() = challenges.filter { !it.isPopular }
}
