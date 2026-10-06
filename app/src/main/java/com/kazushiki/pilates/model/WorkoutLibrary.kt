package com.kazushiki.pilates.model

import com.kazushiki.pilates.exercises.BodyArea
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.exercises.ExerciseLevel

/**
 * Every guided workout. Aggregate lists are declared after the workouts they list
 * (and `all` is lazy) so object initialization never sees an unset property.
 */
object WorkoutLibrary {

    // Slots list the equipment version first; alternatives fall back toward mat-only.
    fun bridge(loops: Int): WorkoutSlot =
        WorkoutSlot(exerciseID = "ring-bridge", variationID = "standard", loops = loops, alternatives = listOf("ball-bridge", "glute-bridge"))

    fun deadBug(loops: Int): WorkoutSlot =
        WorkoutSlot(exerciseID = "weighted-dead-bug", variationID = "standard", loops = loops, alternatives = listOf("dead-bug"))

    fun slot(id: String, loops: Int, variation: String = "standard"): WorkoutSlot =
        WorkoutSlot(exerciseID = id, variationID = variation, loops = loops)

    /** A slot with mat fallbacks for anyone missing the equipment. */
    private fun swap(id: String, loops: Int, alternatives: List<String>, variation: String = "standard"): WorkoutSlot =
        WorkoutSlot(exerciseID = id, variationID = variation, loops = loops, alternatives = alternatives)

    private fun fallback(id: String, loops: Int, alternatives: List<String>, variation: String = "standard"): WorkoutSlot =
        WorkoutSlot(exerciseID = id, variationID = variation, loops = loops, alternatives = alternatives)

    val coreWakeUp = Workout(
        id = "core-wake-up",
        title = "Core Wake-Up",
        summary = "The Hundred to warm up, dead bugs for deep core, then bridges.",
        focus = listOf(BodyArea.CORE, BodyArea.GLUTES),
        level = ExerciseLevel.BEGINNER,
        slots = listOf(
            slot("the-hundred", 1, variation = "tabletop"),
            slot("dead-bug", 6),
            WorkoutSlot(exerciseID = "ball-bridge", variationID = "standard", loops = 8, alternatives = listOf("ring-bridge", "glute-bridge")),
        ),
        isMixed = true,
    )

    val bridgeBuilder = Workout(
        id = "bridge-builder",
        title = "Bridge Builder",
        summary = "Slow, controlled bridges for glutes, hamstrings and spine mobility.",
        focus = listOf(BodyArea.GLUTES, BodyArea.LEGS),
        level = ExerciseLevel.BEGINNER,
        slots = listOf(
            bridge(10),
            WorkoutSlot(exerciseID = "wall-bridge", variationID = "standard", loops = 8, alternatives = listOf("glute-bridge")),
            slot("glute-bridge", 10),
        ),
        isMixed = true,
    )

    val backCare = Workout(
        id = "back-care",
        title = "Back Care",
        summary = "Gentle extension and stability for a strong, comfortable back.",
        focus = listOf(BodyArea.BACK, BodyArea.CORE),
        level = ExerciseLevel.BEGINNER,
        slots = listOf(
            slot("swan-prep", 8),
            slot("bird-dog", 6),
            slot("glute-bridge", 8),
        ),
    )

    val gentleStart = Workout(
        id = "gentle-start",
        title = "Gentle Start",
        summary = "Supported roll ups, bird dogs and bridges. A soft way into Pilates.",
        focus = listOf(BodyArea.CORE, BodyArea.BACK),
        level = ExerciseLevel.BEGINNER,
        slots = listOf(
            WorkoutSlot(exerciseID = "banded-roll-up", variationID = "standard", loops = 5, alternatives = listOf("dead-bug")),
            slot("bird-dog", 6),
            bridge(8),
        ),
        isMixed = true,
    )

