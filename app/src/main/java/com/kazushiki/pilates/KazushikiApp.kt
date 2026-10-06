package com.kazushiki.pilates

import android.app.Application
import com.kazushiki.pilates.data.ActivityStore
import com.kazushiki.pilates.data.ProfileStore
import com.kazushiki.pilates.data.SharedPrefsStore
import com.kazushiki.pilates.services.ReminderScheduler
import com.kazushiki.pilates.services.SubscriptionStore

/** Holds the app-wide stores (the @State stores in KazushikiPilatesApp on iPhone). */
class KazushikiApp : Application() {
    lateinit var profileStore: ProfileStore
        private set
    lateinit var activityStore: ActivityStore
        private set
    lateinit var subscriptionStore: SubscriptionStore
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = SharedPrefsStore(this)
        profileStore = ProfileStore(prefs)
        activityStore = ActivityStore(prefs)
        subscriptionStore = SubscriptionStore(this, prefs)
        ReminderScheduler.createChannel(this)
    }
}
