package com.example.voicereminderforelderlypeople;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import com.example.voicereminderforelderlypeople.R;

public class Service_UpdateAlarm extends Service {

	private static final int NOTIFICATION_ID = 903;

	private AlarmDatabase alarmDatabase;

	public static boolean isThisServiceRunning = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
		} else {
			startForeground(NOTIFICATION_ID, buildNotification());
		}
		isThisServiceRunning = true;

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		alarmDatabase = AlarmDatabase.getInstance(this);

		ArrayList<AlarmEntity> alarmEntityArrayList = getActiveAlarms();

		if (alarmEntityArrayList != null && alarmEntityArrayList.size() > 0) {

			cancelActiveAlarms(alarmEntityArrayList);
			activateAlarms(alarmEntityArrayList);
		}

		stopSelf();

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isThisServiceRunning = false;
		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	private void createNotificationChannel(final int NOTIFICATION_ID) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(Integer.toString(NOTIFICATION_ID),
					"com.example.voicereminderforelderlypeople Notification", importance);
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			assert notificationManager != null;
			notificationManager.createNotificationChannel(channel);
		}
	}

	private Notification buildNotification() {
		createNotificationChannel(NOTIFICATION_ID);

		Intent contentIntent = new Intent(this, Activity_AlarmsList.class);
		contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 5701, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Integer.toString(NOTIFICATION_ID))
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(getResources().getString(R.string.updateAlarm_notifMessage))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setCategory(NotificationCompat.CATEGORY_STATUS)
				.setSmallIcon(R.drawable.ic_reminder_logo)
				.setContentIntent(contentPendingIntent);

		return builder.build();
	}

	@Nullable
	private ArrayList<AlarmEntity> getActiveAlarms() {

		AtomicReference<ArrayList<AlarmEntity>> alarmEntityArrayList = new AtomicReference<>(new ArrayList<>());

		Thread thread = new Thread(() -> alarmEntityArrayList.set(new ArrayList<>(alarmDatabase.alarmDAO().getActiveAlarms())));

		thread.start();
		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return alarmEntityArrayList.get();

	}

	private void cancelActiveAlarms(@NonNull ArrayList<AlarmEntity> alarmEntityArrayList) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		for (AlarmEntity alarmEntity : alarmEntityArrayList) {

			ConstantsAndStatics.killServices(this, alarmEntity.alarmID);

			Intent intent = new Intent(Service_UpdateAlarm.this, AlarmBroadcastReceiver.class);
			intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
			intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(Service_UpdateAlarm.this, alarmEntity.alarmID, intent, PendingIntent.FLAG_NO_CREATE);

			if (pendingIntent != null) {
				alarmManager.cancel(pendingIntent);
			}

		}
	}

	@Nullable
	private ArrayList<Integer> getRepeatDays(int alarmID) {

		AtomicReference<ArrayList<Integer>> repeatDays = new AtomicReference<>();

		Thread thread = new Thread(() -> repeatDays.set(new ArrayList<>(alarmDatabase.alarmDAO().getAlarmRepeatDays(alarmID))));
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return repeatDays.get();

	}

	private void activateAlarms(@NonNull ArrayList<AlarmEntity> alarmEntityArrayList) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		for (AlarmEntity alarmEntity : alarmEntityArrayList) {

			ArrayList<Integer> repeatDays = getRepeatDays(alarmEntity.alarmID);

			LocalDateTime alarmDateTime;

			LocalDate alarmDate = LocalDate.of(alarmEntity.alarmYear, alarmEntity.alarmMonth, alarmEntity.alarmDay);
			LocalTime alarmTime = LocalTime.of(alarmEntity.alarmHour, alarmEntity.alarmMinutes);

			if (alarmEntity.isRepeatOn && repeatDays != null && repeatDays.size() > 0) {

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
					alarmDateTime = null;
				}
			}

			if (alarmDateTime != null) {

				Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
				intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
				intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

				Bundle data = alarmEntity.getAlarmDetailsInABundle();
				data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, repeatDays);
				intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

				PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), alarmEntity.alarmID, intent, 0);

				ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime, ZoneId.systemDefault());

				alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000, pendingIntent), pendingIntent);

			} else {

				Thread thread = new Thread(() -> alarmDatabase.alarmDAO().toggleAlarm(alarmEntity.alarmID, 0));

				thread.start();
				try {
					thread.join();
				} catch (InterruptedException ignored) {
				}

				postAlarmMissedNotification(alarmEntity.alarmID, alarmTime);

			}

		}

	}

	private void postAlarmMissedNotification(int alarmID, LocalTime alarmTime) {

		createNotificationChannel(alarmID);

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		DateTimeFormatter formatter;
		if (! DateFormat.is24HourFormat(this)) {
			formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());
		} else {
			formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
		}
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Integer.toString(alarmID))
				.setContentTitle(getResources().getString(R.string.updateAlarm_alarmMissedTitle))
				.setContentText(getString(R.string.updateAlarm_alarmMissedText, alarmTime.format(formatter)))
				.setSmallIcon(R.drawable.ic_reminder_logo)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ERROR)
				.setDefaults(Notification.DEFAULT_SOUND)
				.setAutoCancel(true)
				.setOngoing(false);

		notificationManager.notify(alarmID, builder.build());

	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
