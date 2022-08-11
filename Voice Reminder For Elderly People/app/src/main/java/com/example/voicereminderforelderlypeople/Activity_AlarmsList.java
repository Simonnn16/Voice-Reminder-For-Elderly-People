package com.example.voicereminderforelderlypeople;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.example.voicereminderforelderlypeople.R;

public class Activity_AlarmsList extends AppCompatActivity implements AlarmAdapter.AdapterInterface {

	private AlarmAdapter alarmAdapter;
	private RecyclerView alarmsRecyclerView;
	private AlarmDatabase alarmDatabase;
	private ViewModel_AlarmsList viewModel;

	private ViewStub viewStub;

	private static final int NEW_ALARM_REQUEST_CODE = 2564;

	private static final int EXISTING_ALARM_REQUEST_CODE = 3178;

	private static final int MODE_ADD_NEW_ALARM = 103;
	private static final int MODE_ACTIVATE_EXISTING_ALARM = 604;

	private static final int MODE_DELETE_ALARM = 504;
	private static final int MODE_DEACTIVATE_ONLY = 509;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarmslist);
		setSupportActionBar(findViewById(R.id.toolbar));

		alarmDatabase = AlarmDatabase.getInstance(this);
		viewModel = new ViewModelProvider(this).get(ViewModel_AlarmsList.class);
		viewModel.init(alarmDatabase);

		Button addAlarmButton = findViewById(R.id.addAlarmButton);
		addAlarmButton.setOnClickListener(view -> {
			Intent intent = new Intent(this, Activity_AlarmDetails.class);
			intent.setAction(ConstantsAndStatics.ACTION_NEW_ALARM);
			startActivityForResult(intent, NEW_ALARM_REQUEST_CODE);
		});

		alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
		alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		viewStub = findViewById(R.id.viewStub);

		initialiseRecyclerView();

		manageViewStub(viewModel.getAlarmsCount(alarmDatabase));

		viewModel.getLiveAlarmsCount().observe(this, this::manageViewStub);

		if (getIntent().getAction() != null) {

			if (getIntent().getAction().equals(ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT)) {

				Intent intent = new Intent(this, Activity_AlarmDetails.class);
				intent.setAction(ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT);

				if (getIntent().getExtras() != null) {
					intent.putExtras(getIntent().getExtras());
				}

				startActivityForResult(intent, NEW_ALARM_REQUEST_CODE);
			}
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	private void manageViewStub(int count) {
		if (count == 0) {
			viewStub.setVisibility(View.VISIBLE);
		} else {
			viewStub.setVisibility(View.GONE);
		}
	}

	private void initialiseRecyclerView() {
		alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
		alarmsRecyclerView.setAdapter(alarmAdapter);
	}

	private void addOrActivateAlarm(int mode, AlarmEntity alarmEntity, @Nullable ArrayList<Integer> repeatDays) {

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		if (repeatDays != null) {
			Collections.sort(repeatDays);
		}

		LocalDateTime alarmDateTime = ConstantsAndStatics.getAlarmDateTime(LocalDate.of(alarmEntity.alarmYear,
				alarmEntity.alarmMonth, alarmEntity.alarmDay), LocalTime.of(alarmEntity.alarmHour,
				alarmEntity.alarmMinutes), alarmEntity.isRepeatOn, repeatDays);

		int alarmID;
		if (mode == MODE_ADD_NEW_ALARM) {

			int[] result = viewModel.addAlarm(alarmDatabase, alarmEntity, repeatDays);

			alarmID = result[0];

			alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
			alarmsRecyclerView.swapAdapter(alarmAdapter, false);
			alarmsRecyclerView.scrollToPosition(result[1]);

		} else {

			viewModel.toggleAlarmState(alarmDatabase, alarmEntity.alarmHour, alarmEntity.alarmMinutes, 1);
			alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
			alarmsRecyclerView.swapAdapter(alarmAdapter, false);
			alarmID = alarmEntity.alarmID;
		}

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

		Bundle data = alarmEntity.getAlarmDetailsInABundle();
		data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, repeatDays);
		data.remove(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID, alarmID);
		intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), alarmID, intent, 0);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime.withSecond(0), ZoneId.systemDefault());

		alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000, pendingIntent), pendingIntent);

		Toast.makeText(this, getString(R.string.toast_alarmSwitchedOn,
				getDuration(Duration.between(ZonedDateTime.now(ZoneId.systemDefault()).withSecond(0), zonedDateTime))), Toast.LENGTH_LONG).show();

		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	private void deleteOrDeactivateAlarm(int mode, int hour, int mins) {

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
				.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
				.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

		int alarmID = viewModel.getAlarmId(alarmDatabase, hour, mins);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), alarmID, intent, PendingIntent.FLAG_NO_CREATE);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}

		ConstantsAndStatics.killServices(this, alarmID);

		DateTimeFormatter formatter;
		if (DateFormat.is24HourFormat(this)) {
			formatter = DateTimeFormatter.ofPattern("HH:mm");
		} else {
			formatter = DateTimeFormatter.ofPattern("hh:mm a");
		}
		LocalTime alarmTime = LocalTime.of(hour, mins);

		if (mode == MODE_DELETE_ALARM) {
			viewModel.removeAlarm(alarmDatabase, hour, mins);
			alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
			alarmsRecyclerView.swapAdapter(alarmAdapter, false);

			Toast.makeText(this, getString(R.string.toast_alarmDeleted, alarmTime.format(formatter)), Toast.LENGTH_LONG).show();
		} else {
			viewModel.toggleAlarmState(alarmDatabase, hour, mins, 0);
			Toast.makeText(this, getString(R.string.toast_alarmSwitchedOff, alarmTime.format(formatter)), Toast.LENGTH_LONG).show();
		}

		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	private void toggleAlarmState(int hour, int mins, final int newAlarmState) {

		ConstantsAndStatics.killServices(this, viewModel.getAlarmId(alarmDatabase, hour, mins));

		if (newAlarmState == 0) {
			deleteOrDeactivateAlarm(MODE_DEACTIVATE_ONLY, hour, mins);
		} else {
			addOrActivateAlarm(MODE_ACTIVATE_EXISTING_ALARM, viewModel.getAlarmEntity(alarmDatabase, hour, mins),
					viewModel.getRepeatDays(alarmDatabase, hour, mins));
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == NEW_ALARM_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {

				if (intent != null) {

					Bundle data = Objects.requireNonNull(intent.getExtras()).getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
					assert data != null;

					if (viewModel.getAlarmId(alarmDatabase, data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE)) != 0) {

						deleteOrDeactivateAlarm(MODE_DELETE_ALARM,
								data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
								data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));
					}

					AlarmEntity alarmEntity = new AlarmEntity(data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE),
							true,
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
							data.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI),
							data.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE),
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

					addOrActivateAlarm(MODE_ADD_NEW_ALARM, alarmEntity,
							data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));
				}
			}
		} else if (requestCode == EXISTING_ALARM_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {

				if (intent != null) {

					Bundle data = Objects.requireNonNull(intent.getExtras())
					                     .getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
					assert data != null;

					deleteOrDeactivateAlarm(MODE_DELETE_ALARM,
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_HOUR),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_MINUTE));

					AlarmEntity alarmEntity = new AlarmEntity(
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE),
							true,
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
							data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
							data.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI),
							data.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE),
							data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

					addOrActivateAlarm(MODE_ADD_NEW_ALARM, alarmEntity,
							data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));

				}
			}
		}
	}

	@Override
	public void onOnOffButtonClick(int rowNumber, int hour, int mins, int newAlarmState) {
		toggleAlarmState(hour, mins, newAlarmState);
	}

	@Override
	public void onDeleteButtonClicked(int rowNumber, int hour, int mins) {
		deleteOrDeactivateAlarm(MODE_DELETE_ALARM, hour, mins);
	}

	@Override
	public void onItemClicked(int rowNumber, int hour, int mins) {

		Context context = this;
		AppCompatActivity activity = this;

		final String KEY_START_ACTIVITY = "startTheActivity";

		Handler handler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(@NonNull Message msg) {
				Bundle data = msg.getData();
				if (data.getBoolean(KEY_START_ACTIVITY)) {

					Bundle bundle = data.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
					assert bundle != null;
					bundle.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
							data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));

					Intent intent = new Intent(context, Activity_AlarmDetails.class)
							.setAction(ConstantsAndStatics.ACTION_EXISTING_ALARM)
							.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, bundle);
					activity.startActivityForResult(intent, EXISTING_ALARM_REQUEST_CODE);
				}
			}
		};

		Thread thread = new Thread(() -> {
			Looper.prepare();

			Bundle bundle = new Bundle();

			List<AlarmEntity> list = alarmDatabase.alarmDAO().getAlarmDetails(hour, mins);
			for (AlarmEntity entity : list) {
				bundle.putBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, entity.getAlarmDetailsInABundle());
				bundle.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
						new ArrayList<>(alarmDatabase.alarmDAO().getAlarmRepeatDays(entity.alarmID)));
			}
			bundle.putBoolean(KEY_START_ACTIVITY, true);

			Message message = Message.obtain();
			message.setData(bundle);
			handler.sendMessageAtFrontOfQueue(message);
		});
		thread.start();

	}

	@NonNull
	private String getDuration(@NonNull Duration duration) {

		NumberFormat numFormat = NumberFormat.getInstance();
		numFormat.setGroupingUsed(false);

		long days = duration.toDays();
		duration = duration.minusDays(days);

		long hours = duration.toHours();
		duration = duration.minusHours(hours);

		long minutes = duration.toMinutes();

		String msg;

		if (days == 0) {
			if (hours == 0) {
				msg = numFormat.format(minutes) + getResources().getQuantityString(R.plurals.mins, (int) minutes);
			} else {
				msg = numFormat.format(hours) + getResources().getQuantityString(R.plurals.hour, (int) hours)
						+ getString(R.string.and)
						+ numFormat.format(minutes) + getResources().getQuantityString(R.plurals.mins, (int) minutes);
			}
		} else {
			msg = numFormat.format(days) + getResources().getQuantityString(R.plurals.day, (int) days) + ", "
					+ numFormat.format(hours) + getResources().getQuantityString(R.plurals.hour, (int) hours)
					+ getString(R.string.and)
					+ numFormat.format(minutes) + " " + getResources().getQuantityString(R.plurals.mins, (int) minutes);
		}
		return msg;
	}

}