package com.example.voicereminderforelderlypeople;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class AlarmData {

	private boolean isSwitchedOn;
	private LocalDateTime alarmDateTime;
	private LocalTime alarmTime;
	private boolean isRepeatOn;
	private ArrayList<Integer> repeatDays;
	private String alarmMessage;

	public AlarmData(boolean isSwitchedOn, @NonNull LocalDateTime alarmDateTime, @Nullable String alarmMessage) {
		this.isSwitchedOn = isSwitchedOn;
		this.alarmDateTime = alarmDateTime;
		this.isRepeatOn = false;
		this.repeatDays = null;
		this.alarmTime = alarmDateTime.toLocalTime();
		this.alarmMessage = alarmMessage;
	}

	public AlarmData(boolean isSwitchedOn, @NonNull LocalTime alarmTime, @Nullable String alarmMessage,
	                 @NonNull ArrayList<Integer> repeatDays) {
		this.isSwitchedOn = isSwitchedOn;
		this.alarmTime = alarmTime;
		this.isRepeatOn = true;
		this.repeatDays = repeatDays;
		this.alarmMessage = alarmMessage;
		this.alarmDateTime = null;
	}

	public boolean isRepeatOn() {
		return isRepeatOn;
	}

	@Nullable
	public ArrayList<Integer> getRepeatDays() {
		return repeatDays;
	}

	public void setRepeatDays(@Nullable ArrayList<Integer> repeatDays) {
		this.repeatDays = repeatDays;
	}

	public boolean isSwitchedOn() {
		return isSwitchedOn;
	}

	public void setSwitchedOn(boolean switchedOn) {
		isSwitchedOn = switchedOn;
	}

	@Nullable
	public LocalDateTime getAlarmDateTime() {
		return alarmDateTime;
	}

	public void setAlarmDateTime(@Nullable LocalDateTime alarmDateTime) {
		this.alarmDateTime = alarmDateTime;
	}

	@NonNull
	public LocalTime getAlarmTime() {
		return alarmTime;
	}

	public void setAlarmMessage(@Nullable String alarmMessage) {
		this.alarmMessage = alarmMessage;
	}

	@Nullable
	public String getAlarmMessage() {
		return alarmMessage;
	}

}
