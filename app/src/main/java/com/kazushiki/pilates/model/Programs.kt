package com.kazushiki.pilates.model

import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.ExerciseLevel

/** How programs are grouped when browsing programs. */
enum class ProgramCategory(val title: String) {
    MAT("Mat"),
    MIXED("Mixed equipment"),
    BAND("Resistance band"),
    RING("Pilates ring"),
    BALL("Small ball"),
    WEIGHTS("Light weights"),
    WALL("Wall"),
    REFORMER("Reformer");

    val id: String get() = name.lowercase()

    /** The equipment a category needs, or null for mat and mixed programs, which work for everyone. */
    val equipment: Equipment?
        get() = when (this) {
            MAT, MIXED -> null
            BAND -> Equipment.BAND
            RING -> Equipment.RING
            BALL -> Equipment.BALL
            WEIGHTS -> Equipment.WEIGHTS
            WALL -> Equipment.WALL
            REFORMER -> Equipment.REFORMER
        }
}

/** What a body-area program targets. */
enum class ProgramFocus(
    val rawValue: String,
    val title: String,
    val subtitle: String,
    val systemImage: String,
) {
    UPPER_BODY("upperBody", "Upper body", "Arms, shoulders and upper back", "figure.strengthtraining.traditional"),
    CORE("core", "Core", "Abs, obliques and deep core", "figure.core.training"),
    LOWER_BODY("lowerBody", "Lower body", "Thighs, hamstrings and calves", "figure.step.training"),
    GLUTES("glutes", "Glutes", "Lift and strengthen", "figure.cooldown"),
    BACK("back", "Back and posture", "Stand taller, ease stiffness", "figure.stand"),
    FULL_BODY("fullBody", "Full body", "Everything in one session", "figure.pilates");

    val id: String get() = rawValue

    companion object {
        /** Body areas to suggest first for each onboarding goal. */
        fun suggested(goals: Set<PilatesGoal>): List<ProgramFocus> {
            val result = mutableListOf<ProgramFocus>()
            for (goal in PilatesGoal.entries) {
                if (goal !in goals) continue
                val picks: List<ProgramFocus> = when (goal) {
                    PilatesGoal.CORE_STRENGTH -> listOf(ProgramFocus.CORE)
                    PilatesGoal.TONE -> listOf(ProgramFocus.GLUTES, ProgramFocus.UPPER_BODY, ProgramFocus.LOWER_BODY)
                    PilatesGoal.POSTURE -> listOf(ProgramFocus.BACK)
                    PilatesGoal.FLEXIBILITY -> listOf(ProgramFocus.BACK, ProgramFocus.FULL_BODY)
                    PilatesGoal.RECOVERY -> listOf(ProgramFocus.BACK)
                }
                for (pick in picks) {
                    if (pick !in result) result.add(pick)
                }
            }
            return result
        }
    }
}

object ProgramLibrary {

    /** Lays out a program: [perWeek] sessions a week for [weeks] weeks, cycling through its workouts. */
    fun program(
        id: String, title: String, summary: String, level: ExerciseLevel, category: ProgramCategory,
        weeks: Int, perWeek: Int, workouts: List<Workout>,
    ): Plan {
        val days = (0 until weeks * perWeek).map { index ->
            PlanDay(number = index + 1, week = index / perWeek + 1, workoutID = workouts[index % workouts.size].id)
        }
        return Plan(id = id, title = title, summary = summary, isChallenge = false, days = days, level = level, category = category)
    }

    private fun focusProgram(
        id: String, title: String, summary: String, level: ExerciseLevel, focus: ProgramFocus,
        weeks: Int, perWeek: Int, workouts: List<Workout>,
    ): Plan {
        val plan = program(
            id = id, title = title, summary = summary, level = level, category = ProgramCategory.MIXED,
            weeks = weeks, perWeek = perWeek, workouts = workouts,
        )
        return plan.copy(focus = focus)
    }

