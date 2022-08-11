package com.example.voicereminderforelderlypeople;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlarmDAO {

	@Insert(entity = AlarmEntity.class, onConflict = OnConflictStrategy.REPLACE)
	void addAlarm(AlarmEntity alarmEntity);

	@Query("DELETE FROM alarm_entity WHERE alarmHour = :hour AND alarmMinutes = :mins")
	void deleteAlarm(int hour, int mins);

	@Query("SELECT * FROM alarm_entity ORDER BY alarmHour, alarmMinutes")
	List<AlarmEntity> getAlarms();

	@Query("SELECT * FROM alarm_entity WHERE alarmHour = :hour AND alarmMinutes = :mins")
	List<AlarmEntity> getAlarmDetails(int hour, int mins);

	@Query("UPDATE alarm_entity SET alarmDay = :newDayOfMonth, alarmMonth = :newMonth, alarmYear = :newYear WHERE " +
			"alarmHour = :hour AND alarmMinutes = :mins")
	void updateAlarmDate(int hour, int mins, int newDayOfMonth, int newMonth, int newYear);

	@Query("UPDATE alarm_entity SET isAlarmOn = :newAlarmState WHERE alarmID = :alarmId")
	void toggleAlarm(int alarmId, int newAlarmState);

	@Query("SELECT repeatDay from alarm_repeat_entity WHERE alarmID = :alarmId")
	List<Integer> getAlarmRepeatDays(int alarmId);

	@Insert(entity = RepeatEntity.class, onConflict = OnConflictStrategy.REPLACE)
	void insertRepeatData(RepeatEntity repeatEntity);

	@Query("SELECT * FROM alarm_entity WHERE isAlarmOn = 1 ORDER BY alarmHour, alarmMinutes")
	List<AlarmEntity> getActiveAlarms();

	@Query("SELECT alarmID FROM alarm_entity WHERE alarmHour = :hour AND alarmMinutes = :mins")
	int getAlarmId(int hour, int mins);

	@Query("SELECT COUNT(*) FROM alarm_entity")
	int getNumberOfAlarms();

	@Query("UPDATE alarm_entity SET hasUserChosenDate = :newState WHERE alarmID = :alarmId")
	void toggleHasUserChosenDate(int alarmId, int newState);

}
