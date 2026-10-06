package com.kazushiki.pilates.figure

import kotlin.math.hypot

// Coordinate system
//
// Poses live in a fixed drawing space (see FigureStage).
// Angles are in degrees: 0° points right, 90° points up, 180° points left.
// y grows downward, like the screen.

/** A point in drawing units. */
data class Pt(val x: Double, val y: Double) {
    constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble())
}

fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
fun lerp(a: Pt, b: Pt, t: Double): Pt = Pt(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
fun distance(p: Pt, q: Pt): Double = hypot(q.x - p.x, q.y - p.y)

/** Segment lengths of the figure, in drawing units. */
object FigureSkeleton {
    const val lumbar = 52.0      // pelvis → mid-back
    const val thoracic = 58.0    // mid-back → shoulders
    const val neck = 38.0        // shoulders → head center
    const val headRadius = 17.0
    const val bunRadius = 7.0
    const val upperArm = 56.0
    const val forearm = 52.0
    const val thigh = 80.0
    const val shin = 78.0
    const val foot = 22.0
}

/** Where the body is anchored for a pose. */
enum class PoseRoot { PELVIS, SHOULDER }

data class ArmPose(
    /** Absolute angle of the upper arm (shoulder → elbow). */
    val upper: Double,
    /** Absolute angle of the forearm (elbow → hand). */
    val fore: Double,
) {
    constructor(upper: Number, fore: Number) : this(upper.toDouble(), fore.toDouble())

    companion object {
        fun interpolate(a: ArmPose, b: ArmPose, t: Double) = ArmPose(lerp(a.upper, b.upper, t), lerp(a.fore, b.fore, t))
    }
}

sealed interface LegPose {
    /** The foot stays planted at [ankle]; the knee is solved automatically. */
    data class Planted(val ankle: Pt, val foot: Double) : LegPose {
        constructor(ankle: Pt, foot: Number) : this(ankle, foot.toDouble())
    }

    /** Thigh, shin and foot at fixed absolute angles. */
    data class Angles(val thigh: Double, val shin: Double, val foot: Double) : LegPose {
        constructor(thigh: Number, shin: Number, foot: Number) : this(thigh.toDouble(), shin.toDouble(), foot.toDouble())
    }

    companion object {
        fun interpolate(a: LegPose, b: LegPose, t: Double): LegPose = when {
            a is Planted && b is Planted -> Planted(lerp(a.ankle, b.ankle, t), lerp(a.foot, b.foot, t))
            a is Angles && b is Angles -> Angles(lerp(a.thigh, b.thigh, t), lerp(a.shin, b.shin, t), lerp(a.foot, b.foot, t))
            // Mixed leg types can't blend smoothly; switch at the halfway point.
            else -> if (t < 0.5) a else b
        }
    }
}

/** One keyframe of the figure. */
data class FigurePose(
    val root: PoseRoot,
    val rootPoint: Pt,
    /** Absolute angle of the lower back (pelvis → mid-back). */
    val lumbar: Double,
    /** Absolute angle of the upper back (mid-back → shoulders). */
    val thoracic: Double,
    /** Absolute angle of the neck (shoulders → head). */
    val neck: Double,
    val arm: ArmPose,
    val leg: LegPose,
    /** Far-side arm and leg. null means the same as the near side. */
    val farArm: ArmPose? = null,
    val farLeg: LegPose? = null,
) {
    constructor(
        root: PoseRoot, rootPoint: Pt, lumbar: Number, thoracic: Number, neck: Number,
        arm: ArmPose, leg: LegPose, farArm: ArmPose? = null, farLeg: LegPose? = null,
    ) : this(root, rootPoint, lumbar.toDouble(), thoracic.toDouble(), neck.toDouble(), arm, leg, farArm, farLeg)

    val resolvedFarArm: ArmPose get() = farArm ?: arm
    val resolvedFarLeg: LegPose get() = farLeg ?: leg

    companion object {
        /** Blends two poses. t = 0 gives a, t = 1 gives b. */
        fun interpolate(a: FigurePose, b: FigurePose, t: Double) = FigurePose(
            root = if (t < 1) a.root else b.root,
            rootPoint = lerp(a.rootPoint, b.rootPoint, t),
            lumbar = lerp(a.lumbar, b.lumbar, t),
            thoracic = lerp(a.thoracic, b.thoracic, t),
            neck = lerp(a.neck, b.neck, t),
            arm = ArmPose.interpolate(a.arm, b.arm, t),
            leg = LegPose.interpolate(a.leg, b.leg, t),
            farArm = if (a.farArm == null && b.farArm == null) null else ArmPose.interpolate(a.resolvedFarArm, b.resolvedFarArm, t),
            farLeg = if (a.farLeg == null && b.farLeg == null) null else LegPose.interpolate(a.resolvedFarLeg, b.resolvedFarLeg, t),
        )
    }
}

/** A rectangle in drawing units. */
data class Box(val x: Double, val y: Double, val width: Double, val height: Double) {
    val minX get() = x
    val minY get() = y
    val maxX get() = x + width
    val maxY get() = y + height
}

/** The fixed drawing area the figure and mat live in. */
object FigureStage {
    val viewBox = Box(40.0, 96.0, 440.0, 200.0)
    const val floorY = 288.0
    val mat = Box(60.0, 280.0, 400.0, 8.0)
    /** How far the far-side arm and leg are shifted to suggest depth. */
    val farSideOffset = Pt(7.0, -6.0)
}

enum class ReformerStraps { NONE, HANDS, FEET }

/** Equipment drawn with the figure. */
sealed interface FigureProp {
    /** Small ball squeezed between the knees. */
    data object Ball : FigureProp
    /** Pilates ring squeezed between the knees. */
    data object Ring : FigureProp
    /** Light hand weights. */
    data object Weights : FigureProp
    /** Resistance band from the hands to the feet. */
    data object Band : FigureProp
    /** Band held in both hands, looped around the near foot (single-leg work). */
    data object BandToNearFoot : FigureProp
    /** Pilates ring held between the palms. */
    data object RingInHands : FigureProp
    /** A wall at this x position (drawing units). */
    data class Wall(val x: Double) : FigureProp {
        constructor(x: Number) : this(x.toDouble())
    }
    /** A reformer: frame, sliding carriage, footbar, springs, and straps to the back pulleys. */
    data class Reformer(val straps: ReformerStraps) : FigureProp
}

/** Fixed positions of the reformer, in drawing units. */
object ReformerGeometry {
    /** Height of the body's center line when lying on the carriage. */
    const val lyingY = 224.0
    val footbar = Pt(410.0, 196.0)
    /** Where heels rest on the footbar. */
    val footbarHeel = Pt(404.0, 194.0)
    val pulley = Pt(62.0, 186.0)
    const val carriageLength = 170.0
    const val carriageTop = 234.0
    const val railTop = 244.0
    const val springEnd = 416.0

    /** The carriage's left edge: anchored at the shoulders when bridging, otherwise at the pelvis. */
    fun carriageLeft(joints: FigureJoints, root: PoseRoot): Double =
        if (root == PoseRoot.SHOULDER) joints.shoulder.x - 40 else joints.pelvis.x - 150
}