    // Mat
    val matFoundations = program(
        id = "program-mat-foundations", title = "Mat Foundations",
        summary = "Learn the basics: breathing, core control and a strong back.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.MAT, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.coreWakeUp, WorkoutLibrary.matBasics, WorkoutLibrary.posturePrep),
    )
    val stretchAndRestore = program(
        id = "program-stretch-restore", title = "Stretch and Restore",
        summary = "Gentle mobility and back care for recovery weeks.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.MAT, weeks = 2, perWeek = 3,
        workouts = listOf(WorkoutLibrary.stretchReset, WorkoutLibrary.backCare),
    )
    val coreSculptProgram = program(
        id = "program-core-sculpt", title = "Core Sculpt",
        summary = "Roll ups, Teasers and planks to build a strong, defined center.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.MAT, weeks = 4, perWeek = 4,
        workouts = listOf(WorkoutLibrary.coreSculpt, WorkoutLibrary.plankPower, WorkoutLibrary.rollingCore),
    )
    val classicalMat = program(
        id = "program-classical-mat", title = "Classical Mat",
        summary = "The classical repertoire with straight-leg Hundreds and full Teasers.",
        level = ExerciseLevel.ADVANCED, category = ProgramCategory.MAT, weeks = 4, perWeek = 4,
        workouts = listOf(WorkoutLibrary.classicalFlow, WorkoutLibrary.advancedCoreBurn, WorkoutLibrary.plankPower),
    )

    // Mixed
    val homeStudio = program(
        id = "program-home-studio", title = "Home Studio",
        summary = "Uses every small prop you own and swaps in mat moves for the rest.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.MIXED, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.homeStudioMix, WorkoutLibrary.fullBodyFlow, WorkoutLibrary.hundredAndBridge),
    )
    val totalBody28 = program(
        id = "program-total-body-28", title = "Total Body 28",
        summary = "Four weeks of full-body sessions built around whatever you have at home.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.MIXED, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.totalBodyMix, WorkoutLibrary.coreWakeUp, WorkoutLibrary.bridgeBuilder),
    )

    // Band
    val bandBasicsProgram = program(
        id = "program-band-basics", title = "Band Basics",
        summary = "Get comfortable with the band: rows, presses and supported roll ups.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.BAND, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.bandBasics, WorkoutLibrary.gentleStart),
    )
    val bandStrengthProgram = program(
        id = "program-band-strength", title = "Band Strength",
        summary = "More resistance and longer sets for back, legs and arms.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.BAND, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.bandStrength, WorkoutLibrary.bandBasics),
    )

    // Ring
    val ringToneUpProgram = program(
        id = "program-ring-tone-up", title = "Ring Tone-Up",
        summary = "Inner thighs, arms and core with the Pilates ring.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.RING, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.ringToneUp, WorkoutLibrary.bridgeBuilder),
    )
    val ringSculptProgram = program(
        id = "program-ring-sculpt", title = "Ring Sculpt",
        summary = "Longer ring sets and harder core work.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.RING, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.ringSculpt, WorkoutLibrary.ringToneUp),
    )

    // Ball
    val ballBasicsProgram = program(
        id = "program-ball-basics", title = "Ball Basics",
        summary = "Core and inner-thigh work with a small ball.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.BALL, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.ballBasics, WorkoutLibrary.coreWakeUp),
    )
    val ballBurnProgram = program(
        id = "program-ball-burn", title = "Ball Core Burn",
        summary = "The Hundred with a ball, curl-ups and planks.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.BALL, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.ballBurn, WorkoutLibrary.ballBasics),
    )

    // Weights
    val weightsSculptProgram = program(
        id = "program-weights-sculpt", title = "Light Weights Sculpt",
        summary = "Toned arms and shoulders with light hand weights.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.WEIGHTS, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.weightsSculpt, WorkoutLibrary.matBasics),
    )
    val armsAndAbsProgram = program(
        id = "program-arms-abs", title = "Arms and Abs",
        summary = "Weighted arm work paired with core sessions.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.WEIGHTS, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.armsAndAbs, WorkoutLibrary.weightsSculpt),
    )

    // Wall
    val wallStarterProgram = program(
        id = "program-wall-starter", title = "Wall Pilates Starter",
        summary = "Use a wall for support while you build glutes and core.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.WALL, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.wallStarter, WorkoutLibrary.stretchReset),
    )
    val wallStrengthProgram = program(
        id = "program-wall-strength", title = "Wall Pilates Strength",
        summary = "Single-leg wall bridges and harder core work.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.WALL, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.wallStrength, WorkoutLibrary.wallStarter),
    )

