package app.miyomu.android

import android.app.Application
import android.content.Context
import app.miyomu.application.MiyomuServices
import app.miyomu.data.AndroidMiyomuDatabaseFactory
import java.io.File

/**
 * Holds the long-lived service container at process level: rotating the Activity or losing the
 * process UI must not close the database or cancel in-flight downloads.
 */
class MiyomuAndroidApp : Application() {

    val services: MiyomuServices by lazy {
        MiyomuServices.create(
            dataDir = File(filesDir.absolutePath),
            databaseFactory = AndroidMiyomuDatabaseFactory(this),
        )
    }

    companion object {
        fun from(context: Context): MiyomuAndroidApp =
            context.applicationContext as MiyomuAndroidApp
    }
}
