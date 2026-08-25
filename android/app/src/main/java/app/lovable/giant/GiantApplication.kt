package app.lovable.giant

import android.app.Application
import android.util.Log
import app.lovable.giant.data.repository.SessionRepository

class GiantApplication : Application() {

    lateinit var sessionRepository: SessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Initializing GiantApplication Native Core...")
        sessionRepository = SessionRepository(this)
    }

    companion object {
        private const val TAG = "GiantApplication"
        lateinit var instance: GiantApplication
            private set
    }
}
