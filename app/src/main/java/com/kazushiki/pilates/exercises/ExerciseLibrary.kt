package com.kazushiki.pilates.exercises

import com.kazushiki.pilates.figure.ArmPose
import com.kazushiki.pilates.figure.FigurePose
import com.kazushiki.pilates.figure.FigureProp
import com.kazushiki.pilates.figure.LegPose
import com.kazushiki.pilates.figure.PoseRoot
import com.kazushiki.pilates.figure.Pt
import com.kazushiki.pilates.figure.ReformerGeometry
import com.kazushiki.pilates.figure.ReformerStraps

/**
 * All bundled exercises. To add one: define its poses and a sequence of steps.
 * Every move must be reviewed by a certified Pilates instructor before launch.
 *
 * Initialization order matters: every val is declared before anything that reads it,
 * and the aggregate lists ([moreExercises], [all]) are lazy.
 */
object ExerciseLibrary {
    /** Shoulders and head rest on the mat at this height. */
    const val lyingY = 269.0

    // Shared poses

    /** A lying-on-the-back pose anchored at the pelvis, knees bent and feet on the mat. */
    fun supine(
        at: Number = 268,
        y: Number = lyingY,
        lumbar: Number = 180,
        thoracic: Number = 180,
        neck: Number = 172,
        arm: ArmPose = ArmPose(0, 0),
        leg: LegPose = LegPose.Angles(52, -57.8, 0),
        farArm: ArmPose? = null,
        farLeg: LegPose? = null,
    ): FigurePose = FigurePose(
        root = PoseRoot.PELVIS,
        rootPoint = Pt(at.toDouble(), y.toDouble()),
        lumbar = lumbar.toDouble(),
        thoracic = thoracic.toDouble(),
        neck = neck.toDouble(),
        arm = arm,
        leg = leg,
        farArm = farArm,
        farLeg = farLeg,
    )

    val tabletop: LegPose = LegPose.Angles(90, 0, -10)
    val armsToCeiling = ArmPose(90, 90)

    // Bridges

    /** Bridge keyframes pivoting at the shoulders. [feet] plants the working foot. */
    fun bridgePoses(
        feet: LegPose,
        nearLegAngles: Map<String, Number>? = null,
        nearKneeBend: Double = 0.0,
        shoulder: Pt = Pt(150.0, lyingY),
    ): Map<String, FigurePose> {
        val spine: List<Triple<String, Double, Double>> = listOf(
            Triple("down", 180.0, 180.0),
            Triple("peelUp", 214.0, 190.0),
            Triple("up", 202.0, 202.0),
            Triple("peelDown", 200.0, 186.0),
        )
        val poses = LinkedHashMap<String, FigurePose>()
        for ((name, lumbar, thoracic) in spine) {
            var pose = FigurePose(
                root = PoseRoot.SHOULDER, rootPoint = shoulder,
                lumbar = lumbar, thoracic = thoracic, neck = 172.0,
                arm = ArmPose(0, 0), leg = feet,
            )
            val lift = nearLegAngles?.get(name)
            if (lift != null) {
                // Single-leg: the near leg reaches up (knee bent by nearKneeBend); the far foot works.
                val l = lift.toDouble()
                val shin = l - nearKneeBend
                pose = pose.copy(farLeg = feet, leg = LegPose.Angles(l, shin, shin))
            }
            poses[name] = pose
        }
        return poses
    }

    fun bridgeSequence(setup: String, lift: String): List<ExerciseStep> {
        val lower = "Inhale, then roll down one vertebra at a time, upper back first."
        return listOf(
            ExerciseStep(pose = "down", duration = 0.9, phase = "Set up", cue = setup),
            ExerciseStep(pose = "peelUp", duration = 0.9, phase = "Lift", cue = lift),
            ExerciseStep(pose = "up", duration = 0.9, phase = "Lift", cue = lift),
            ExerciseStep(pose = "up", duration = 1.4, phase = "Hold", cue = "Squeeze your glutes. Weight stays on your shoulders, not your neck."),
            ExerciseStep(pose = "peelDown", duration = 1.0, phase = "Lower", cue = lower),
            ExerciseStep(pose = "down", duration = 1.0, phase = "Lower", cue = lower),
        )
    }

    val feetOnMat: LegPose = LegPose.Planted(Pt(350, 272), 0)

