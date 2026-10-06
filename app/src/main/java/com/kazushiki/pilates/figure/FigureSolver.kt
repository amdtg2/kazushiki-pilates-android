package com.kazushiki.pilates.figure

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Joint positions of a solved pose, in drawing units. */
data class FigureJoints(
    val pelvis: Pt,
    val midBack: Pt,
    val shoulder: Pt,
    val head: Pt,
    val bun: Pt,
    val elbow: Pt,
    val hand: Pt,
    val knee: Pt,
    val ankle: Pt,
    val toe: Pt,
    val farElbow: Pt,
    val farHand: Pt,
    val farKnee: Pt,
    val farAnkle: Pt,
    val farToe: Pt,
) {
    val torso get() = listOf(pelvis, midBack, shoulder)
    val arm get() = listOf(shoulder, elbow, hand)
    val leg get() = listOf(pelvis, knee, ankle, toe)
    val farArm get() = listOf(shoulder, farElbow, farHand)
    val farLeg get() = listOf(pelvis, farKnee, farAnkle, farToe)
    val all get() = listOf(pelvis, midBack, shoulder, head, bun, elbow, hand, knee, ankle, toe, farElbow, farHand, farKnee, farAnkle, farToe)
}

/** Turns a FigurePose into joint positions (forward kinematics, plus two-bone IK for planted legs). */
object FigureSolver {
    fun solve(pose: FigurePose): FigureJoints {
        val pelvis: Pt
        val midBack: Pt
        val shoulder: Pt
        when (pose.root) {
            PoseRoot.SHOULDER -> {
                shoulder = pose.rootPoint
                midBack = offset(shoulder, pose.thoracic, -FigureSkeleton.thoracic)
                pelvis = offset(midBack, pose.lumbar, -FigureSkeleton.lumbar)
            }
            PoseRoot.PELVIS -> {
                pelvis = pose.rootPoint
                midBack = offset(pelvis, pose.lumbar, FigureSkeleton.lumbar)
                shoulder = offset(midBack, pose.thoracic, FigureSkeleton.thoracic)
            }
        }
        val head = offset(shoulder, pose.neck, FigureSkeleton.neck)
        val bun = offset(head, pose.neck, FigureSkeleton.headRadius + 2)
        val arm = solveArm(pose.arm, shoulder)
        val farArm = solveArm(pose.resolvedFarArm, shoulder)
        val leg = solveLeg(pose.leg, pelvis)
        val farLeg = solveLeg(pose.resolvedFarLeg, pelvis)
        return FigureJoints(
            pelvis = pelvis, midBack = midBack, shoulder = shoulder,
            head = head, bun = bun, elbow = arm.first, hand = arm.second,
            knee = leg.knee, ankle = leg.ankle, toe = leg.toe,
            farElbow = farArm.first, farHand = farArm.second,
            farKnee = farLeg.knee, farAnkle = farLeg.ankle, farToe = farLeg.toe,
        )
    }

    fun solveArm(arm: ArmPose, shoulder: Pt): Pair<Pt, Pt> {
        val elbow = offset(shoulder, arm.upper, FigureSkeleton.upperArm)
        return elbow to offset(elbow, arm.fore, FigureSkeleton.forearm)
    }

    data class SolvedLeg(val knee: Pt, val ankle: Pt, val toe: Pt)

    fun solveLeg(leg: LegPose, hip: Pt): SolvedLeg = when (leg) {
        is LegPose.Planted -> {
            val thigh = FigureSkeleton.thigh
            val shin = FigureSkeleton.shin
            val reach = min(distance(hip, leg.ankle), thigh + shin - 0.01)
            val cosine = (thigh * thigh + reach * reach - shin * shin) / (2 * thigh * reach)
            val hipBend = acos(max(-1.0, min(1.0, cosine))) * 180 / PI
            // Bend the knee upward (away from the floor).
            val knee = offset(hip, angle(hip, leg.ankle) + hipBend, thigh)
            val ankle = offset(knee, angle(knee, leg.ankle), shin)
            SolvedLeg(knee, ankle, offset(ankle, leg.foot, FigureSkeleton.foot))
        }
        is LegPose.Angles -> {
            val knee = offset(hip, leg.thigh, FigureSkeleton.thigh)
            val ankle = offset(knee, leg.shin, FigureSkeleton.shin)
            SolvedLeg(knee, ankle, offset(ankle, leg.foot, FigureSkeleton.foot))
        }
    }

    /** Moves [point] by [length] in the direction [degrees]. */
    fun offset(point: Pt, degrees: Double, length: Double): Pt {
        val radians = degrees * PI / 180
        return Pt(point.x + cos(radians) * length, point.y - sin(radians) * length)
    }

    /** Absolute angle (degrees) of the line from p to q. */
    fun angle(p: Pt, q: Pt): Double = atan2(-(q.y - p.y), q.x - p.x) * 180 / PI
}