    val rollingCore = Workout(
        id = "rolling-core",
        title = "Rolling Core",
        summary = "Roll ups and single-leg stretch, finished with the Hundred.",
        focus = listOf(BodyArea.CORE),
        level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            slot("roll-up", 6),
            slot("single-leg-stretch", 2),
            slot("bird-dog", 6),
            slot("the-hundred", 1, variation = "tabletop"),
        ),
    )

    val hundredAndBridge = Workout(
        id = "hundred-and-bridge",
        title = "Hundred Sandwich",
        summary = "Bridges and dead bugs around a full Hundred.",
        focus = listOf(BodyArea.CORE, BodyArea.GLUTES),
        level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            bridge(10),
            slot("the-hundred", 1, variation = "tabletop"),
            deadBug(6),
            slot("glute-bridge", 10),
        ),
        isMixed = true,
    )

    val fullBodyFlow = Workout(
        id = "full-body-flow",
        title = "Full Body Flow",
        summary = "Front, back and sides: roll ups, extension, stability and bridges.",
        focus = listOf(BodyArea.FULL_BODY),
        level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            slot("roll-up", 5),
            slot("swan-prep", 6),
            slot("bird-dog", 6),
            deadBug(6),
            bridge(10),
        ),
        isMixed = true,
    )

    val advancedCoreBurn = Workout(
        id = "advanced-core-burn",
        title = "Advanced Core Burn",
        summary = "Straight-leg Hundreds, single-leg stretch, roll ups and single-leg bridges.",
        focus = listOf(BodyArea.CORE, BodyArea.GLUTES),
        level = ExerciseLevel.ADVANCED,
        slots = listOf(
            slot("the-hundred", 1, variation = "extended"),
            slot("single-leg-stretch", 3),
            slot("glute-bridge", 10, variation = "single"),
            slot("roll-up", 6),
            slot("the-hundred", 1, variation = "extended"),
        ),
        restSeconds = 20.0,
    )

    // Reformer

    val reformerFoundations = Workout(
        id = "reformer-foundations",
        title = "Reformer Foundations",
        summary = "Footwork, the Hundred in straps, frogs and bridging. The reformer basics.",
        focus = listOf(BodyArea.LEGS, BodyArea.CORE),
        level = ExerciseLevel.BEGINNER,
        slots = listOf(
            slot("reformer-footwork", 10),
            slot("reformer-hundred", 1, variation = "tabletop"),
            slot("reformer-frog", 10),
            slot("reformer-bridge", 8),
        ),
        requires = Equipment.REFORMER,
    )

    val reformerFlow = Workout(
        id = "reformer-flow",
        title = "Reformer Flow",
        summary = "A longer reformer session with extra footwork and bridging.",
        focus = listOf(BodyArea.FULL_BODY),
        level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            slot("reformer-footwork", 12),
            slot("reformer-frog", 10),
            slot("reformer-hundred", 1, variation = "tabletop"),
            slot("reformer-bridge", 10),
            slot("reformer-footwork", 10),
        ),
        requires = Equipment.REFORMER,
    )

    // More workouts: Mat

    val matBasics = Workout(
        id = "mat-basics", title = "Mat Basics",
        summary = "The Hundred, dead bugs, bridges and cat-cow. A gentle full-body start.",
        focus = listOf(BodyArea.CORE, BodyArea.GLUTES), level = ExerciseLevel.BEGINNER,
        slots = listOf(slot("the-hundred", 1, variation = "tabletop"), slot("dead-bug", 6), slot("glute-bridge", 10), slot("cat-cow", 6)),
    )

    val posturePrep = Workout(
        id = "posture-prep", title = "Posture Prep",
        summary = "Back extension and stability to undo a day at the desk.",
        focus = listOf(BodyArea.BACK), level = ExerciseLevel.BEGINNER,
        slots = listOf(slot("cat-cow", 6), slot("swan-prep", 8), slot("swimming", 3), slot("bird-dog", 6)),
    )

    val stretchReset = Workout(
        id = "stretch-reset", title = "Stretch and Reset",
        summary = "Slow spinal movement and stretching for rest days.",
        focus = listOf(BodyArea.BACK), level = ExerciseLevel.BEGINNER,
        slots = listOf(slot("cat-cow", 6), slot("spine-stretch-forward", 6), slot("swan-prep", 6), slot("bird-dog", 4)),
    )

    val plankPower = Workout(
        id = "plank-power", title = "Plank Power",
        summary = "Planks, push-ups and swimming for a strong center and upper body.",
        focus = listOf(BodyArea.CORE, BodyArea.ARMS), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(slot("plank-leg-lift", 6), slot("pilates-push-up", 6), slot("swimming", 4), slot("dead-bug", 6)),
    )

    val coreSculpt = Workout(
        id = "core-sculpt", title = "Core Sculpt",
        summary = "Roll ups, single-leg stretch and the Teaser, finished with a spine stretch.",
        focus = listOf(BodyArea.CORE), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(slot("the-hundred", 1, variation = "tabletop"), slot("roll-up", 6), slot("single-leg-stretch", 2), slot("teaser", 4), slot("spine-stretch-forward", 5)),
    )

    val classicalFlow = Workout(
        id = "classical-flow", title = "Classical Flow",
        summary = "A classical mat sequence: Hundred, roll up, single-leg stretch, Teaser, swimming, push-ups.",
        focus = listOf(BodyArea.FULL_BODY), level = ExerciseLevel.ADVANCED,
        slots = listOf(
            slot("the-hundred", 1, variation = "extended"), slot("roll-up", 6), slot("single-leg-stretch", 3),
            slot("teaser", 5), slot("swimming", 4), slot("pilates-push-up", 6),
        ),
        restSeconds = 20.0,
    )

    // Band

    val bandBasics = Workout(
        id = "band-basics", title = "Band Basics",
        summary = "Supported roll ups, rows and leg presses with a resistance band.",
        focus = listOf(BodyArea.BACK, BodyArea.LEGS), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            swap("banded-roll-up", 5, listOf("dead-bug")), swap("seated-band-row", 10, listOf("swimming")),
            swap("banded-leg-press", 10, listOf("glute-bridge")), slot("glute-bridge", 10),
        ),
    )

    val bandStrength = Workout(
        id = "band-strength", title = "Band Strength",
        summary = "More rows and presses with the band, plus planks.",
        focus = listOf(BodyArea.BACK, BodyArea.LEGS, BodyArea.ARMS), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            swap("seated-band-row", 12, listOf("swimming")), swap("banded-leg-press", 12, listOf("glute-bridge")),
            swap("banded-roll-up", 6, listOf("roll-up")), slot("plank-leg-lift", 6), swap("seated-band-row", 10, listOf("swimming")),
        ),
    )

    // Ring

    val ringToneUp = Workout(
        id = "ring-tone-up", title = "Ring Tone-Up",
        summary = "Squeeze the ring through curl-ups, bridges and arm presses.",
        focus = listOf(BodyArea.CORE, BodyArea.LEGS, BodyArea.ARMS), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            swap("ring-curl-up", 8, listOf("ball-curl-up", "dead-bug")), swap("ring-bridge", 10, listOf("ball-bridge", "glute-bridge")),
            swap("ring-arm-press", 10, listOf("bird-dog")),
        ),
    )

    val ringSculpt = Workout(
        id = "ring-sculpt", title = "Ring Sculpt",
        summary = "Longer ring sets for arms, inner thighs and core.",
        focus = listOf(BodyArea.ARMS, BodyArea.LEGS, BodyArea.CORE), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            swap("ring-arm-press", 12, listOf("pilates-push-up")), swap("ring-curl-up", 10, listOf("ball-curl-up", "dead-bug")),
            swap("ring-bridge", 12, listOf("ball-bridge", "glute-bridge")), slot("single-leg-stretch", 2),
        ),
    )

    // Ball

    val ballBasics = Workout(
        id = "ball-basics", title = "Ball Basics",
        summary = "Curl-ups and bridges squeezing a small ball, plus dead bugs.",
        focus = listOf(BodyArea.CORE, BodyArea.LEGS), level = ExerciseLevel.BEGINNER,
        slots = listOf(swap("ball-curl-up", 8, listOf("ring-curl-up", "dead-bug")), swap("ball-bridge", 10, listOf("ring-bridge", "glute-bridge")), slot("dead-bug", 6)),
    )

    val ballBurn = Workout(
        id = "ball-burn", title = "Ball Core Burn",
        summary = "The Hundred with a ball, curl-ups, bridges and planks.",
        focus = listOf(BodyArea.CORE, BodyArea.LEGS), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            swap("ball-hundred", 1, listOf("the-hundred"), variation = "tabletop"), swap("ball-curl-up", 10, listOf("ring-curl-up", "dead-bug")),
            swap("ball-bridge", 12, listOf("ring-bridge", "glute-bridge")), slot("plank-leg-lift", 6),
        ),
    )

    // Weights

    val weightsSculpt = Workout(
        id = "weights-sculpt", title = "Light Weights Sculpt",
        summary = "Chest presses, front raises and weighted dead bugs.",
        focus = listOf(BodyArea.ARMS, BodyArea.CORE), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            swap("weighted-chest-press", 10, listOf("bird-dog")), swap("weighted-front-raise", 10, listOf("swimming")),
            swap("weighted-dead-bug", 6, listOf("dead-bug")),
        ),
    )

    val armsAndAbs = Workout(
        id = "arms-and-abs", title = "Arms and Abs",
        summary = "Heavier sets for arms and shoulders, plus push-ups and the Hundred.",
        focus = listOf(BodyArea.ARMS, BodyArea.CORE), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            swap("weighted-front-raise", 12, listOf("swimming")), swap("weighted-chest-press", 12, listOf("pilates-push-up")),
            swap("weighted-dead-bug", 8, listOf("dead-bug")), slot("pilates-push-up", 6), slot("the-hundred", 1, variation = "tabletop"),
        ),
    )

    // Wall

    val wallStarter = Workout(
        id = "wall-starter", title = "Wall Pilates Starter",
        summary = "Wall bridges and wall dead bugs between gentle back work.",
        focus = listOf(BodyArea.GLUTES, BodyArea.CORE), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            slot("cat-cow", 5), swap("wall-bridge", 8, listOf("glute-bridge")),
            swap("wall-dead-bug", 6, listOf("dead-bug")), slot("swan-prep", 6),
        ),
    )

    val wallStrength = Workout(
        id = "wall-strength", title = "Wall Pilates Strength",
        summary = "Single-leg wall bridges, wall dead bugs and planks.",
        focus = listOf(BodyArea.GLUTES, BodyArea.LEGS, BodyArea.CORE), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            swap("wall-single-leg-bridge", 8, listOf("glute-bridge")), swap("wall-dead-bug", 8, listOf("dead-bug")),
            swap("wall-bridge", 10, listOf("glute-bridge")), slot("plank-leg-lift", 6),
        ),
    )

    // Reformer (more)

    val reformerIntro = Workout(
        id = "reformer-intro", title = "Reformer Intro",
        summary = "Footwork, arm pulls and frogs to learn the carriage.",
        focus = listOf(BodyArea.LEGS, BodyArea.ARMS), level = ExerciseLevel.BEGINNER,
        slots = listOf(slot("reformer-footwork", 10), slot("reformer-arm-pulls", 10), slot("reformer-frog", 8)),
        requires = Equipment.REFORMER,
    )

    val reformerStrength = Workout(
        id = "reformer-strength", title = "Reformer Strength",
        summary = "Footwork on the toes, arm pulls, leg lowers and bridging.",
        focus = listOf(BodyArea.LEGS, BodyArea.CORE, BodyArea.ARMS), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            slot("reformer-footwork", 10, variation = "toes"), slot("reformer-arm-pulls", 12),
            slot("reformer-leg-lowers", 8), slot("reformer-bridge", 10),
        ),
        requires = Equipment.REFORMER,
    )

    val reformerBurn = Workout(
        id = "reformer-burn", title = "Reformer Burn",
        summary = "A long reformer session: footwork, the Hundred with straight legs, leg lowers, frogs, arms and bridging.",
        focus = listOf(BodyArea.FULL_BODY), level = ExerciseLevel.ADVANCED,
        slots = listOf(
            slot("reformer-footwork", 12), slot("reformer-hundred", 1, variation = "extended"),
            slot("reformer-leg-lowers", 10), slot("reformer-frog", 12),
            slot("reformer-arm-pulls", 12), slot("reformer-bridge", 10),
        ),
        restSeconds = 20.0,
        requires = Equipment.REFORMER,
    )

    // Mixed equipment

    val homeStudioMix = Workout(
        id = "home-studio-mix", title = "Home Studio Mix",
        summary = "Ring, band, weights, ball and wall in one session. Moves swap to mat versions for anything you don't have.",
        focus = listOf(BodyArea.FULL_BODY), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            swap("ring-bridge", 10, listOf("ball-bridge", "glute-bridge")), swap("seated-band-row", 10, listOf("swimming")),
            swap("weighted-front-raise", 10, listOf("pilates-push-up")), swap("ball-curl-up", 10, listOf("ring-curl-up", "dead-bug")),
            swap("wall-dead-bug", 6, listOf("dead-bug")),
        ),
        isMixed = true,
    )

    val totalBodyMix = Workout(
        id = "total-body-mix", title = "Total Body Mix",
        summary = "A beginner full-body session using whatever equipment you have.",
        focus = listOf(BodyArea.FULL_BODY), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            swap("banded-roll-up", 5, listOf("dead-bug")), swap("weighted-chest-press", 10, listOf("bird-dog")),
            swap("ball-bridge", 10, listOf("ring-bridge", "glute-bridge")), swap("wall-bridge", 8, listOf("glute-bridge")),
            slot("swan-prep", 6),
        ),
        isMixed = true,
    )

    // Body-area workouts. Every equipment move has a mat fallback, so they suit everyone.

    val upperBodyBasics = Workout(
        id = "upper-body-basics", title = "Upper Body Basics",
        summary = "Presses, rows and raises for arms, shoulders and upper back.",
        focus = listOf(BodyArea.ARMS, BodyArea.BACK), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            fallback("weighted-chest-press", 10, listOf("bird-dog")),
            fallback("seated-band-row", 10, listOf("swimming")),
            fallback("ring-arm-press", 10, listOf("swan-prep")),
            fallback("weighted-front-raise", 10, listOf("cat-cow")),
        ),
        isMixed = true,
    )

    val upperBodySculpt = Workout(
        id = "upper-body-sculpt", title = "Upper Body Sculpt",
        summary = "Push-ups and planks plus heavier rows, presses and raises.",
        focus = listOf(BodyArea.ARMS, BodyArea.BACK, BodyArea.CORE), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            slot("pilates-push-up", 6),
            fallback("seated-band-row", 12, listOf("swimming")),
            fallback("weighted-front-raise", 12, listOf("plank-leg-lift")),
            fallback("reformer-arm-pulls", 12, listOf("weighted-chest-press", "pilates-push-up")),
            slot("plank-leg-lift", 6),
        ),
        isMixed = true,
    )

    val coreBasics = Workout(
        id = "core-basics", title = "Core Basics",
        summary = "The Hundred, dead bugs and curl-ups to switch on your deep core.",
        focus = listOf(BodyArea.CORE), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            slot("the-hundred", 1, variation = "tabletop"),
            slot("dead-bug", 6),
            fallback("ball-curl-up", 8, listOf("ring-curl-up", "bird-dog")),
            fallback("wall-dead-bug", 6, listOf("bird-dog")),
        ),
        isMixed = true,
    )

    val coreStrength = Workout(
        id = "core-strength", title = "Core Strength",
        summary = "Roll ups, single-leg stretch, Teasers and planks.",
        focus = listOf(BodyArea.CORE), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            fallback("ball-hundred", 1, listOf("the-hundred"), variation = "tabletop"),
            slot("roll-up", 6),
            slot("single-leg-stretch", 2),
            slot("teaser", 4),
            slot("plank-leg-lift", 6),
        ),
        isMixed = true,
    )

    val lowerBodyBasics = Workout(
        id = "lower-body-basics", title = "Lower Body Basics",
        summary = "Leg presses and bridges for thighs and hamstrings.",
        focus = listOf(BodyArea.LEGS, BodyArea.GLUTES), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            fallback("reformer-footwork", 10, listOf("banded-leg-press", "glute-bridge")),
            fallback("wall-bridge", 8, listOf("glute-bridge")),
            fallback("ring-bridge", 10, listOf("ball-bridge", "glute-bridge")),
            fallback("banded-leg-press", 10, listOf("swimming")),
        ),
        isMixed = true,
    )

    val lowerBodyBurn = Workout(
        id = "lower-body-burn", title = "Lower Body Burn",
        summary = "Single-leg bridges, frogs and leg presses. Expect to feel it.",
        focus = listOf(BodyArea.LEGS, BodyArea.GLUTES), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            fallback("wall-single-leg-bridge", 8, listOf("glute-bridge")),
            fallback("reformer-frog", 10, listOf("banded-leg-press", "glute-bridge")),
            fallback("banded-leg-press", 12, listOf("swimming")),
            fallback("ring-bridge", 12, listOf("ball-bridge", "glute-bridge")),
            slot("plank-leg-lift", 6),
        ),
        isMixed = true,
    )

    val gluteBasics = Workout(
        id = "glute-basics", title = "Glute Basics",
        summary = "Bridges, bird dogs and swimming to wake up your glutes.",
        focus = listOf(BodyArea.GLUTES), level = ExerciseLevel.BEGINNER,
        slots = listOf(
            slot("glute-bridge", 10),
            fallback("ball-bridge", 10, listOf("ring-bridge", "glute-bridge")),
            slot("bird-dog", 6),
            fallback("wall-bridge", 8, listOf("glute-bridge")),
            slot("swimming", 3),
        ),
        isMixed = true,
    )

    val gluteSculpt = Workout(
        id = "glute-sculpt", title = "Glute Sculpt",
        summary = "Single-leg bridges, plank leg lifts and long holds at the top.",
        focus = listOf(BodyArea.GLUTES, BodyArea.LEGS), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            fallback("wall-single-leg-bridge", 8, listOf("glute-bridge")),
            slot("glute-bridge", 10, variation = "single"),
            slot("plank-leg-lift", 6),
            fallback("reformer-bridge", 10, listOf("ring-bridge", "glute-bridge")),
            slot("swimming", 4),
        ),
        isMixed = true,
    )

    val postureStrength = Workout(
        id = "posture-strength", title = "Posture Strength",
        summary = "Rows, back extension and stability for a strong upper back.",
        focus = listOf(BodyArea.BACK), level = ExerciseLevel.INTERMEDIATE,
        slots = listOf(
            fallback("seated-band-row", 12, listOf("swimming")),
            slot("swan-prep", 8),
            slot("swimming", 4),
            slot("bird-dog", 6),
            slot("spine-stretch-forward", 5),
        ),
        isMixed = true,
    )

    // Aggregates (declared after every workout they list).

    val moreWorkouts: List<Workout> = listOf(
        matBasics, posturePrep, stretchReset, plankPower, coreSculpt, classicalFlow,
        bandBasics, bandStrength,
        ringToneUp, ringSculpt,
        ballBasics, ballBurn,
        weightsSculpt, armsAndAbs,
        wallStarter, wallStrength,
        reformerIntro, reformerStrength, reformerBurn,
        homeStudioMix, totalBodyMix,
    )

    /** Body-area workouts. Every equipment move has a mat fallback, so they suit everyone. */
    val focusWorkouts: List<Workout> = listOf(
        upperBodyBasics, upperBodySculpt,
        coreBasics, coreStrength,
        lowerBodyBasics, lowerBodyBurn,
        gluteBasics, gluteSculpt,
        postureStrength,
    )

    val all: List<Workout> by lazy {
        listOf(
            coreWakeUp, bridgeBuilder, backCare, gentleStart,
            rollingCore, hundredAndBridge, fullBodyFlow, advancedCoreBurn,
            reformerFoundations, reformerFlow,
        ) + moreWorkouts + focusWorkouts
    }

    val reformerWorkouts: List<Workout> get() = all.filter { it.requires == Equipment.REFORMER }

    fun workout(id: String): Workout? = all.firstOrNull { it.id == id }

    /** Workouts at or below a level that the user has the equipment for, in library order. */
    fun workouts(upTo: ExerciseLevel, equipment: Set<Equipment> = setOf(Equipment.MAT)): List<Workout> =
        all.filter { it.level.rank <= upTo.rank && it.isAvailable(equipment) }
}
