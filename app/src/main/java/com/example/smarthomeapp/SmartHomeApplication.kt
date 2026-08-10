package com.example.smarthomeapp

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class SmartHomeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        enableDatabasePersistence()
    }

    /**
     * Disk persistence lets the dashboard render last-known device state while offline and
     * replays any toggles made offline once the connection returns. It must be set once,
     * before any other [FirebaseDatabase] call, which is why it lives here.
     *
     * Throws until Realtime Database has been created in the Firebase console — the database
     * URL is absent from google-services.json until then. Guarded so a misconfigured project
     * surfaces as a log line rather than a crash at launch.
     */
    private fun enableDatabasePersistence() {
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Realtime Database unavailable. Create the database in the Firebase console, " +
                    "then re-download google-services.json into app/.",
                e
            )
        }
    }

    private companion object {
        const val TAG = "SmartHomeApplication"
    }
}
