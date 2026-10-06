package com.kazushiki.pilates.figure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kazushiki.pilates.ui.theme.KP
import kotlin.math.min

/** Draws the figure in a given pose on a mat. Sizes itself to fit, keeping proportions. */
@Composable
fun FigureView(pose: FigurePose, props: List<FigureProp> = emptyList(), modifier: Modifier = Modifier) {
    val near = KP.colors.figure
    val far = KP.colors.figureFar
    val colors = PropColors(prop = KP.colors.prop, apparatus = KP.colors.apparatus, carriage = KP.colors.mat)
    val matColor = KP.colors.mat
    val floorColor = KP.colors.floor
    val box = FigureStage.viewBox
    val farOffset = FigureStage.farSideOffset

    Canvas(
        modifier = modifier
            .aspectRatio((box.width / box.height).toFloat())
            .semantics { contentDescription = "Animated figure demonstrating the exercise" },
    ) {
        val joints = FigureSolver.solve(pose)
        val torso = polyline(joints.torso)
        val arm = polyline(joints.arm)
        val leg = polyline(joints.leg)
        val farArm = polyline(joints.farArm)
        val farLeg = polyline(joints.farLeg)
        val head = circle(joints.head, FigureSkeleton.headRadius)
        val bun = circle(joints.bun, FigureSkeleton.bunRadius)
        val behind = propPaths(props, joints, pose.root, inFront = false)
        val inFront = propPaths(props, joints, pose.root, inFront = true)
        val showsMat = props.none { it is FigureProp.Reformer }

        val floor = Path().apply {
            moveTo(30f, FigureStage.floorY.toFloat())
            lineTo(490f, FigureStage.floorY.toFloat())
        }
        val mat = Path().apply {
            addRoundRect(RoundRect(rect(FigureStage.mat.x, FigureStage.mat.y, FigureStage.mat.width, FigureStage.mat.height), CornerRadius(4f, 4f)))
        }

        val boxWidth = box.width.toFloat()
        val boxHeight = box.height.toFloat()
        val scale = min(size.width / boxWidth, size.height / boxHeight)
        val dx = (size.width - boxWidth * scale) / 2f - box.minX.toFloat() * scale
        val dy = (size.height - boxHeight * scale) / 2f - box.minY.toFloat() * scale

        withTransform({
            translate(left = dx, top = dy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            drawPath(floor, color = floorColor, style = Stroke(width = 2f))
            for (item in behind) drawPropShape(item, colors)
            if (showsMat) drawPath(mat, color = matColor)

            translate(left = farOffset.x.toFloat(), top = farOffset.y.toFloat()) {
                drawPath(farLeg, color = far, style = limb(13f))
                drawPath(farArm, color = far, style = limb(11f))
            }

            drawPath(torso, color = near, style = limb(24f))
            drawPath(bun, color = near)
            drawPath(head, color = near)
            drawPath(leg, color = near, style = limb(14f))
            drawPath(arm, color = near, style = limb(11f))

            for (item in inFront) drawPropShape(item, colors)
        }
    }
}

// Props

private enum class PropRole { PROP, APPARATUS, CARRIAGE }

private data class PropColors(val prop: Color, val apparatus: Color, val carriage: Color) {
    fun color(role: PropRole): Color = when (role) {
        PropRole.PROP -> prop
        PropRole.APPARATUS -> apparatus
        PropRole.CARRIAGE -> carriage
    }
}

private sealed interface PropShape {
    class FillShape(val path: Path, val role: PropRole) : PropShape
    class StrokeShape(val path: Path, val width: Float, val role: PropRole) : PropShape
}

private fun propPaths(props: List<FigureProp>, joints: FigureJoints, root: PoseRoot, inFront: Boolean): List<PropShape> {
    val far = FigureStage.farSideOffset
    fun shifted(p: Pt): Pt = Pt(p.x + far.x, p.y + far.y)
    val betweenKnees = Pt(joints.knee.x + far.x / 2, joints.knee.y + far.y / 2)

    val shapes = mutableListOf<PropShape>()
    for (prop in props) {
        when (prop) {
            is FigureProp.Wall -> {
                if (!inFront) {
                    shapes.add(PropShape.FillShape(rectPath(prop.x, 100.0, 10.0, FigureStage.floorY - 100), PropRole.PROP))
                }
            }
            FigureProp.Ball -> {
                if (inFront) shapes.add(PropShape.FillShape(circle(betweenKnees, 13.0), PropRole.PROP))
            }
            FigureProp.Ring -> {
                if (inFront) shapes.add(PropShape.StrokeShape(circle(betweenKnees, 17.0), 4f, PropRole.PROP))
            }
            FigureProp.Weights -> {
                if (inFront) {
                    shapes.add(PropShape.FillShape(circle(joints.hand, 8.0), PropRole.PROP))
                    shapes.add(PropShape.FillShape(circle(shifted(joints.farHand), 7.0), PropRole.PROP))
                }
            }
            FigureProp.Band -> {
                if (inFront) {
                    shapes.add(PropShape.StrokeShape(polyline(listOf(joints.hand, joints.toe)), 3f, PropRole.PROP))
                    shapes.add(PropShape.StrokeShape(polyline(listOf(shifted(joints.farHand), shifted(joints.farToe))), 2f, PropRole.PROP))
                }
            }
            FigureProp.BandToNearFoot -> {
                if (inFront) {
                    shapes.add(PropShape.StrokeShape(polyline(listOf(joints.hand, joints.toe)), 3f, PropRole.PROP))
                    shapes.add(PropShape.StrokeShape(polyline(listOf(shifted(joints.farHand), joints.toe)), 2f, PropRole.PROP))
                }
            }
            FigureProp.RingInHands -> {
                if (inFront) {
                    val farHand = shifted(joints.farHand)
                    val between = Pt((joints.hand.x + farHand.x) / 2 + 12, (joints.hand.y + farHand.y) / 2)
                    shapes.add(PropShape.StrokeShape(circle(between, 15.0), 4f, PropRole.PROP))
                }
            }
            is FigureProp.Reformer -> {
                if (inFront) {
                    shapes.add(PropShape.FillShape(circle(ReformerGeometry.footbar, 6.0), PropRole.APPARATUS))
                } else {
                    shapes.addAll(reformerShapes(joints, root, prop.straps))
                }
            }
        }
    }
    return shapes
}

private fun reformerShapes(joints: FigureJoints, root: PoseRoot, straps: ReformerStraps): List<PropShape> {
    val g = ReformerGeometry
    val shapes = mutableListOf<PropShape>()
    // Frame, legs, pulley post and footbar post.
    shapes.add(PropShape.FillShape(rectPath(58.0, g.railTop, 394.0, 8.0), PropRole.APPARATUS))
    for (x in listOf(70.0, 440.0)) {
        shapes.add(PropShape.FillShape(rectPath(x - 4, g.railTop + 8, 8.0, FigureStage.floorY - g.railTop - 8), PropRole.APPARATUS))
    }
    shapes.add(PropShape.StrokeShape(polyline(listOf(Pt(g.pulley.x, g.railTop), Pt(g.pulley.x, g.pulley.y + 2))), 5f, PropRole.APPARATUS))
    shapes.add(PropShape.FillShape(circle(g.pulley, 5.0), PropRole.APPARATUS))
    shapes.add(PropShape.StrokeShape(polyline(listOf(Pt(420.0, g.railTop), g.footbar)), 6f, PropRole.APPARATUS))

    // Carriage, shoulder block and springs.
    val left = g.carriageLeft(joints, root)
    val right = left + g.carriageLength
    val spring = mutableListOf(Pt(right, 248.0))
    val coils = 8
    for (k in 1 until coils) {
        spring.add(Pt(right + (g.springEnd - right) * k.toDouble() / coils.toDouble(), if (k % 2 == 0) 251.0 else 245.0))
    }
    spring.add(Pt(g.springEnd, 248.0))
    shapes.add(PropShape.StrokeShape(polyline(spring), 2f, PropRole.PROP))
    val carriage = Path().apply {
        addRoundRect(RoundRect(rect(left, g.carriageTop, g.carriageLength, g.railTop - g.carriageTop), CornerRadius(3f, 3f)))
    }
    shapes.add(PropShape.FillShape(carriage, PropRole.CARRIAGE))
    shapes.add(PropShape.FillShape(rectPath(left + 14, g.carriageTop - 20, 8.0, 20.0), PropRole.APPARATUS))

    // Straps run from the back pulley to the hands or feet.
    when (straps) {
        ReformerStraps.HANDS -> shapes.add(PropShape.StrokeShape(polyline(listOf(g.pulley, joints.hand)), 2f, PropRole.PROP))
        ReformerStraps.FEET -> shapes.add(PropShape.StrokeShape(polyline(listOf(g.pulley, joints.ankle)), 2f, PropRole.PROP))
        ReformerStraps.NONE -> Unit
    }
    return shapes
}

private fun DrawScope.drawPropShape(shape: PropShape, colors: PropColors) {
    when (shape) {
        is PropShape.FillShape -> drawPath(shape.path, color = colors.color(shape.role))
        is PropShape.StrokeShape -> drawPath(shape.path, color = colors.color(shape.role), style = limb(shape.width))
    }
}

// Helpers

private fun polyline(points: List<Pt>): Path {
    val path = Path()
    points.forEachIndexed { index, p ->
        if (index == 0) path.moveTo(p.x.toFloat(), p.y.toFloat()) else path.lineTo(p.x.toFloat(), p.y.toFloat())
    }
    return path
}

private fun circle(center: Pt, radius: Double): Path {
    val path = Path()
    path.addOval(rect(center.x - radius, center.y - radius, radius * 2, radius * 2))
    return path
}

private fun rect(x: Double, y: Double, width: Double, height: Double): Rect =
    Rect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat())

private fun rectPath(x: Double, y: Double, width: Double, height: Double): Path {
    val path = Path()
    path.addRect(rect(x, y, width, height))
    return path
}

private fun limb(width: Float): Stroke = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
