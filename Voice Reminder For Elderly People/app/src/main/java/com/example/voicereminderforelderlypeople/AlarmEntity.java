package com.example.voicereminderforelderlypeople;

import android.net.Uri;
import android.os.Bundle;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarm_entity", indices = {@Index(value = {"alarmHour", "alarmMinutes"}, unique = true)})
public class AlarmEntity {

	@PrimaryKey(autoGenerate = true)
	@ColumnInfo
	public int alarmID;

	public int alarmHour;

	public int alarmMinutes;

	public boolean isAlarmOn;

	public int alarmDay;

	public int alarmMonth;

	public int alarmYear;

	public boolean isSnoozeOn;

	public int snoozeTimeInMinutes;

	public int snoozeFrequency;

	public int alarmVolume;

	public boolean isRepeatOn;

	public int alarmType;

	public Uri alarmTone;

	public boolean hasUserChosenDate;

	public String alarmMessage;

	public AlarmEntity(int alarmHour, int alarmMinutes, boolean isAlarmOn, int alarmDay, int alarmMonth, int alarmYear, Uri alarmTone, String alarmMessage,
	                   boolean hasUserChosenDate) {

		this.alarmHour = alarmHour;
		this.alarmMinutes = alarmMinutes;
		this.isAlarmOn = isAlarmOn;
		this.alarmDay = alarmDay;
		this.alarmMonth = alarmMonth;
		this.alarmYear = alarmYear;
		this.alarmTone = alarmTone;
		this.alarmMessage = alarmMessage;
		this.hasUserChosenDate = hasUserChosenDate;
	}

	@Ignore
	public Bundle getAlarmDetailsInABundle() {
		Bundle data = new Bundle();
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID, alarmID);
		data.putBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_ALARM_ON, isAlarmOn);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR, alarmHour);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE, alarmMinutes);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY, alarmDay);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH, alarmMonth);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR, alarmYear);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE, alarmType);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME, alarmVolume);
		data.putInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS, snoozeTimeInMinutes);
		data.putParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI, alarmTone);
		data.putString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE, alarmMessage);
		data.putBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE, hasUserChosenDate);
		return data;
	}

}
