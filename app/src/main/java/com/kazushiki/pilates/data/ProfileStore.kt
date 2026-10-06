package com.kazushiki.pilates.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kazushiki.pilates.exercises.Equipment
import com.kazushiki.pilates.model.PilatesGoal
import com.kazushiki.pilates.model.UserProfile

class ProfileStore(private val store: KeyValueStore = MemoryStore()) {
    var profile: UserProfile by mutableStateOf(
        store.getString(PROFILE)?.let { runCatching { AppJson.decodeFromString(UserProfile.serializer(), it) }.getOrNull() } ?: UserProfile()
    )
        private set

    var hasCompletedOnboarding: Boolean by mutableStateOf(store.getBoolean(ONBOARDING) ?: false)
        private set

    /** Spoken coaching during workouts. */
    var voiceCuesEnabled: Boolean by mutableStateOf(store.getBoolean(VOICE) ?: true)
        private set

    /** Replaces the profile and saves it. */
    fun update(transform: (UserProfile) -> UserProfile) {
        profile = transform(profile)
        store.putString(PROFILE, AppJson.encodeToString(UserProfile.serializer(), profile))
    }

    fun setVoiceCues(enabled: Boolean) {
        voiceCuesEnabled = enabled
        store.putBoolean(VOICE, enabled)
    }

    fun toggleGoal(goal: PilatesGoal) = update {
        it.copy(goals = if (goal in it.goals) it.goals - goal else it.goals + goal)
    }

    fun toggleEquipment(item: Equipment) {
        if (item == Equipment.MAT) return
        update { it.copy(equipment = if (item in it.equipment) it.equipment - item else it.equipment + item) }
    }

    fun completeOnboarding() {
        if (!profile.isReady) return
        hasCompletedOnboarding = true
        store.putBoolean(ONBOARDING, true)
    }

    fun resetOnboarding() {
        update { UserProfile() }
        hasCompletedOnboarding = false
        store.putBoolean(ONBOARDING, false)
    }

    companion object {
        const val PROFILE = "kp.userProfile"
        const val ONBOARDING = "kp.onboardingComplete"
        const val VOICE = "kp.voiceCues"
    }
}
