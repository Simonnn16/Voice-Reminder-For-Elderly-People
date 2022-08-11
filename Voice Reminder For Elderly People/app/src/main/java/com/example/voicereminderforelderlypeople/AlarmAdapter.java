package com.example.voicereminderforelderlypeople;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Objects;

import com.example.voicereminderforelderlypeople.R;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

	private final AlarmAdapter.AdapterInterface listener;

	private final ArrayList<AlarmData> alarmDataArrayList;

	private Context context;

	public interface AdapterInterface {

		void onOnOffButtonClick(int rowNumber, int hour, int mins, int newAlarmState);

		void onDeleteButtonClicked(int rowNumber, int hour, int mins);

		void onItemClicked(int rowNumber, int hour, int mins);
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		public ImageButton alarmOnOffImgBtn, alarmDeleteBtn;
		public TextView alarmTimeTextView, alarmDateTextView, alarmTitleTextView;
		public CardView alarmCardView;

		public ViewHolder(View view) {
			super(view);
			alarmOnOffImgBtn = view.findViewById(R.id.alarmOnOffImgBtn);
			alarmTimeTextView = view.findViewById(R.id.alarmTimeTextView);
			alarmDateTextView = view.findViewById(R.id.alarmDateTextView);
			alarmTitleTextView = view.findViewById(R.id.recyclerView_alarmTitleTextView);
			alarmDeleteBtn = view.findViewById(R.id.alarmDeleteBtn);
			alarmCardView = view.findViewById(R.id.alarmCardView);
		}
	}

	public AlarmAdapter(@NonNull ArrayList<AlarmData> alarmDataArrayList, @NonNull AlarmAdapter.AdapterInterface listener,
	                    @NonNull Context context) {
		this.alarmDataArrayList = alarmDataArrayList;
		this.listener = listener;
		this.context = context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerviewrow_alarmslist, parent, false);
		return new ViewHolder(listItem);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

		AlarmData alarmData = alarmDataArrayList.get(position);

		if (alarmData.isSwitchedOn()) {
			holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_on);
		} else {
			holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_off);
		}

		if (DateFormat.is24HourFormat(context)) {
			holder.alarmTimeTextView.setText(context.getResources().getString(R.string.time_24hour,
					alarmData.getAlarmTime().getHour(),
					alarmData.getAlarmTime().getMinute()));
		} else {
			String amPm = alarmData.getAlarmTime().getHour() < 12 ? "AM" : "PM";

			int alarmHour;

			if ((alarmData.getAlarmTime().getHour() > 0) && (alarmData.getAlarmTime().getHour() <= 12)) {
				alarmHour = alarmData.getAlarmTime().getHour();
			} else if (alarmData.getAlarmTime().getHour() > 12 && alarmData.getAlarmTime().getHour() <= 23) {
				alarmHour = alarmData.getAlarmTime().getHour() - 12;
			} else {
				alarmHour = alarmData.getAlarmTime().getHour() + 12;
			}

			holder.alarmTimeTextView.setText(context.getResources().getString(R.string.time_12hour, alarmHour,
					alarmData.getAlarmTime().getMinute(), amPm));
		}

		if (!alarmData.isRepeatOn()) {

			String nullMessage = "AlarmAdapter: alarmDateTime was null for a non-repetitive alarm.";

			int day = (Objects.requireNonNull(alarmData.getAlarmDateTime(), nullMessage).getDayOfWeek().getValue() + 1) > 7 ? 1 :
					(alarmData.getAlarmDateTime().getDayOfWeek().getValue() + 1);

			holder.alarmDateTextView.setText(context.getResources().getString(R.string.date,
					new DateFormatSymbols().getShortWeekdays()[day],
					alarmData.getAlarmDateTime().getDayOfMonth(),
					new DateFormatSymbols().getShortMonths()[alarmData.getAlarmDateTime().getMonthValue() - 1]));
		} else {
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < Objects.requireNonNull(alarmData.getRepeatDays(), "AlarmAdapter: repeatDays was null.").size(); i++) {
				int day = (alarmData.getRepeatDays().get(i) + 1) > 7 ? 1 : (alarmData.getRepeatDays().get(i) + 1);
				str.append(new DateFormatSymbols().getShortWeekdays()[day].substring(0, 3));
				if (i < alarmData.getRepeatDays().size() - 1) {
					str.append(" ");
				}
			}
			holder.alarmDateTextView.setText(str.toString());
		}

		String alarmMessage = alarmData.getAlarmMessage();
		holder.alarmTitleTextView.setText(alarmMessage == null ? "" : alarmMessage);

		holder.alarmOnOffImgBtn.setOnClickListener(view -> {
			int newAlarmState;
			if (!alarmData.isSwitchedOn()) {
				newAlarmState = 1;
				alarmData.setSwitchedOn(true);
				holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_on);
			} else {
				newAlarmState = 0;
				alarmData.setSwitchedOn(false);
				holder.alarmOnOffImgBtn.setImageResource(R.drawable.ic_alarm_off);
			}
			listener.onOnOffButtonClick(holder.getLayoutPosition(),
					alarmData.getAlarmTime().getHour(), alarmData.getAlarmTime().getMinute(), newAlarmState);
		});

		holder.alarmDeleteBtn.setOnClickListener(view -> listener.onDeleteButtonClicked(holder.getLayoutPosition(),
				alarmData.getAlarmTime().getHour(), alarmData.getAlarmTime().getMinute()));

		holder.alarmCardView.setOnClickListener(view -> listener.onItemClicked(holder.getLayoutPosition(),
				alarmData.getAlarmTime().getHour(), alarmData.getAlarmTime().getMinute()));
	}

	@Override
	public int getItemCount() {
		return alarmDataArrayList.size();
	}

}