    // Reformer
    val reformerFoundationsProgram = program(
        id = "program-reformer-foundations", title = "Reformer Foundations",
        summary = "Learn the carriage, springs and straps.",
        level = ExerciseLevel.BEGINNER, category = ProgramCategory.REFORMER, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.reformerIntro, WorkoutLibrary.reformerFoundations),
    )
    val reformerStrengthProgram = program(
        id = "program-reformer-strength", title = "Reformer Strength",
        summary = "Longer sets, leg lowers and bridging on the reformer.",
        level = ExerciseLevel.INTERMEDIATE, category = ProgramCategory.REFORMER, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.reformerStrength, WorkoutLibrary.reformerFlow),
    )
    val reformerBurnProgram = program(
        id = "program-reformer-burn", title = "Reformer Burn",
        summary = "The hardest reformer sessions, four times a week.",
        level = ExerciseLevel.ADVANCED, category = ProgramCategory.REFORMER, weeks = 4, perWeek = 4,
        workouts = listOf(WorkoutLibrary.reformerBurn, WorkoutLibrary.reformerStrength, WorkoutLibrary.reformerFlow),
    )

    // Body-area programs

    val upperBodyStart = focusProgram(
        id = "focus-upper-start", title = "Upper Body Start",
        summary = "Toned arms and a stronger upper back, three times a week.",
        level = ExerciseLevel.BEGINNER, focus = ProgramFocus.UPPER_BODY, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.upperBodyBasics, WorkoutLibrary.weightsSculpt, WorkoutLibrary.posturePrep),
    )
    val upperBodyDefine = focusProgram(
        id = "focus-upper-define", title = "Upper Body Define",
        summary = "Push-ups, planks and heavier sets for definition.",
        level = ExerciseLevel.INTERMEDIATE, focus = ProgramFocus.UPPER_BODY, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.upperBodySculpt, WorkoutLibrary.plankPower, WorkoutLibrary.armsAndAbs),
    )

    val coreStart = focusProgram(
        id = "focus-core-start", title = "Core Start",
        summary = "Learn to find and fire your deep core.",
        level = ExerciseLevel.BEGINNER, focus = ProgramFocus.CORE, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.coreBasics, WorkoutLibrary.coreWakeUp, WorkoutLibrary.matBasics),
    )
    val coreBuild = focusProgram(
        id = "focus-core-build", title = "Core Build",
        summary = "Roll ups, Teasers and planks for a strong, defined center.",
        level = ExerciseLevel.INTERMEDIATE, focus = ProgramFocus.CORE, weeks = 4, perWeek = 4,
        workouts = listOf(WorkoutLibrary.coreStrength, WorkoutLibrary.coreSculpt, WorkoutLibrary.rollingCore),
    )
    val coreAdvanced = focusProgram(
        id = "focus-core-advanced", title = "Core Burn",
        summary = "Straight-leg Hundreds and the classical core series.",
        level = ExerciseLevel.ADVANCED, focus = ProgramFocus.CORE, weeks = 4, perWeek = 4,
        workouts = listOf(WorkoutLibrary.advancedCoreBurn, WorkoutLibrary.coreStrength, WorkoutLibrary.classicalFlow),
    )

    val lowerBodyStart = focusProgram(
        id = "focus-lower-start", title = "Lower Body Start",
        summary = "Stronger thighs and hamstrings with presses and bridges.",
        level = ExerciseLevel.BEGINNER, focus = ProgramFocus.LOWER_BODY, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.lowerBodyBasics, WorkoutLibrary.bridgeBuilder),
    )
    val lowerBodyBuild = focusProgram(
        id = "focus-lower-build", title = "Lower Body Build",
        summary = "Single-leg work and longer sets for strong, toned legs.",
        level = ExerciseLevel.INTERMEDIATE, focus = ProgramFocus.LOWER_BODY, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.lowerBodyBurn, WorkoutLibrary.lowerBodyBasics, WorkoutLibrary.gluteSculpt),
    )

    val gluteStart = focusProgram(
        id = "focus-glute-start", title = "Glute Start",
        summary = "Wake up and strengthen your glutes.",
        level = ExerciseLevel.BEGINNER, focus = ProgramFocus.GLUTES, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.gluteBasics, WorkoutLibrary.bridgeBuilder),
    )
    val gluteBuild = focusProgram(
        id = "focus-glute-build", title = "Glute Build",
        summary = "Single-leg bridges and plank lifts to lift and shape.",
        level = ExerciseLevel.INTERMEDIATE, focus = ProgramFocus.GLUTES, weeks = 4, perWeek = 4,
        workouts = listOf(WorkoutLibrary.gluteSculpt, WorkoutLibrary.gluteBasics, WorkoutLibrary.lowerBodyBurn),
    )

    val postureReset = focusProgram(
        id = "focus-posture-reset", title = "Posture Reset",
        summary = "Undo desk days with gentle extension and mobility.",
        level = ExerciseLevel.BEGINNER, focus = ProgramFocus.BACK, weeks = 3, perWeek = 3,
        workouts = listOf(WorkoutLibrary.posturePrep, WorkoutLibrary.backCare, WorkoutLibrary.stretchReset),
    )
    val postureBuild = focusProgram(
        id = "focus-posture-build", title = "Posture Strength",
        summary = "Rows and back extension for a strong upper back.",
        level = ExerciseLevel.INTERMEDIATE, focus = ProgramFocus.BACK, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.postureStrength, WorkoutLibrary.posturePrep),
    )

    val fullBodyFlowProgram = focusProgram(
        id = "focus-full-flow", title = "Full Body Flow",
        summary = "Balanced sessions that work everything.",
        level = ExerciseLevel.INTERMEDIATE, focus = ProgramFocus.FULL_BODY, weeks = 4, perWeek = 3,
        workouts = listOf(WorkoutLibrary.fullBodyFlow, WorkoutLibrary.homeStudioMix, WorkoutLibrary.plankPower),
    )
    val fullBodyClassical = focusProgram(
        id = "focus-full-classical", title = "Full Body Classical",
        summary = "The classical mat repertoire from start to finish.",
        level = ExerciseLevel.ADVANCED, focus = ProgramFocus.FULL_BODY, weeks = 4, perWeek = 4,
        workouts = listOf(WorkoutLibrary.classicalFlow, WorkoutLibrary.fullBodyFlow, WorkoutLibrary.coreSculpt),
    )

    // Aggregates (declared after every program they list).

    val all: List<Plan> by lazy {
        listOf(
            matFoundations, stretchAndRestore, coreSculptProgram, classicalMat,
            homeStudio, totalBody28,
            bandBasicsProgram, bandStrengthProgram,
            ringToneUpProgram, ringSculptProgram,
            ballBasicsProgram, ballBurnProgram,
            weightsSculptProgram, armsAndAbsProgram,
            wallStarterProgram, wallStrengthProgram,
            reformerFoundationsProgram, reformerStrengthProgram, reformerBurnProgram,
        )
    }

    /** Programs built around a body area. They adapt to whatever equipment the user has. */
    val focusPrograms: List<Plan> by lazy {
        listOf(
            upperBodyStart, upperBodyDefine,
            coreStart, coreBuild, coreAdvanced,
            lowerBodyStart, lowerBodyBuild,
            gluteStart, gluteBuild,
            postureReset, postureBuild,
            fullBodyFlowProgram, fullBodyClassical,
        )
    }

    fun programs(category: ProgramCategory): List<Plan> = all.filter { it.category == category }

    fun programs(focus: ProgramFocus): List<Plan> = focusPrograms.filter { it.focus == focus }

    /** Programs to suggest: body areas that match her goals, then equipment programs she
     *  can do, at her level first. No duplicates. */
    fun recommended(profile: UserProfile): List<Plan> {
        val byGoal = ProgramFocus.suggested(profile.goals).flatMap { programs(it) }
        val byEquipment = all.filter { program ->
            val needed = program.category.equipment
            needed == null || needed in profile.equipment
        }
        val seen = mutableSetOf<String>()
        val available = (byGoal + byEquipment).filter { seen.add(it.id) }
        return available.filter { it.level == profile.level } + available.filter { it.level != profile.level }
    }
}
