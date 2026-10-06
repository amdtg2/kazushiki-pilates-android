package com.kazushiki.pilates.exercises

import com.kazushiki.pilates.figure.FigurePose
import com.kazushiki.pilates.figure.FigureProp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Equipment(val title: String) {
    @SerialName("mat") MAT("Mat only"),
    @SerialName("band") BAND("Resistance band"),
    @SerialName("ring") RING("Pilates ring"),
    @SerialName("ball") BALL("Small ball"),
    @SerialName("weights") WEIGHTS("Light weights"),
    @SerialName("wall") WALL("Wall"),
    @SerialName("reformer") REFORMER("Reformer");

    val id: String get() = name.lowercase()
}

@Serializable
enum class BodyArea(val title: String) {
    @SerialName("core") CORE("Core"),
    @SerialName("glutes") GLUTES("Glutes"),
    @SerialName("legs") LEGS("Legs"),
    @SerialName("arms") ARMS("Arms"),
    @SerialName("back") BACK("Back"),
    @SerialName("fullBody") FULL_BODY("Full body");
}

@Serializable
enum class ExerciseLevel(val title: String, val detail: String) {
    @SerialName("beginner") BEGINNER("Beginner", "New to Pilates, or coming back after a break"),
    @SerialName("intermediate") INTERMEDIATE("Intermediate", "Comfortable with basics like the Hundred"),
    @SerialName("advanced") ADVANCED("Advanced", "You practice regularly and want a challenge");

    val rank: Int get() = ordinal
}

/** One timed step of an exercise: move to [pose] over [duration] seconds. */
data class ExerciseStep(
    val pose: String,
    val duration: Double,
    val phase: String,
    val cue: String,
    /** Small rhythmic arm pumps on top of the pose (the Hundred). */
    val isPump: Boolean = false,
)

/** A version of an exercise (e.g. Beginner / Advanced). Every variation provides the same pose names. */
data class ExerciseVariation(
    val id: String,
    val title: String,
    val level: ExerciseLevel,
    val poses: Map<String, FigurePose>,
) {
    fun pose(named: String): FigurePose = poses[named] ?: poses.values.first()
}

sealed interface ExerciseCounter {
    /** Counts full loops of the sequence as reps. */
    data class Reps(val count: Int) : ExerciseCounter
    /** Counts arm pumps to 100 during pump steps. */
    data object HundredCount : ExerciseCounter
}

data class Exercise(
    val id: String,
    val name: String,
    val summary: String,
    val equipment: List<Equipment>,
    val bodyAreas: List<BodyArea>,
    val variations: List<ExerciseVariation>,
    val sequence: List<ExerciseStep>,
    val counter: ExerciseCounter,
    /** Equipment drawn with the figure. */
    val props: List<FigureProp> = emptyList(),
) {
    /** Length of one full loop of the sequence, in seconds. */
    val loopDuration: Double get() = sequence.sumOf { it.duration }

    /** Phase names in order, without repeats. */
    val phases: List<String> get() = sequence.map { it.phase }.distinct()
}
