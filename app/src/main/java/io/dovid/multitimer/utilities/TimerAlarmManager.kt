package io.dovid.multitimer.utilities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Build
import android.util.Log
import io.dovid.multitimer.BuildConfig
import io.dovid.multitimer.database.DatabaseHelper
import io.dovid.multitimer.model.TimerDAO
import io.dovid.multitimer.model.TimerEntity
import java.util.*

/**
 * Author: Umberto D'Ovidio
 * Date: 02/09/17
 * Email: umberto.dovidio@gmail.com
 * Website: http://dovid.io
 * Tutorial link : http://dovid.io
 */

object TimerAlarmManager {

    private val TAG = "TIMERALARMMANAGER"

    fun setupAlarms(context: Context, timers: ArrayList<TimerEntity>) {
        var minExpiredTime = java.lang.Long.MAX_VALUE
        var nameOfTimer: String? = null
        for (timer in timers) {
            if (timer.isRunning && timer.expiredTime < minExpiredTime && timer.shouldNotify()) {
                minExpiredTime = timer.expiredTime
                nameOfTimer = timer.name
            }
        }
        setAlarm(context, nameOfTimer, minExpiredTime)
    }

    fun setupAlarms(context: Context, databaseHelper: DatabaseHelper) {
        val timers = TimerDAO.getTimers(databaseHelper)

        var minExpiredTime = java.lang.Long.MAX_VALUE
        var nameOfTimer: String? = null
        for (timer in timers) {
            if (timer.isRunning && timer.expiredTime < minExpiredTime && timer.shouldNotify()) {
                minExpiredTime = timer.expiredTime
                nameOfTimer = timer.name
            }
        }
        setAlarm(context, nameOfTimer, minExpiredTime)
    }

    fun setupAlarms(context: Context) {
        var databaseHelper: DatabaseHelper? = null

        try {
            databaseHelper = DatabaseHelper.getInstance(context)

            val timers = TimerDAO.getTimers(databaseHelper)

            var minExpiredTime = java.lang.Long.MAX_VALUE
            var nameOfTimer: String? = null
            for (timer in timers) {
                if (timer.isRunning && timer.expiredTime < minExpiredTime && timer.shouldNotify()) {
                    minExpiredTime = timer.expiredTime
                    nameOfTimer = timer.name
                }
            }

            setAlarm(context, nameOfTimer, minExpiredTime)

        } catch (e: SQLiteException) {
            throw RuntimeException(e)
        } finally {
            databaseHelper?.close()
        }
    }

    private fun setAlarm(context: Context, nameOfTimer: String?, minExpiredTime: Long) {

        Log.d(TAG, "settingAlarm")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (nameOfTimer != null) {
            val intentAlarm = Intent(context, AlarmReceiver::class.java)
            intentAlarm.action = BuildConfig.TIME_IS_UP
            intentAlarm.putExtra(BuildConfig.EXTRA_TIMER_NAME, nameOfTimer)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Date().time + minExpiredTime, PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, Date().time + minExpiredTime, PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT))
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, Date().time + minExpiredTime, PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT))
            }
        } else {
            val intentAlarm = Intent(context, AlarmReceiver::class.java)
            intentAlarm.action = BuildConfig.TIME_IS_UP
            alarmManager.cancel(PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT))
        }
    }
}