    val gluteBridge = Exercise(
        id = "glute-bridge",
        name = "Glute Bridge",
        summary = "Strengthens the glutes and hamstrings while teaching the spine to move one segment at a time.",
        equipment = listOf(Equipment.MAT),
        bodyAreas = listOf(BodyArea.GLUTES, BodyArea.CORE, BodyArea.BACK),
        variations = listOf(
            ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER, poses = bridgePoses(feet = feetOnMat)),
            ExerciseVariation(
                id = "single", title = "Single leg", level = ExerciseLevel.ADVANCED,
                poses = bridgePoses(feet = feetOnMat, nearLegAngles = mapOf("down" to 50, "peelUp" to 38, "up" to 22, "peelDown" to 30)),
            ),
        ),
        sequence = bridgeSequence(
            setup = "Feet hip-width apart, heels under your knees, arms long by your sides.",
            lift = "Exhale: tilt your pelvis and peel your spine up off the mat.",
        ),
        counter = ExerciseCounter.Reps(10),
    )

    val ballBridge = Exercise(
        id = "ball-bridge",
        name = "Ball Squeeze Bridge",
        summary = "A bridge with a small ball between the knees to switch on the inner thighs.",
        equipment = listOf(Equipment.BALL),
        bodyAreas = listOf(BodyArea.GLUTES, BodyArea.LEGS),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER, poses = bridgePoses(feet = feetOnMat))),
        sequence = bridgeSequence(
            setup = "Place the ball between your knees. Feet hip-width apart, arms long.",
            lift = "Exhale: squeeze the ball and peel your spine up off the mat.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Ball),
    )

    val ringBridge = Exercise(
        id = "ring-bridge",
        name = "Ring Squeeze Bridge",
        summary = "A bridge squeezing a Pilates ring between the knees for glutes and inner thighs.",
        equipment = listOf(Equipment.RING),
        bodyAreas = listOf(BodyArea.GLUTES, BodyArea.LEGS),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER, poses = bridgePoses(feet = feetOnMat))),
        sequence = bridgeSequence(
            setup = "Hold the ring between your knees, pads on the inside of each knee.",
            lift = "Exhale: press into the ring and peel your spine up off the mat.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Ring),
    )

    val wallBridge = Exercise(
        id = "wall-bridge",
        name = "Wall Bridge",
        summary = "Feet up on a wall shifts the work into the hamstrings and takes pressure off the lower back.",
        equipment = listOf(Equipment.WALL),
        bodyAreas = listOf(BodyArea.GLUTES, BodyArea.LEGS),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = bridgePoses(feet = LegPose.Planted(Pt(362, 212), 90)),
            ),
        ),
        sequence = bridgeSequence(
            setup = "Lie with your hips near the wall, feet flat on it, knees bent about 90 degrees.",
            lift = "Exhale: press your feet into the wall and peel your spine up.",
        ),
        counter = ExerciseCounter.Reps(8),
        props = listOf(FigureProp.Wall(372)),
    )

    // The Hundred

    val hundred: Exercise = run {
        fun variation(id: String, title: String, level: ExerciseLevel, legs: LegPose): ExerciseVariation =
            ExerciseVariation(
                id = id, title = title, level = level,
                poses = mapOf(
                    "flat" to supine(),
                    "ready" to supine(thoracic = 150, neck = 132, arm = ArmPose(-8, -8), leg = legs),
                ),
            )

        Exercise(
            id = "the-hundred",
            name = "The Hundred",
            summary = "A classic Pilates warm-up: a hundred small arm pumps while holding a curl, paced by the breath.",
            equipment = listOf(Equipment.MAT),
            bodyAreas = listOf(BodyArea.CORE),
            variations = listOf(
                variation(id = "tabletop", title = "Beginner", level = ExerciseLevel.BEGINNER, legs = tabletop),
                variation(id = "extended", title = "Advanced", level = ExerciseLevel.ADVANCED, legs = LegPose.Angles(40, 40, 30)),
            ),
            sequence = listOf(
                ExerciseStep(pose = "flat", duration = 1.2, phase = "Set up", cue = "Lie on your back, knees bent, arms long by your sides."),
                ExerciseStep(pose = "ready", duration = 2.0, phase = "Curl up", cue = "Exhale: nod your chin, curl your head and shoulders up, and lift your legs."),
                ExerciseStep(pose = "ready", duration = 50.0, phase = "Pump", cue = "Pump your arms small and fast, ribs down, eyes on your thighs.", isPump = true),
                ExerciseStep(pose = "flat", duration = 2.0, phase = "Lower", cue = "Lower your head and feet with control."),
                ExerciseStep(pose = "flat", duration = 1.5, phase = "Rest", cue = "Rest and breathe."),
            ),
            counter = ExerciseCounter.HundredCount,
        )
    }

    // Dead Bug

    private val deadBugPoses: Map<String, FigurePose> = run {
        val reachArm = ArmPose(155, 160)
        val longLeg = LegPose.Angles(18, 14, 10)
        mapOf(
            "ready" to supine(arm = armsToCeiling, leg = tabletop),
            "extendA" to supine(arm = reachArm, leg = tabletop, farArm = armsToCeiling, farLeg = longLeg),
            "extendB" to supine(arm = armsToCeiling, leg = longLeg, farArm = reachArm, farLeg = tabletop),
        )
    }

    private val deadBugSequence: List<ExerciseStep> = run {
        val reach = "Exhale: lower one arm overhead and the opposite leg long, hovering just above the mat."
        // Same text as the iPhone app.
        val back = "Inhale: bring them back to tabletop without letting your back arch."
        listOf(
            ExerciseStep(pose = "ready", duration = 1.0, phase = "Set up", cue = "Arms reach to the ceiling, knees over hips. Press your lower back into the mat."),
            ExerciseStep(pose = "extendA", duration = 1.4, phase = "Reach", cue = reach),
            ExerciseStep(pose = "extendA", duration = 0.6, phase = "Reach", cue = reach),
            ExerciseStep(pose = "ready", duration = 1.2, phase = "Return", cue = back),
            ExerciseStep(pose = "extendB", duration = 1.4, phase = "Switch", cue = "Exhale: switch sides, the other arm and the opposite leg."),
            ExerciseStep(pose = "extendB", duration = 0.6, phase = "Switch", cue = "Exhale: switch sides, the other arm and the opposite leg."),
            ExerciseStep(pose = "ready", duration = 1.2, phase = "Return", cue = back),
        )
    }

    val deadBug = Exercise(
        id = "dead-bug",
        name = "Dead Bug",
        summary = "Opposite arm and leg reach away while the lower back stays anchored. Deep core control.",
        equipment = listOf(Equipment.MAT),
        bodyAreas = listOf(BodyArea.CORE),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER, poses = deadBugPoses)),
        sequence = deadBugSequence,
        counter = ExerciseCounter.Reps(6),
    )

    val weightedDeadBug = Exercise(
        id = "weighted-dead-bug",
        name = "Weighted Dead Bug",
        summary = "The dead bug with light weights in the hands for extra core and shoulder work.",
        equipment = listOf(Equipment.WEIGHTS),
        bodyAreas = listOf(BodyArea.CORE, BodyArea.ARMS),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE, poses = deadBugPoses)),
        sequence = deadBugSequence,
        counter = ExerciseCounter.Reps(6),
        props = listOf(FigureProp.Weights),
    )

    // Single-Leg Stretch

    val singleLegStretch: Exercise = run {
        val curlThoracic = 150.0
        val curlNeck = 132.0
        val handsOnShin = ArmPose(22, 22)
        val kneeIn = LegPose.Angles(115, -5, -10)
        val legLong = LegPose.Angles(32, 32, 28)
        val poses = mapOf(
            "flat" to supine(),
            "legA" to supine(thoracic = curlThoracic, neck = curlNeck, arm = handsOnShin, leg = kneeIn, farLeg = legLong),
            "legB" to supine(thoracic = curlThoracic, neck = curlNeck, arm = handsOnShin, leg = legLong, farLeg = kneeIn),
        )
        val switchCue = "Switch legs: pull the other knee in and reach the extended leg long."
        val sequence = mutableListOf(
            ExerciseStep(pose = "flat", duration = 1.2, phase = "Set up", cue = "Lie on your back, knees bent."),
            ExerciseStep(pose = "legA", duration = 1.8, phase = "Curl up", cue = "Exhale: curl your head and shoulders up. Hug one knee in and reach the other leg long."),
        )
        for (index in 0 until 8) {
            sequence.add(ExerciseStep(pose = if (index % 2 == 0) "legB" else "legA", duration = 0.9, phase = "Switch", cue = switchCue))
        }
        sequence.add(ExerciseStep(pose = "flat", duration = 1.6, phase = "Lower", cue = "Lower your head and feet and rest."))

        Exercise(
            id = "single-leg-stretch",
            name = "Single-Leg Stretch",
            summary = "Alternate hugging one knee in while the other leg reaches long, holding a steady curl.",
            equipment = listOf(Equipment.MAT),
            bodyAreas = listOf(BodyArea.CORE),
            variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE, poses = poses)),
            sequence = sequence.toList(),
            counter = ExerciseCounter.Reps(3),
        )
    }

    // Roll Up

    private fun rollUpPoses(armsOverhead: Boolean): Map<String, FigurePose> {
        val legs = LegPose.Angles(0, 0, 70)
        val startArms = if (armsOverhead) ArmPose(180, 180) else ArmPose(70, 60)
        return mapOf(
            "lying" to supine(at = 262, arm = startArms, leg = legs),
            "armsUp" to supine(at = 262, arm = if (armsOverhead) armsToCeiling else ArmPose(60, 50), leg = legs),
            "curl" to supine(at = 262, lumbar = 170, thoracic = 140, neck = 122, arm = ArmPose(28, 22), leg = legs),
            "up" to supine(at = 262, lumbar = 72, thoracic = 36, neck = -8, arm = ArmPose(4, 0), leg = legs),
        )
    }

    private fun rollUpSequence(setup: String): List<ExerciseStep> {
        val rollUp = "Exhale: nod your chin and peel up one vertebra at a time."
        val rollDown = "Inhale, then exhale to roll back down slowly, lower back first."
        return listOf(
            ExerciseStep(pose = "lying", duration = 0.8, phase = "Set up", cue = setup),
            ExerciseStep(pose = "armsUp", duration = 1.2, phase = "Inhale", cue = "Inhale: float your arms up."),
            ExerciseStep(pose = "curl", duration = 1.4, phase = "Roll up", cue = rollUp),
            ExerciseStep(pose = "up", duration = 1.4, phase = "Roll up", cue = rollUp),
            ExerciseStep(pose = "up", duration = 0.8, phase = "Reach", cue = "Reach past your toes, belly scooped away from your thighs."),
            ExerciseStep(pose = "curl", duration = 1.6, phase = "Roll down", cue = rollDown),
            ExerciseStep(pose = "lying", duration = 1.4, phase = "Roll down", cue = rollDown),
        )
    }

    val rollUp = Exercise(
        id = "roll-up",
        name = "Roll Up",
        summary = "Peel up from lying to a forward reach and back down, one vertebra at a time.",
        equipment = listOf(Equipment.MAT),
        bodyAreas = listOf(BodyArea.CORE, BodyArea.BACK),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE, poses = rollUpPoses(armsOverhead = true))),
        sequence = rollUpSequence(setup = "Lie long, legs together, feet flexed, arms reaching overhead."),
        counter = ExerciseCounter.Reps(6),
    )

    val bandedRollUp = Exercise(
        id = "banded-roll-up",
        name = "Banded Roll Up",
        summary = "A band around the feet gives support, so you can roll up and down with control.",
        equipment = listOf(Equipment.BAND),
        bodyAreas = listOf(BodyArea.CORE, BodyArea.BACK),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER, poses = rollUpPoses(armsOverhead = false))),
        sequence = rollUpSequence(setup = "Loop the band around your feet and hold an end in each hand."),
        counter = ExerciseCounter.Reps(6),
        props = listOf(FigureProp.Band),
    )

    // Swan Prep

    val swanPrep = Exercise(
        id = "swan-prep",
        name = "Swan Prep",
        summary = "A gentle back extension lying face down. Strengthens the upper back for better posture.",
        equipment = listOf(Equipment.MAT),
        bodyAreas = listOf(BodyArea.BACK),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "down" to supine(at = 270, neck = 180, leg = LegPose.Angles(0, 0, -5)),
                    "up" to supine(at = 270, lumbar = 173, thoracic = 152, neck = 160, arm = ArmPose(8, 6), leg = LegPose.Angles(0, 0, -5)),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "down", duration = 0.8, phase = "Set up", cue = "Lie face down, legs long, arms by your sides, forehead hovering over the mat."),
            ExerciseStep(pose = "up", duration = 1.6, phase = "Lift", cue = "Exhale: lengthen through the crown of your head and lift your chest."),
            ExerciseStep(pose = "up", duration = 1.0, phase = "Hold", cue = "Slide your shoulders down your back. Keep your neck long."),
            ExerciseStep(pose = "down", duration = 1.8, phase = "Lower", cue = "Inhale and lower with control."),
        ),
        counter = ExerciseCounter.Reps(8),
    )

    // Bird Dog

    val birdDog: Exercise = run {
        fun birdDogAllFours(arm: ArmPose, leg: LegPose, farArm: ArmPose? = null, farLeg: LegPose? = null): FigurePose =
            FigurePose(
                root = PoseRoot.PELVIS, rootPoint = Pt(290, 192),
                lumbar = 166.0, thoracic = 166.0, neck = 176.0,
                arm = arm, leg = leg, farArm = farArm, farLeg = farLeg,
            )
        val handDown = ArmPose(-90, -90)
        val kneeDown = LegPose.Angles(-90, 0, 0)
        // -188° is the same direction as 172°, but blends from hands-down by lifting the arm forward
        // instead of swinging it back over the head.
        val armReach = ArmPose(-188, -188)
        val legReach = LegPose.Angles(4, 4, -4)
        val hold = "Hold. Belly lifted, hips square to the mat."

        Exercise(
            id = "bird-dog",
            name = "Bird Dog",
            summary = "On hands and knees, reach the opposite arm and leg long. Core stability and a strong back.",
            equipment = listOf(Equipment.MAT),
            bodyAreas = listOf(BodyArea.CORE, BodyArea.BACK),
            variations = listOf(
                ExerciseVariation(
                    id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                    poses = mapOf(
                        "ready" to birdDogAllFours(arm = handDown, leg = kneeDown),
                        "reachA" to birdDogAllFours(arm = armReach, leg = kneeDown, farArm = handDown, farLeg = legReach),
                        "reachB" to birdDogAllFours(arm = handDown, leg = legReach, farArm = armReach, farLeg = kneeDown),
                    ),
                ),
            ),
            sequence = listOf(
                ExerciseStep(pose = "ready", duration = 1.0, phase = "Set up", cue = "Hands under shoulders, knees under hips, spine long."),
                ExerciseStep(pose = "reachA", duration = 1.4, phase = "Reach", cue = "Exhale: reach one arm forward and the opposite leg back."),
                ExerciseStep(pose = "reachA", duration = 0.8, phase = "Reach", cue = hold),
                ExerciseStep(pose = "ready", duration = 1.2, phase = "Return", cue = "Inhale: return to all fours with control."),
                ExerciseStep(pose = "reachB", duration = 1.4, phase = "Switch", cue = "Exhale: switch sides."),
                ExerciseStep(pose = "reachB", duration = 0.8, phase = "Switch", cue = hold),
                ExerciseStep(pose = "ready", duration = 1.2, phase = "Return", cue = "Inhale: return to all fours with control."),
            ),
            counter = ExerciseCounter.Reps(6),
        )
    }

    // Reformer

    const val reformerY = ReformerGeometry.lyingY
    val heelsOnBar: LegPose = LegPose.Planted(ReformerGeometry.footbarHeel, 80)

    val reformerFootwork = Exercise(
        id = "reformer-footwork",
        name = "Reformer Footwork",
        summary = "The classic reformer warm-up: press the carriage out and back with your heels on the footbar.",
        equipment = listOf(Equipment.REFORMER),
        bodyAreas = listOf(BodyArea.LEGS, BodyArea.GLUTES),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Heels", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "in" to supine(at = 310, y = reformerY, leg = heelsOnBar),
                    "out" to supine(at = 250, y = reformerY, leg = heelsOnBar),
                ),
            ),
            ExerciseVariation(
                id = "toes", title = "Toes", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "in" to supine(at = 310, y = reformerY, leg = LegPose.Planted(ReformerGeometry.footbarHeel, 55)),
                    "out" to supine(at = 250, y = reformerY, leg = LegPose.Planted(ReformerGeometry.footbarHeel, 55)),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "in", duration = 1.0, phase = "Set up", cue = "Lie on the carriage, shoulders against the blocks, heels on the footbar."),
            ExerciseStep(pose = "out", duration = 1.4, phase = "Press", cue = "Exhale: press the carriage out until your legs are long."),
            ExerciseStep(pose = "out", duration = 0.4, phase = "Press", cue = "Long legs, but don't lock your knees."),
            ExerciseStep(pose = "in", duration = 1.6, phase = "Return", cue = "Inhale: bring the carriage home slowly. Don't let it slam."),
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Reformer(ReformerStraps.NONE)),
    )

    val reformerHundred: Exercise = run {
        // Feet resting on the footbar, knees bent (solved to match the footbar position).
        val feetOnBar = LegPose.Angles(62.1, -31.4, 80)
        fun variation(id: String, title: String, level: ExerciseLevel, legs: LegPose): ExerciseVariation =
            ExerciseVariation(
                id = id, title = title, level = level,
                poses = mapOf(
                    "flat" to supine(at = 300, y = reformerY, leg = feetOnBar),
                    "ready" to supine(at = 300, y = reformerY, thoracic = 150, neck = 132, arm = ArmPose(-8, -8), leg = legs),
                ),
            )

        Exercise(
            id = "reformer-hundred",
            name = "Reformer Hundred",
            summary = "The Hundred holding the straps, so your arms work against the springs.",
            equipment = listOf(Equipment.REFORMER),
            bodyAreas = listOf(BodyArea.CORE, BodyArea.ARMS),
            variations = listOf(
                variation(id = "tabletop", title = "Beginner", level = ExerciseLevel.BEGINNER, legs = tabletop),
                variation(id = "extended", title = "Advanced", level = ExerciseLevel.ADVANCED, legs = LegPose.Angles(30, 30, 25)),
            ),
            sequence = listOf(
                ExerciseStep(pose = "flat", duration = 1.2, phase = "Set up", cue = "Hold a strap in each hand, feet on the footbar, arms reaching up."),
                ExerciseStep(pose = "ready", duration = 2.0, phase = "Curl up", cue = "Exhale: curl up, lift your legs, and pull the straps down by your hips."),
                ExerciseStep(pose = "ready", duration = 50.0, phase = "Pump", cue = "Pump your arms against the straps, ribs down, eyes on your thighs.", isPump = true),
                ExerciseStep(pose = "flat", duration = 2.0, phase = "Lower", cue = "Lower your head and feet with control."),
                ExerciseStep(pose = "flat", duration = 1.5, phase = "Rest", cue = "Rest and breathe."),
            ),
            counter = ExerciseCounter.HundredCount,
            props = listOf(FigureProp.Reformer(ReformerStraps.HANDS)),
        )
    }

    val reformerFrog = Exercise(
        id = "reformer-frog",
        name = "Frog in Straps",
        summary = "Feet in the straps, press the legs out on a diagonal. Inner thighs, hamstrings and a steady pelvis.",
        equipment = listOf(Equipment.REFORMER),
        bodyAreas = listOf(BodyArea.LEGS, BodyArea.CORE),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "frog" to supine(at = 300, y = reformerY, leg = LegPose.Angles(105, -20, -10)),
                    "press" to supine(at = 300, y = reformerY, leg = LegPose.Angles(35, 35, 35)),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "frog", duration = 1.0, phase = "Set up", cue = "Feet in the straps, heels together, knees open wide."),
            ExerciseStep(pose = "press", duration = 1.4, phase = "Press", cue = "Exhale: press your legs out long on a diagonal."),
            ExerciseStep(pose = "press", duration = 0.4, phase = "Press", cue = "Keep your pelvis heavy and still."),
            ExerciseStep(pose = "frog", duration = 1.6, phase = "Return", cue = "Inhale: bend back to frog with control."),
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Reformer(ReformerStraps.FEET)),
    )

    val reformerBridge = Exercise(
        id = "reformer-bridge",
        name = "Reformer Bridge",
        summary = "A bridge with feet on the footbar. Keeping the carriage still makes the glutes and hamstrings work harder.",
        equipment = listOf(Equipment.REFORMER),
        bodyAreas = listOf(BodyArea.GLUTES, BodyArea.LEGS, BodyArea.BACK),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE,
                poses = bridgePoses(feet = heelsOnBar, shoulder = Pt(200.0, reformerY)),
            ),
        ),
        sequence = bridgeSequence(
            setup = "Heels on the footbar, shoulders against the blocks, arms long.",
            lift = "Exhale: peel your spine up while keeping the carriage still.",
        ),
        counter = ExerciseCounter.Reps(8),
        props = listOf(FigureProp.Reformer(ReformerStraps.NONE)),
    )

    // ------------------------------------------------------------------------------------
    // More exercises: extra mat classics plus moves for each piece of small equipment and
    // the reformer (ExerciseLibrary+More.swift on iPhone).
    // ------------------------------------------------------------------------------------

    // Shared shapes

    /** Sitting on the mat, anchored at the pelvis. */
    fun seated(
        lumbar: Number = 85,
        thoracic: Number = 88,
        neck: Number = 88,
        arm: ArmPose,
        leg: LegPose = LegPose.Angles(40, -40, 0),
    ): FigurePose = FigurePose(
        root = PoseRoot.PELVIS, rootPoint = Pt(230.0, lyingY),
        lumbar = lumbar.toDouble(), thoracic = thoracic.toDouble(), neck = neck.toDouble(), arm = arm, leg = leg,
    )

    val legsLongSeated: LegPose = LegPose.Angles(0, 0, 70)

    fun allFours(lumbar: Number = 166, thoracic: Number = 166, neck: Number = 176): FigurePose = FigurePose(
        root = PoseRoot.PELVIS, rootPoint = Pt(290, 192),
        lumbar = lumbar.toDouble(), thoracic = thoracic.toDouble(), neck = neck.toDouble(),
        arm = ArmPose(-90, -90), leg = LegPose.Angles(-90, 0, 0),
    )

    /** A lift-and-lower move: set up, lift, a short hold, lower. */
    fun liftSequence(
        start: String, end: String, setup: String, lift: String, hold: String, lower: String,
        liftTime: Double = 1.4, lowerTime: Double = 1.6,
    ): List<ExerciseStep> = listOf(
        ExerciseStep(pose = start, duration = 1.0, phase = "Set up", cue = setup),
        ExerciseStep(pose = end, duration = liftTime, phase = "Lift", cue = lift),
        ExerciseStep(pose = end, duration = 0.5, phase = "Hold", cue = hold),
        ExerciseStep(pose = start, duration = lowerTime, phase = "Lower", cue = lower),
    )

    // Mat

    val plankLegLift: Exercise = run {
        val base = FigurePose(
            root = PoseRoot.PELVIS, rootPoint = Pt(260, 207.2),
            lumbar = 158.0, thoracic = 158.0, neck = 160.0,
            arm = ArmPose(-90, -90), leg = LegPose.Angles(-22, -22, -80),
        )
        val lifted = LegPose.Angles(-8, -8, -60)
        val liftA = base.copy(leg = lifted, farLeg = base.leg)
        val liftB = base.copy(farLeg = lifted)
        val lower = "Lower it with control."
        Exercise(
            id = "plank-leg-lift", name = "Plank Leg Lift",
            summary = "Hold a long plank and lift one leg at a time without letting the hips move.",
            equipment = listOf(Equipment.MAT), bodyAreas = listOf(BodyArea.CORE, BodyArea.GLUTES, BodyArea.ARMS),
            variations = listOf(
                ExerciseVariation(
                    id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE,
                    poses = mapOf("plank" to base, "liftA" to liftA, "liftB" to liftB),
                ),
            ),
            sequence = listOf(
                ExerciseStep(pose = "plank", duration = 1.0, phase = "Set up", cue = "Hands under shoulders, body in one long line from head to heels."),
                ExerciseStep(pose = "liftA", duration = 1.2, phase = "Lift", cue = "Exhale: lift one leg without letting your hips move."),
                ExerciseStep(pose = "plank", duration = 1.0, phase = "Lower", cue = lower),
                ExerciseStep(pose = "liftB", duration = 1.2, phase = "Lift", cue = "Exhale: now the other leg."),
                ExerciseStep(pose = "plank", duration = 1.0, phase = "Lower", cue = lower),
            ),
            counter = ExerciseCounter.Reps(6),
        )
    }

    val pilatesPushUp = Exercise(
        id = "pilates-push-up", name = "Pilates Push-Up",
        summary = "A narrow push-up with elbows hugging the ribs. Arms, chest and a strong center.",
        equipment = listOf(Equipment.MAT), bodyAreas = listOf(BodyArea.ARMS, BodyArea.CORE),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE,
                poses = mapOf(
                    "plank" to FigurePose(
                        root = PoseRoot.PELVIS, rootPoint = Pt(260, 207.2), lumbar = 158.0, thoracic = 158.0, neck = 160.0,
                        arm = ArmPose(-90, -90), leg = LegPose.Angles(-22, -22, -80),
                    ),
                    "low" to FigurePose(
                        root = PoseRoot.PELVIS, rootPoint = Pt(250.9, 239), lumbar = 170.0, thoracic = 170.0, neck = 170.0,
                        arm = ArmPose(-18.9, -136.2), leg = LegPose.Angles(-10, -10, -80),
                    ),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "plank", duration = 1.0, phase = "Set up", cue = "Plank with hands under shoulders. Lower to your knees if you need to."),
            ExerciseStep(pose = "low", duration = 1.6, phase = "Lower", cue = "Inhale: bend your elbows back along your ribs and lower in one piece."),
            ExerciseStep(pose = "plank", duration = 1.4, phase = "Press", cue = "Exhale: press the floor away."),
        ),
        counter = ExerciseCounter.Reps(6),
    )

    val swimming: Exercise = run {
        val legsLong = LegPose.Angles(0, 0, -5)
        val legUp = LegPose.Angles(8, 8, 5)
        val reach = ArmPose(180, 180)
        val armUp = ArmPose(166, 166)
        val swimCue = "Lift your chest, then flutter opposite arm and leg."
        val steps = mutableListOf(ExerciseStep(pose = "base", duration = 1.0, phase = "Set up", cue = "Lie face down, arms long overhead, legs long."))
        for (index in 0 until 6) {
            steps.add(ExerciseStep(pose = if (index % 2 == 0) "swimA" else "swimB", duration = 0.7, phase = "Swim", cue = swimCue))
        }
        steps.add(ExerciseStep(pose = "base", duration = 1.2, phase = "Rest", cue = "Lower and rest your forehead."))
        Exercise(
            id = "swimming", name = "Swimming",
            summary = "Face down, flutter opposite arm and leg. Strengthens the back and backs of the legs.",
            equipment = listOf(Equipment.MAT), bodyAreas = listOf(BodyArea.BACK, BodyArea.GLUTES),
            variations = listOf(
                ExerciseVariation(
                    id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                    poses = mapOf(
                        "base" to supine(at = 270, neck = 175, arm = reach, leg = legsLong),
                        "swimA" to supine(at = 270, thoracic = 168, neck = 165, arm = armUp, leg = legsLong, farArm = reach, farLeg = legUp),
                        "swimB" to supine(at = 270, thoracic = 168, neck = 165, arm = reach, leg = legUp, farArm = armUp, farLeg = legsLong),
                    ),
                ),
            ),
            sequence = steps.toList(),
            counter = ExerciseCounter.Reps(4),
        )
    }

    val teaser = Exercise(
        id = "teaser", name = "Teaser",
        summary = "Roll up into a V-balance with arms and legs reaching. The signature Pilates core move.",
        equipment = listOf(Equipment.MAT), bodyAreas = listOf(BodyArea.CORE),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE,
                poses = mapOf(
                    "prep" to supine(at = 260, arm = ArmPose(180, 180)),
                    "up" to supine(at = 260, lumbar = 125, thoracic = 120, neck = 110, arm = ArmPose(32, 32), leg = LegPose.Angles(50, 50, 45)),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "prep", duration = 1.0, phase = "Set up", cue = "Lie on your back, knees bent, arms reaching overhead."),
            ExerciseStep(pose = "up", duration = 2.0, phase = "Roll up", cue = "Exhale: float your arms forward and roll up into a V, legs lifted."),
            ExerciseStep(pose = "up", duration = 1.2, phase = "Balance", cue = "Balance just behind your sit bones, chest open."),
            ExerciseStep(pose = "prep", duration = 2.0, phase = "Roll down", cue = "Inhale, then roll down one vertebra at a time."),
        ),
        counter = ExerciseCounter.Reps(5),
    )

    val spineStretchForward = Exercise(
        id = "spine-stretch-forward", name = "Spine Stretch Forward",
        summary = "Sit tall, then round forward over long legs. Stretches the back and hamstrings.",
        equipment = listOf(Equipment.MAT), bodyAreas = listOf(BodyArea.BACK),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "tall" to seated(arm = ArmPose(0, 0), leg = legsLongSeated),
                    "fold" to seated(lumbar = 60, thoracic = 22, neck = -30, arm = ArmPose(-4, -6), leg = legsLongSeated),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "tall", duration = 1.0, phase = "Set up", cue = "Sit tall, legs long and hip-width apart, arms reaching forward."),
            ExerciseStep(pose = "fold", duration = 2.0, phase = "Round", cue = "Exhale: round forward, reaching past your toes."),
            ExerciseStep(pose = "fold", duration = 0.8, phase = "Round", cue = "Scoop your belly away from your thighs."),
            ExerciseStep(pose = "tall", duration = 2.0, phase = "Stack up", cue = "Inhale: stack back up to sitting tall."),
        ),
        counter = ExerciseCounter.Reps(5),
    )

    val catCow = Exercise(
        id = "cat-cow", name = "Cat-Cow",
        summary = "On hands and knees, round and arch the spine with the breath. A gentle warm-up for the back.",
        equipment = listOf(Equipment.MAT), bodyAreas = listOf(BodyArea.BACK),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "table" to allFours(),
                    "cat" to allFours(lumbar = 150, thoracic = 186, neck = 225),
                    "cow" to allFours(lumbar = 178, thoracic = 150, neck = 150),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "table", duration = 1.0, phase = "Set up", cue = "Hands under shoulders, knees under hips."),
            ExerciseStep(pose = "cat", duration = 1.6, phase = "Cat", cue = "Exhale: round your spine to the ceiling and let your head drop."),
            ExerciseStep(pose = "cow", duration = 2.0, phase = "Cow", cue = "Inhale: lift your chest and tailbone, belly soft."),
            ExerciseStep(pose = "table", duration = 1.0, phase = "Reset", cue = "Return to a long, neutral spine."),
        ),
        counter = ExerciseCounter.Reps(6),
    )

    // Band

    val seatedBandRow = Exercise(
        id = "seated-band-row", name = "Seated Band Row",
        summary = "Sit tall with the band around your feet and row the elbows back. Upper back and posture.",
        equipment = listOf(Equipment.BAND), bodyAreas = listOf(BodyArea.BACK, BodyArea.ARMS),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "reach" to seated(lumbar = 88, thoracic = 92, arm = ArmPose(-2, 0), leg = legsLongSeated),
                    "row" to seated(lumbar = 92, thoracic = 98, neck = 90, arm = ArmPose(-120, 0), leg = legsLongSeated),
                ),
            ),
        ),
        sequence = liftSequence(
            start = "reach", end = "row",
            setup = "Sit tall with the band around your feet, an end in each hand.",
            lift = "Exhale: pull your elbows back past your ribs.",
            hold = "Squeeze between your shoulder blades.",
            lower = "Inhale: reach forward with control.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Band),
    )

    val bandedLegPress = Exercise(
        id = "banded-leg-press", name = "Banded Leg Press",
        summary = "Lying down, press one leg out long against the band. Legs and glutes; switch sides halfway.",
        equipment = listOf(Equipment.BAND), bodyAreas = listOf(BodyArea.LEGS, BodyArea.GLUTES),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "in" to supine(arm = ArmPose(55, 20), leg = LegPose.Angles(112, -10, -10), farLeg = LegPose.Angles(52, -57.8, 0)),
                    "out" to supine(arm = ArmPose(40, 30), leg = LegPose.Angles(58, 58, 50), farLeg = LegPose.Angles(52, -57.8, 0)),
                ),
            ),
        ),
        sequence = listOf(
            ExerciseStep(pose = "in", duration = 1.0, phase = "Set up", cue = "Loop the band around one foot and hold an end in each hand, knee bent in."),
            ExerciseStep(pose = "out", duration = 1.4, phase = "Press", cue = "Exhale: press your leg out long against the band."),
            ExerciseStep(pose = "in", duration = 1.6, phase = "Return", cue = "Inhale: bend the knee back in slowly. Switch legs halfway through."),
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.BandToNearFoot),
    )

    // Ring

    val ringArmPress = Exercise(
        id = "ring-arm-press", name = "Ring Arm Press",
        summary = "Squeeze the ring between your palms, then lift it to eye level. Chest, arms and posture.",
        equipment = listOf(Equipment.RING), bodyAreas = listOf(BodyArea.ARMS),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "front" to seated(arm = ArmPose(0, 2)),
                    "lift" to seated(arm = ArmPose(24, 28)),
                ),
            ),
        ),
        sequence = liftSequence(
            start = "front", end = "lift",
            setup = "Sit tall, holding the ring between your palms at chest height.",
            lift = "Exhale: squeeze the ring and lift it to eye level.",
            hold = "Keep squeezing. Shoulders stay down.",
            lower = "Inhale: lower to chest height, still squeezing.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.RingInHands),
    )

    private val curlPoses: Map<String, FigurePose> = mapOf(
        "flat" to supine(leg = tabletop),
        "curl" to supine(thoracic = 150, neck = 132, arm = ArmPose(-8, -8), leg = tabletop),
    )

    val ringCurlUp = Exercise(
        id = "ring-curl-up", name = "Ring Curl-Up",
        summary = "Squeeze a ring between the knees in tabletop while you curl up. Core and inner thighs.",
        equipment = listOf(Equipment.RING), bodyAreas = listOf(BodyArea.CORE, BodyArea.LEGS),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER, poses = curlPoses)),
        sequence = liftSequence(
            start = "flat", end = "curl",
            setup = "Knees in tabletop with the ring between them.",
            lift = "Exhale: squeeze the ring and curl your head and shoulders up.",
            hold = "Hold the squeeze, eyes on your knees.",
            lower = "Inhale: lower with control.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Ring),
    )

    // Ball

    val ballCurlUp = Exercise(
        id = "ball-curl-up", name = "Ball Curl-Up",
        summary = "A small ball between the knees keeps the inner thighs working while you curl up.",
        equipment = listOf(Equipment.BALL), bodyAreas = listOf(BodyArea.CORE, BodyArea.LEGS),
        variations = listOf(ExerciseVariation(id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER, poses = curlPoses)),
        sequence = liftSequence(
            start = "flat", end = "curl",
            setup = "Knees in tabletop with the ball between them.",
            lift = "Exhale: squeeze the ball and curl up.",
            hold = "Keep the ball still.",
            lower = "Inhale: lower with control.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Ball),
    )

    val ballHundred = Exercise(
        id = "ball-hundred", name = "Ball Hundred",
        summary = "The Hundred in tabletop, squeezing a small ball between the knees.",
        equipment = listOf(Equipment.BALL), bodyAreas = listOf(BodyArea.CORE, BodyArea.LEGS),
        variations = listOf(hundred.variations[0]),
        sequence = hundred.sequence,
        counter = ExerciseCounter.HundredCount,
        props = listOf(FigureProp.Ball),
    )

    // Weights

    val weightedFrontRaise = Exercise(
        id = "weighted-front-raise", name = "Seated Front Raise",
        summary = "Sitting tall, lift light weights to shoulder height. Shoulders and posture.",
        equipment = listOf(Equipment.WEIGHTS), bodyAreas = listOf(BodyArea.ARMS),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "down" to seated(arm = ArmPose(-62, -62)),
                    "raised" to seated(arm = ArmPose(2, 2)),
                ),
            ),
        ),
        sequence = liftSequence(
            start = "down", end = "raised",
            setup = "Sit tall with a weight in each hand, arms down by your knees.",
            lift = "Exhale: lift your arms to shoulder height.",
            hold = "Shoulders stay down, neck long.",
            lower = "Inhale: lower slowly.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Weights),
    )

    val weightedChestPress = Exercise(
        id = "weighted-chest-press", name = "Chest Press",
        summary = "Lying on your back, press light weights to the ceiling. Chest and arms.",
        equipment = listOf(Equipment.WEIGHTS), bodyAreas = listOf(BodyArea.ARMS),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "down" to supine(arm = ArmPose(0, 90)),
                    "up" to supine(arm = ArmPose(90, 90)),
                ),
            ),
        ),
        sequence = liftSequence(
            start = "down", end = "up",
            setup = "Knees bent, elbows on the mat, weights above your elbows.",
            lift = "Exhale: press the weights up to the ceiling.",
            hold = "Ribs soft, back stays still.",
            lower = "Inhale: lower until your elbows touch the mat.",
            liftTime = 1.2,
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Weights),
    )

    // Wall

    private val feetOnWall: LegPose = LegPose.Planted(Pt(362, 212), 90)

    val wallSingleLegBridge = Exercise(
        id = "wall-single-leg-bridge", name = "Wall Single-Leg Bridge",
        summary = "One foot on the wall, the other leg reaching up. A harder bridge for glutes and hamstrings.",
        equipment = listOf(Equipment.WALL), bodyAreas = listOf(BodyArea.GLUTES, BodyArea.LEGS),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE,
                poses = bridgePoses(feet = feetOnWall, nearLegAngles = mapOf("down" to 78, "peelUp" to 78, "up" to 72, "peelDown" to 75), nearKneeBend = 44.0),
            ),
        ),
        sequence = bridgeSequence(
            setup = "One foot on the wall, the other leg reaching to the ceiling.",
            lift = "Exhale: press through the wall foot and peel your spine up.",
        ),
        counter = ExerciseCounter.Reps(8),
        props = listOf(FigureProp.Wall(372)),
    )

    val wallDeadBug: Exercise = run {
        val up = ArmPose(90, 90)
        val reach = ArmPose(155, 160)
        val back = "Inhale: bring it back up."
        Exercise(
            id = "wall-dead-bug", name = "Wall Dead Bug",
            summary = "Feet press into the wall while the arms reach overhead. Deep core without straining the back.",
            equipment = listOf(Equipment.WALL), bodyAreas = listOf(BodyArea.CORE),
            variations = listOf(
                ExerciseVariation(
                    id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                    poses = mapOf(
                        "ready" to supine(at = 262, arm = up, leg = feetOnWall),
                        "reachA" to supine(at = 262, arm = reach, leg = feetOnWall, farArm = up),
                        "reachB" to supine(at = 262, arm = up, leg = feetOnWall, farArm = reach),
                    ),
                ),
            ),
            sequence = listOf(
                ExerciseStep(pose = "ready", duration = 1.0, phase = "Set up", cue = "Feet pressing into the wall, knees bent about 90 degrees, arms up."),
                ExerciseStep(pose = "reachA", duration = 1.4, phase = "Reach", cue = "Exhale: lower one arm overhead while pressing into the wall."),
                ExerciseStep(pose = "ready", duration = 1.2, phase = "Return", cue = back),
                ExerciseStep(pose = "reachB", duration = 1.4, phase = "Switch", cue = "Exhale: now the other arm."),
                ExerciseStep(pose = "ready", duration = 1.2, phase = "Return", cue = back),
            ),
            counter = ExerciseCounter.Reps(6),
            props = listOf(FigureProp.Wall(372)),
        )
    }

    // Reformer (more)

    val reformerArmPulls = Exercise(
        id = "reformer-arm-pulls", name = "Supine Arm Pulls",
        summary = "Lying on the carriage, press the straps from the ceiling down to your hips.",
        equipment = listOf(Equipment.REFORMER), bodyAreas = listOf(BodyArea.ARMS, BodyArea.BACK),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.BEGINNER,
                poses = mapOf(
                    "up" to supine(at = 300, y = reformerY, arm = ArmPose(90, 90), leg = LegPose.Angles(62.1, -31.4, 80)),
                    "down" to supine(at = 300, y = reformerY, arm = ArmPose(0, 0), leg = LegPose.Angles(62.1, -31.4, 80)),
                ),
            ),
        ),
        sequence = liftSequence(
            start = "up", end = "down",
            setup = "Hold the straps with arms reaching to the ceiling, feet on the footbar.",
            lift = "Exhale: press the straps down to your hips.",
            hold = "Ribs stay soft on the carriage.",
            lower = "Inhale: let your arms float back up with control.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Reformer(ReformerStraps.HANDS)),
    )

    val reformerLegLowers = Exercise(
        id = "reformer-leg-lowers", name = "Leg Lowers in Straps",
        summary = "Feet in the straps, lower and lift long legs while the back stays still.",
        equipment = listOf(Equipment.REFORMER), bodyAreas = listOf(BodyArea.CORE, BodyArea.LEGS),
        variations = listOf(
            ExerciseVariation(
                id = "standard", title = "Standard", level = ExerciseLevel.INTERMEDIATE,
                poses = mapOf(
                    "top" to supine(at = 300, y = reformerY, leg = LegPose.Angles(48, 48, 20)),
                    "low" to supine(at = 300, y = reformerY, leg = LegPose.Angles(22, 22, 20)),
                ),
            ),
        ),
        sequence = liftSequence(
            start = "top", end = "low",
            setup = "Feet in the straps, legs long and lifted.",
            lift = "Inhale: lower your legs without letting your back arch.",
            hold = "Pelvis heavy and still.",
            lower = "Exhale: lift back up from your inner thighs.",
        ),
        counter = ExerciseCounter.Reps(10),
        props = listOf(FigureProp.Reformer(ReformerStraps.FEET)),
    )

    // Aggregates (declared last, and lazy, so every exercise above is initialized first).

    val moreExercises: List<Exercise> by lazy {
        listOf(
            plankLegLift, pilatesPushUp, swimming, teaser, spineStretchForward, catCow,
            seatedBandRow, bandedLegPress,
            ringArmPress, ringCurlUp,
            ballCurlUp, ballHundred,
            weightedFrontRaise, weightedChestPress,
            wallSingleLegBridge, wallDeadBug,
            reformerArmPulls, reformerLegLowers,
        )
    }

    val all: List<Exercise> by lazy {
        listOf(
            gluteBridge, hundred, deadBug, singleLegStretch, rollUp, swanPrep, birdDog,
            ballBridge, ringBridge, wallBridge, weightedDeadBug, bandedRollUp,
            reformerFootwork, reformerHundred, reformerFrog, reformerBridge,
        ) + moreExercises
    }

    fun exercise(id: String): Exercise? = all.firstOrNull { it.id == id }
}
