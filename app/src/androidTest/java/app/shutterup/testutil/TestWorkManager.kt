package app.shutterup.testutil

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.concurrent.Executors

fun initTestWorkManager(context: Context) {
    val config = Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(Executors.newSingleThreadExecutor())
        .build()
    WorkManagerTestInitHelper.initializeTestWorkManager(context.applicationContext, config)
}
