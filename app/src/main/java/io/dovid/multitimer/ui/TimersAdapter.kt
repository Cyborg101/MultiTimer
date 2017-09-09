package io.dovid.multitimer.ui

import android.app.Activity
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import io.dovid.multitimer.BuildConfig
import io.dovid.multitimer.R
import io.dovid.multitimer.database.DatabaseHelper
import io.dovid.multitimer.model.TimerDAO
import io.dovid.multitimer.model.TimerEntity
import io.dovid.multitimer.utilities.TimerAlarmManager
import io.dovid.multitimer.utilities.TimerRunner
import org.apache.commons.lang.time.DurationFormatUtils
import java.util.*


/**
 * Author: Umberto D'Ovidio
 * Date: 19/08/17
 * Email: umberto.dovidio@gmail.com
 * Website: http://dovid.io
 * Tutorial link : http://dovid.io
 */

internal class TimersAdapter(private val context: Context) : RecyclerView.Adapter<TimersAdapter.TimerViewHolder>() {
    // TODO: animazioni quando si fa partire un timer

    private var timers: ArrayList<TimerEntity>
    private val databaseHelper: DatabaseHelper
    private lateinit var colors: IntArray
    private var lastPosition = -1

    init {
        databaseHelper = DatabaseHelper.getInstance(context)
        timers = TimerDAO.getTimers(databaseHelper)

        TimerRunner.run(context)
    }

    fun setColors(colors: IntArray) {
        this.colors = colors
        notifyDataSetChanged()
    }

    fun refreshTimers() {
        timers = TimerDAO.getTimers(databaseHelper)
        notifyDataSetChanged()
    }

    fun insertTimer() {
        timers = TimerDAO.getTimers(databaseHelper)
        notifyItemInserted(timers.size)
    }

    private fun deleteTimer(position: Int) {
        TimerDAO.deleteTimer(databaseHelper, timers!![position].id)
        notifyItemRemoved(position)
    }

    private fun showUpdateTimerDialog(position: Int) {
        val setupDialog = TimerUpdateDialog.getInstance(
                databaseHelper,
                timers!![position].id)
        setupDialog.show((context as Activity).fragmentManager, "create tag")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.timer, parent, false)
        return TimerViewHolder(v)
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return timers!!.size
    }

    inner class TimerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(position: Int) {
            if (timers!![position].isRunning || timers!![position].defaultTime != timers!![position].expiredTime) {
                setupPlayView(position)
            } else {
                setupPauseView(position)
            }
        }

        private fun setupPauseView(position: Int) {

            itemView.findViewById<View>(R.id.pauseTimer).visibility = View.VISIBLE
            itemView.findViewById<View>(R.id.playTimer).visibility = View.GONE

            val timerNameTV = itemView.findViewById<TextView>(R.id.textViewTimerName)
            val defaultTimeTV = itemView.findViewById<TextView>(R.id.textViewDefaultTime)

            val timer = timers[position]

            timerNameTV.text = timer.name
            timerNameTV.setBackgroundColor(ContextCompat.getColor(context, colors[timer.id % colors.size]))
            timerNameTV.setOnCreateContextMenuListener { contextMenu, view, contextMenuInfo ->
                contextMenu.setHeaderTitle(R.string.what_to_do)
                contextMenu.add(0, view.id, 0, R.string.delete).setOnMenuItemClickListener {
                    deleteTimer(position)
                    true
                }

                contextMenu.add(0, view.id, 0, R.string.update).setOnMenuItemClickListener {
                    showUpdateTimerDialog(position)
                    true
                }
            }

            defaultTimeTV.text = DurationFormatUtils.formatDuration(timer.defaultTime, BuildConfig.ITALIANTIME)
            val playButton = itemView.findViewById<ImageButton>(R.id.buttonPlay)
            playButton.setOnClickListener {
                TimerDAO.updateTimerPlayTimestamp(databaseHelper, timer.id, java.util.Date().time)
                TimerDAO.updateTimerRunning(databaseHelper, timer.id, true)
                refreshTimers()
                TimerAlarmManager.setupAlarms(context, timers)
            }

            val switchButton = itemView.findViewById<Switch>(R.id.switchNotify)
            switchButton.isChecked = timer.shouldNotify()

            switchButton.setOnCheckedChangeListener { compoundButton, b -> TimerDAO.updateTimerShouldNotify(databaseHelper, timer.id, b) }
        }

        private fun setupPlayView(position: Int) {

            val timer = timers[position]

            itemView.findViewById<View>(R.id.pauseTimer).visibility = View.GONE
            itemView.findViewById<View>(R.id.playTimer).visibility = View.VISIBLE

            val pause = itemView.findViewById<ImageButton>(R.id.buttonPause)
            val countdownRunning = itemView.findViewById<TextView>(R.id.editTextCountdownRunning)

            countdownRunning.text = DurationFormatUtils.formatDuration(timer.expiredTime, BuildConfig.ITALIANTIME)
            countdownRunning.setBackgroundResource(colors[position % colors.size])

            val resetButton = itemView.findViewById<Button>(R.id.buttonReset)
            resetButton.setTextColor(ContextCompat.getColor(context, colors[timer.id % colors.size]))

            resetButton.setOnClickListener {
                itemView.findViewById<View>(R.id.playTimer).visibility = View.GONE
                itemView.findViewById<View>(R.id.pauseTimer).visibility = View.VISIBLE
                TimerDAO.updateTimerRunning(databaseHelper, timer.id, false)
                TimerDAO.updateTimerExpiredTime(databaseHelper, timer.id, timer.defaultTime)
                TimerDAO.putPlayTimeStampNull(databaseHelper, timer.id)
                refreshTimers()
                TimerAlarmManager.setupAlarms(context, timers)
            }

            pause.setOnClickListener {
                if (timer.isRunning) {
                    pause.setImageResource(R.drawable.play_icon)
                    TimerDAO.putPlayTimeStampNull(databaseHelper, timer.id)
                } else {
                    pause.setImageResource(R.drawable.pause_icon)
                    TimerDAO.updateTimerPlayTimestamp(databaseHelper, timer.id, java.util.Date().time - (timer.defaultTime - timer.expiredTime))
                }
                TimerDAO.updateTimerRunning(databaseHelper, timer.id, !timer.isRunning)
                refreshTimers()
                TimerAlarmManager.setupAlarms(context, timers)
            }

            if (timer.expiredTime != timer.defaultTime) {
                if (!timer.isRunning) {
                    pause.setImageResource(R.drawable.play_icon)
                }
            }
        }

        fun clearAnimation() {
            itemView.clearAnimation()
        }
    }

    companion object {

        private val TAG = "CUSTOMADAPTER"
    }
}
