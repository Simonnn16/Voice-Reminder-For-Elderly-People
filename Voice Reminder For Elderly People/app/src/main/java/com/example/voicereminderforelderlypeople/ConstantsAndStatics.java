package com.example.voicereminderforelderlypeople;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

final class ConstantsAndStatics {

	static final String BUNDLE_KEY_ALARM_DETAILS = "com.example.voicereminderforelderlypeople.ALARM_DETAILS_BUNDLE";

	static final String BUNDLE_KEY_ALARM_HOUR = "com.example.voicereminderforelderlypeople.ALARM_HOUR";

	static final String BUNDLE_KEY_ALARM_MINUTE = "com.example.voicereminderforelderlypeople.ALARM_MINUTES";

	static final String BUNDLE_KEY_ALARM_TYPE = "com.example.voicereminderforelderlypeople.ALARM_TYPE";

	static final String BUNDLE_KEY_ALARM_VOLUME = "com.example.voicereminderforelderlypeople.ALARM_VOLUME";

	static final String BUNDLE_KEY_SNOOZE_TIME_IN_MINS = "com.example.voicereminderforelderlypeople.SNOOZE_TIME_IN_MINS";

	static final String BUNDLE_KEY_IS_REPEAT_ON = "com.example.voicereminderforelderlypeople.IS_REPEAT_ON";

	static final String BUNDLE_KEY_IS_ALARM_ON = "com.example.voicereminderforelderlypeople.IS_ALARM_ON";

	static final String BUNDLE_KEY_REPEAT_DAYS = "com.example.voicereminderforelderlypeople.REPEAT_DAYS";

	static final int ALARM_TYPE_SOUND_ONLY = 0;

	static final int ALARM_TYPE_VIBRATE_ONLY = 1;

	static final int ALARM_TYPE_SOUND_AND_VIBRATE = 2;

	static final String ACTION_DELIVER_ALARM = "com.example.voicereminderforelderlypeople.DELIVER_ALARM";

	static final String BUNDLE_KEY_ALARM_DAY = "com.example.voicereminderforelderlypeople.ALARM_DAY";

	static final String BUNDLE_KEY_ALARM_MONTH = "com.example.voicereminderforelderlypeople.ALARM_MONTH";

	static final String BUNDLE_KEY_ALARM_YEAR = "com.example.voicereminderforelderlypeople.ALARM_YEAR";

	static final String BUNDLE_KEY_ALARM_MESSAGE = "com.example.voicereminderforelderlypeople.ALARM_MESSAGE";

	static final String BUNDLE_KEY_ALARM_TONE_URI = "com.example.voicereminderforelderlypeople.ALARM_TONE_URI";

	static final String BUNDLE_KEY_HAS_USER_CHOSEN_DATE = "com.example.voicereminderforelderlypeople.HAS_USER_CHOSEN_DATE";

	static final String ACTION_SNOOZE_ALARM = "com.example.voicereminderforelderlypeople.SNOOZE_ALARM";

	static final String ACTION_CANCEL_ALARM = "com.example.voicereminderforelderlypeople.CANCEL_ALARM";

	static final String SHARED_PREF_FILE_NAME = "com.example.voicereminderforelderlypeople.SHARED_PREF_FILE";

	static final String ACTION_NEW_ALARM = "com.example.voicereminderforelderlypeople.ACTION_NEW_ALARM";

	static final String ACTION_EXISTING_ALARM = "com.example.voicereminderforelderlypeople.ACTION_EXISTING_ALARM";

	static final String ACTION_NEW_ALARM_FROM_INTENT =	"com.example.voicereminderforelderlypeople.ACTION_NEW_ALARM_FROM_INTENT";

	static final String BUNDLE_KEY_OLD_ALARM_HOUR = "com.example.voicereminderforelderlypeople.OLD_ALARM_HOUR";

	static final String BUNDLE_KEY_OLD_ALARM_MINUTE = "com.example.voicereminderforelderlypeople.OLD_ALARM_MINUTE";

	static final String BUNDLE_KEY_ALARM_ID = "com.example.voicereminderforelderlypeople.OLD_ALARM_ID";

	static final String ACTION_DESTROY_RING_ALARM_ACTIVITY = "com.example.voicereminderforelderlypeople.DESTROY_RING_ALARM_ACTIVITY";

	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON = "com.example.voicereminderforelderlypeople.DEFAULT_SNOOZE_STATE";

	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL = "com.example.voicereminderforelderlypeople.DEFAULT_SNOOZE_INTERVAL";

	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ = "com.example.voicereminderforelderlypeople.DEFAULT_SNOOZE_FREQUENCY";

	static final String SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI = "com.example.voicereminderforelderlypeople.DEFAULT_ALARM_TONE_URI";

	static final String SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME = "com.example.voicereminderforelderlypeople.DEFAULT_ALARM_VOLUME";

	static final String SHARED_PREF_KEY_AUTO_SET_TONE = "com.example.voicereminderforelderlypeople.AUTO_SET_TONE";

	static final String WORK_NAME_ACTIVATE_ALARMS = "com.example.WORK_ACTIVATE_ALARMS";

	static void schedulePeriodicWork(Context context) {

		try {
			WorkManager.initialize(context, new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build());
		} catch (Exception ignored) {
		}

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(Worker_ActivateAlarms.class, 15, TimeUnit.MINUTES).build();

		WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME_ACTIVATE_ALARMS, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest);
	}

	static void cancelScheduledPeriodicWork(Context context) {

		try {
			WorkManager.initialize(context, new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build());
		} catch (Exception ignored) {
		}

		WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ACTIVATE_ALARMS);
	}

	static void killServices(Context context, int alarmID) {

		if (Service_RingAlarm.isThisServiceRunning && Service_RingAlarm.alarmID == alarmID) {
			Intent intent1 = new Intent(context, Service_RingAlarm.class);
			context.stopService(intent1);
		}
	}

	static LocalDateTime getAlarmDateTime(LocalDate alarmDate, LocalTime alarmTime, boolean isRepeatOn, @Nullable ArrayList<Integer> repeatDays) {

		LocalDateTime alarmDateTime;

		if (isRepeatOn && repeatDays != null && repeatDays.size() > 0) {

			Collections.sort(repeatDays);

			alarmDateTime = LocalDateTime.of(LocalDate.now(), alarmTime);
			int dayOfWeek = alarmDateTime.getDayOfWeek().getValue();

			for (int i = 0; i < repeatDays.size(); i++) {
				if (repeatDays.get(i) == dayOfWeek) {
					if (alarmTime.isAfter(LocalTime.now())) {
						break;
					}
				} else if (repeatDays.get(i) > dayOfWeek) {
					alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(i))));
					break;
				}
				if (i == repeatDays.size() - 1) {
					alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(DayOfWeek.of(repeatDays.get(0))));
				}
			}

		} else {

			alarmDateTime = LocalDateTime.of(alarmDate, alarmTime);

			if (! alarmDateTime.isAfter(LocalDateTime.now())) {
				alarmDateTime = alarmDateTime.plusDays(1);
			}
		}

		return alarmDateTime.withSecond(0).withNano(0);
	}

}
