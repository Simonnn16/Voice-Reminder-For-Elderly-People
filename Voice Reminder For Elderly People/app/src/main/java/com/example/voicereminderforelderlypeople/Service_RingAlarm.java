package com.example.voicereminderforelderlypeople;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import in.basulabs.audiofocuscontroller.AudioFocusController;

public class Service_RingAlarm extends Service implements AudioFocusController.OnAudioFocusChangeListener {

	private Bundle alarmDetails;

	private MediaPlayer mediaPlayer;

	private static final int NOTIFICATION_ID = 20153;

	private AlarmDatabase alarmDatabase;

	private CountDownTimer ringTimer;
	private Vibrator vibrator;
	private AudioManager audioManager;
	private NotificationManager notificationManager;

	private boolean isPlaying = false;
	private File fileToPlay;

	private int initialAlarmStreamVolume;

	public static int alarmID = -1;

	public static boolean isThisServiceRunning = false;

	private SharedPreferences sharedPreferences;

	private boolean preMatureDeath;

	private ArrayList<Integer> repeatDays;

	private AudioFocusController audioFocusController;

	private boolean alarmRingingStarted;

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_SNOOZE_ALARM)) {
			} else if (Objects.equals(intent.getAction(), ConstantsAndStatics.ACTION_CANCEL_ALARM)) {
				dismissAlarm();
			}
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		alarmDetails = Objects.requireNonNull(Objects.requireNonNull(intent.getExtras()).getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(NOTIFICATION_ID, buildRingNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
		} else {
			startForeground(NOTIFICATION_ID, buildRingNotification());
		}
		isThisServiceRunning = true;
		preMatureDeath = true;
		alarmRingingStarted = false;

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

		audioFocusController = new AudioFocusController.Builder(this)
				.setAcceptsDelayedFocus(true)
				.setAudioFocusChangeListener(this)
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.setUsage(AudioAttributes.USAGE_ALARM)
				.setPauseWhenAudioIsNoisy(false)
				.setStream(AudioManager.STREAM_ALARM)
				.setDurationHint(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
				.build();

		alarmID = alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);

		ringTimer = new CountDownTimer(60000, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
			}

			@Override
			public void onFinish() {
			}
		};

		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		assert vibrator != null;
		assert audioManager != null;
		assert notificationManager != null;

		alarmDatabase = AlarmDatabase.getInstance(this);

		initialAlarmStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConstantsAndStatics.ACTION_SNOOZE_ALARM);
		intentFilter.addAction(ConstantsAndStatics.ACTION_CANCEL_ALARM);
		registerReceiver(broadcastReceiver, intentFilter);

		audioFocusController.requestFocus();

		loadRepeatDays();

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (preMatureDeath) {
			dismissAlarm();
		}

		try {
			ringTimer.cancel();
			vibrator.cancel();
			if (mediaPlayer != null) {
				mediaPlayer.stop();
				mediaPlayer.release();
			}
		} catch (Exception ignored) {
		}

		audioManager.setStreamVolume(AudioManager.STREAM_ALARM, initialAlarmStreamVolume, 0);
		unregisterReceiver(broadcastReceiver);
		isThisServiceRunning = false;
		alarmID = -1;
	}

	private void loadRepeatDays() {
		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON)) {
			AlarmDatabase alarmDatabase = AlarmDatabase.getInstance(this);
			Thread thread = new Thread(() -> repeatDays = new ArrayList<>(
					alarmDatabase.alarmDAO().getAlarmRepeatDays(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID))));
			thread.start();
		} else {
			repeatDays = null;
		}
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel channel = new NotificationChannel(Integer.toString(NOTIFICATION_ID), "com.example.voicereminderforelderlypeople Notifications",
					importance);
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			channel.setSound(null, null);
			assert notificationManager != null;
			notificationManager.createNotificationChannel(channel);
		}
	}

	private Notification buildRingNotification() {
		createNotificationChannel();

		String alarmMessage = alarmDetails.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE, null);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Integer.toString(NOTIFICATION_ID))
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText("Initialising alarm...")
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.setSmallIcon(R.drawable.ic_reminder_logo);

		if (alarmMessage != null) {
			builder.setContentTitle(getString(R.string.app_name))
			       .setContentText(alarmMessage)
			       .setStyle(new NotificationCompat.BigTextStyle().bigText(alarmMessage));
		} else {
			builder.setContentText(getString(R.string.notifContent_ring));
		}


		return builder.build();
	}

	private void ringAlarm() {
		mediaPlayer = null;

		ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
		File music = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
		File file = new File(music, "textFile" + ".mp3");
		file.getPath();

		if(isPlaying){
			stopAudio();
		}else{
			fileToPlay = file;
		}
		playAudio(fileToPlay);

		notificationManager.notify(NOTIFICATION_ID, buildRingNotification());

		ringTimer.start();
	}

	private void stopAudio() {
		isPlaying = false;
	}

	private void playAudio(File fileToPlay) {
		mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		isPlaying = true;
	}

	private void dismissAlarm() {

		stopRinging();
		cancelPendingIntent();

		Thread thread_toggleAlarm =
				new Thread(() -> alarmDatabase.alarmDAO().toggleAlarm(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID), 0));

		if (alarmDetails.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON, false) && repeatDays != null && repeatDays.size() > 0) {

			LocalTime alarmTime = LocalTime.of(alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
					alarmDetails.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));

			Collections.sort(repeatDays);

			LocalDateTime alarmDateTime = LocalDateTime.of(LocalDate.now(), alarmTime);
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
			setAlarm(alarmDateTime);

		} else {

			thread_toggleAlarm.start();

			try {
				thread_toggleAlarm.join();
			} catch (InterruptedException ignored) {
			}
		}

		ConstantsAndStatics.schedulePeriodicWork(this);
		preMatureDeath = false;
		stopForeground(true);
		stopSelf();

	}

	private void stopRinging() {
		try {
			ringTimer.cancel();

			if ((alarmDetails
					.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) == ConstantsAndStatics.ALARM_TYPE_VIBRATE_ONLY) || (alarmDetails
					.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE) == ConstantsAndStatics.ALARM_TYPE_SOUND_AND_VIBRATE)) {
				vibrator.cancel();
			}
			if (mediaPlayer != null) {
				mediaPlayer.stop();
			}
		} catch (Exception ignored) {
		} finally {
			Intent intent = new Intent(ConstantsAndStatics.ACTION_DESTROY_RING_ALARM_ACTIVITY);
			sendBroadcast(intent);
		}
		audioFocusController.abandonFocus();
	}

	private void setAlarm(@NonNull LocalDateTime alarmDateTime) {

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
				.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
				.setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
				.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), alarmID, intent, 0);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime.withSecond(0), ZoneId.systemDefault());

		alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000, pendingIntent), pendingIntent);
	}

	private void cancelPendingIntent() {

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
				.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
				.setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
				.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, alarmDetails);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), alarmID, intent, PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void decreaseVolume() {
	}

	@Override
	public void increaseVolume() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
		if (!alarmRingingStarted) {
			alarmRingingStarted = true;
			ringAlarm();
		}
	}

}
