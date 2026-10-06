package com.kazushiki.pilates.model

import com.kazushiki.pilates.exercises.BodyArea
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
enum class SessionRating(val title: String) {
    @SerialName("tooEasy") TOO_EASY("Too easy"),
    @SerialName("justRight") JUST_RIGHT("Just right"),
    @SerialName("tooHard") TOO_HARD("Too hard"),
}

/** What the user did when logging a workout by hand. */
@Serializable
enum class ManualActivity(val title: String, val systemImage: String) {
    @SerialName("pilates") PILATES("Pilates", "figure.pilates"),
    @SerialName("reformer") REFORMER("Reformer", "figure.rower"),
    @SerialName("yoga") YOGA("Yoga", "figure.yoga"),
    @SerialName("strength") STRENGTH("Strength", "dumbbell.fill"),
    @SerialName("cardio") CARDIO("Cardio", "figure.run"),
    @SerialName("other") OTHER("Other", "sparkles"),
}

/** Stores instants as epoch milliseconds. */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilli())
    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}

@Serializable
data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    @Serializable(with = InstantSerializer::class) val date: Instant,
    val workoutID: String,
    val workoutTitle: String,
    val planID: String? = null,
    val dayNumber: Int? = null,
    val seconds: Double,
    val rating: SessionRating? = null,
    /** Set for workouts done outside the app and logged by hand. */
    val isManual: Boolean? = null,
    val activity: ManualActivity? = null,
    val bodyAreas: List<BodyArea>? = null,
) {
    val minutes: Int get() = max(1, (seconds / 60).roundToInt())
}
