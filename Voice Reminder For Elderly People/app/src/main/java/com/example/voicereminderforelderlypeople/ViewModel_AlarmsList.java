package com.example.voicereminderforelderlypeople;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ViewModel_AlarmsList extends ViewModel {

	private MutableLiveData<ArrayList<AlarmData>> alarmDataArrayList;
	private MutableLiveData<Integer> alarmsCount;

	public LiveData<Integer> getLiveAlarmsCount() {
		return alarmsCount;
	}

	private void incrementAlarmsCount() {
		if (alarmsCount == null || alarmsCount.getValue() == null) {
			alarmsCount = new MutableLiveData<>();
			alarmsCount.setValue(1);
		} else {
			alarmsCount.setValue(alarmsCount.getValue() + 1);
		}
	}

	private void decrementAlarmsCount() {
		if ((alarmsCount != null) && (alarmsCount.getValue() != null) && (alarmsCount.getValue() > 0)) {
			alarmsCount.setValue(alarmsCount.getValue() - 1);
		}
	}

	public int getAlarmsCount(AlarmDatabase alarmDatabase) {

		AtomicInteger count = new AtomicInteger(0);

		Thread thread = new Thread(() -> count.set(alarmDatabase.alarmDAO().getNumberOfAlarms()));
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		alarmsCount = new MutableLiveData<>(count.get());

		return count.get();
	}

	private void init(AlarmDatabase alarmDatabase, boolean force) {

		if (alarmDataArrayList == null || alarmDataArrayList.getValue() == null || force) {

			alarmDataArrayList = new MutableLiveData<>(new ArrayList<>());

			Thread thread = new Thread(() -> {

				List<AlarmEntity> alarmEntityList = alarmDatabase.alarmDAO().getAlarms();

				if (alarmEntityList != null) {

					for (AlarmEntity entity : alarmEntityList) {

						LocalDateTime alarmDateTime;

						if (!entity.isRepeatOn && !entity.isAlarmOn) {

							alarmDateTime = LocalDateTime.of(entity.alarmYear, entity.alarmMonth, entity.alarmDay, entity.alarmHour,
									entity.alarmMinutes);

							if (alarmDateTime.isBefore(LocalDateTime.now())) {
								while (alarmDateTime.isBefore(LocalDateTime.now())) {
									alarmDateTime = alarmDateTime.plusDays(1);
								}
								alarmDatabase.alarmDAO()
								             .updateAlarmDate(entity.alarmHour, entity.alarmMinutes,
										             alarmDateTime.getDayOfMonth(),
										             alarmDateTime.getMonthValue(),
										             alarmDateTime.getYear());
								alarmDatabase.alarmDAO().toggleHasUserChosenDate(entity.alarmID, 0);
							}
						}
					}

					alarmEntityList = alarmDatabase.alarmDAO().getAlarms();

					for (AlarmEntity entity : alarmEntityList) {

						LocalDateTime alarmDateTime = LocalDateTime.of(entity.alarmYear, entity.alarmMonth,
								entity.alarmDay, entity.alarmHour, entity.alarmMinutes);

						ArrayList<Integer> repeatDays = entity.isRepeatOn ? new ArrayList<>(alarmDatabase.alarmDAO()
						                                                                                 .getAlarmRepeatDays(entity.alarmID)) : null;

						Objects.requireNonNull(alarmDataArrayList.getValue()).add(getAlarmDataObject(entity, alarmDateTime, repeatDays));

					}
				}

			});

			thread.start();

			if (force) {
				try {
					thread.join();
				} catch (InterruptedException ignored) {
				}
			}

		}

	}

	public void init(AlarmDatabase alarmDatabase) {
		init(alarmDatabase, false);
	}

	public ArrayList<AlarmData> getAlarmDataArrayList() {
		if (alarmDataArrayList.getValue() == null) {
			return new ArrayList<>();
		} else {
			return alarmDataArrayList.getValue();
		}
	}

	public int[] addAlarm(@NonNull AlarmDatabase alarmDatabase, @NonNull AlarmEntity alarmEntity, @Nullable ArrayList<Integer> repeatDays) {

		AtomicInteger alarmID = new AtomicInteger();

		Thread thread = new Thread(() -> {

			alarmDatabase.alarmDAO().addAlarm(alarmEntity);

			alarmID.set(alarmDatabase.alarmDAO().getAlarmId(alarmEntity.alarmHour, alarmEntity.alarmMinutes));

			if (alarmEntity.isRepeatOn && repeatDays != null) {
				Collections.sort(repeatDays);
				for (int day : repeatDays) {
					alarmDatabase.alarmDAO().insertRepeatData(new RepeatEntity(alarmID.get(), day));
				}
			}

		});
		thread.start();

		LocalDateTime alarmDateTime = LocalDateTime.of(alarmEntity.alarmYear, alarmEntity.alarmMonth,
				alarmEntity.alarmDay, alarmEntity.alarmHour, alarmEntity.alarmMinutes);

		int scrollToPosition = 0;

		AlarmData newAlarmData = getAlarmDataObject(alarmEntity, alarmDateTime, repeatDays);

		if (alarmDataArrayList.getValue() == null || alarmDataArrayList.getValue().size() == 0) {

			alarmDataArrayList = new MutableLiveData<>(new ArrayList<>());
			Objects.requireNonNull(alarmDataArrayList.getValue()).add(newAlarmData);

		} else {

			int index = isAlarmInTheList(alarmEntity.alarmHour, alarmEntity.alarmMinutes);
			if (index != -1) {
				alarmDataArrayList.getValue().remove(index);
			}

			for (int i = 0; i < Objects.requireNonNull(alarmDataArrayList.getValue()).size(); i++) {

				if (alarmDataArrayList.getValue().get(i).getAlarmTime().isBefore(alarmDateTime.toLocalTime())) {

					if ((i + 1) < alarmDataArrayList.getValue().size()) {

						if (alarmDataArrayList.getValue().get(i + 1).getAlarmTime().isAfter(alarmDateTime.toLocalTime())) {
							alarmDataArrayList.getValue().add(i + 1, newAlarmData);
							scrollToPosition = i + 1;
							break;
						}
					} else {
						alarmDataArrayList.getValue().add(newAlarmData);
						scrollToPosition = alarmDataArrayList.getValue().size() - 1;
						break;
					}
				}

				if (i == alarmDataArrayList.getValue().size() - 1) {
					alarmDataArrayList.getValue().add(0, newAlarmData);
					break;
				}
			}
		}

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		incrementAlarmsCount();

		return new int[]{alarmID.get(), scrollToPosition};

	}

	private AlarmData getAlarmDataObject(@NonNull AlarmEntity entity, @NonNull LocalDateTime alarmDateTime,
	                                     @Nullable ArrayList<Integer> repeatDays) {

		if (!entity.isRepeatOn) {
			return new AlarmData(entity.isAlarmOn, alarmDateTime, entity.alarmMessage);
		} else {
			assert repeatDays != null;
			return new AlarmData(entity.isAlarmOn, alarmDateTime.toLocalTime(), entity.alarmMessage, repeatDays);
		}

	}

	public void removeAlarm(@NonNull AlarmDatabase alarmDatabase, int hour, int mins) {

		AtomicInteger alarmId = new AtomicInteger();

		Thread thread = new Thread(() -> {
			alarmId.set(alarmDatabase.alarmDAO().getAlarmId(hour, mins));
			alarmDatabase.alarmDAO().deleteAlarm(hour, mins);
		});
		thread.start();

		for (int i = 0; i < Objects.requireNonNull(alarmDataArrayList.getValue()).size(); i++) {
			AlarmData alarmData = alarmDataArrayList.getValue().get(i);

			if (alarmData.getAlarmTime().equals(LocalTime.of(hour, mins))) {
				alarmDataArrayList.getValue().remove(i);
				break;
			}
		}

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		decrementAlarmsCount();

	}

	public int toggleAlarmState(@NonNull AlarmDatabase alarmDatabase, int hour, int mins, int newAlarmState) {

		AtomicInteger alarmId = new AtomicInteger();

		Thread thread = new Thread(() -> {
			alarmId.set(alarmDatabase.alarmDAO().getAlarmId(hour, mins));

			alarmDatabase.alarmDAO().toggleAlarm(alarmId.get(), newAlarmState);
		});
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return isAlarmInTheList(hour, mins);
	}

	public int getAlarmId(@NonNull AlarmDatabase alarmDatabase, int hour, int mins) {
		AtomicInteger alarmId = new AtomicInteger(0);

		Thread thread = new Thread(() -> {
			try {
				alarmId.set(alarmDatabase.alarmDAO().getAlarmId(hour, mins));
			} catch (Exception ex) {
				alarmId.set(0);
			}
		});
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return alarmId.get();
	}

	public ArrayList<Integer> getRepeatDays(@NonNull AlarmDatabase alarmDatabase, int hour, int mins) {
		AtomicReference<ArrayList<Integer>> repeatDays = new AtomicReference<>(new ArrayList<>());

		Thread thread = new Thread(() -> repeatDays.set(new ArrayList<>(alarmDatabase.alarmDAO()
		                                                                             .getAlarmRepeatDays(getAlarmId(alarmDatabase, hour, mins)))));
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return repeatDays.get();
	}

	public AlarmEntity getAlarmEntity(@NonNull AlarmDatabase alarmDatabase, int hour, int mins) {

		AtomicReference<AlarmEntity> alarmEntity = new AtomicReference<>();

		Thread thread = new Thread(() -> alarmEntity.set(alarmDatabase.alarmDAO().getAlarmDetails(hour, mins).get(0)));
		thread.start();

		try {
			thread.join();
		} catch (InterruptedException ignored) {
		}

		return alarmEntity.get();
	}

	private int isAlarmInTheList(int hour, int mins) {

		if (alarmDataArrayList.getValue() != null && alarmDataArrayList.getValue().size() > 0) {
			for (AlarmData alarmData : alarmDataArrayList.getValue()) {
				if (alarmData.getAlarmTime().equals(LocalTime.of(hour, mins))) {
					return alarmDataArrayList.getValue().indexOf(alarmData);
				}
			}
		}

		return -1;
	}


}
