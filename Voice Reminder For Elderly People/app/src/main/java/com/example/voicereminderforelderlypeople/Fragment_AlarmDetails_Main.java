package com.example.voicereminderforelderlypeople;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.app.Activity.RESULT_OK;

import com.example.voicereminderforelderlypeople.R;

public class Fragment_AlarmDetails_Main extends Fragment implements View.OnClickListener,
		SeekBar.OnSeekBarChangeListener, TimePicker.OnTimeChangedListener, AdapterView.OnItemSelectedListener {

	private static final int REQUEST_AUDIO_PERMISSION_CODE = 101;

	MediaRecorder mediaRecorder;
	boolean isRecording = false;

	int seconds = 0;
	String path = null;
	int dummySeconds = 0;
	int playableSeconds = 0;

	ExecutorService executorService = Executors.newSingleThreadExecutor();

	private static final int RINGTONE_REQUEST_CODE = 5280;

	private ViewModel_AlarmDetails viewModel;

	private FragmentGUIListener listener;
	private TextView alarmDateTV, alarmMessageTV;
	private boolean isSavedInstanceStateNull;

	public interface FragmentGUIListener {

		void onSaveButtonClick();

		void onRequestDatePickerFragCreation();

		void onRequestMessageFragCreation();

		void onCancelButtonClick();

	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof FragmentGUIListener) {
			listener = (FragmentGUIListener) context;
		} else {
			throw new ClassCastException(context.getClass().getSimpleName() + " must implement Fragment_AlarmDetails_Main.FragmentGUIListener.");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isSavedInstanceStateNull = savedInstanceState == null;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.frag_alarm_details_main, container, false);

		viewModel = new ViewModelProvider(requireActivity()).get(ViewModel_AlarmDetails.class);

		TimePicker timePicker = view.findViewById(R.id.addAlarmTimePicker);
		ConstraintLayout alarmDateConstraintLayout = view.findViewById(R.id.alarmDateConstraintLayout);
		ConstraintLayout alarmToneConstraintLayout = view.findViewById(R.id.alarmToneConstraintLayout);
		ConstraintLayout alarmMessageConstraintLayout = view.findViewById(R.id.alarmMessageConstraintLayout);
		Button saveButton = view.findViewById(R.id.saveButton);
		Button cancelButton = view.findViewById(R.id.cancelButton);
		alarmDateTV = view.findViewById(R.id.alarmDateTextView);
		alarmMessageTV = view.findViewById(R.id.textView_alarmMessage);
		Button speakButton = view.findViewById(R.id.speakButton);
		ImageView recordButton = view.findViewById(R.id.recordButton);

		speakButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking");
				startActivityForResult(intent, 100);
			}
		});

		recordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(checkRecordingPermission()){
					if(!isRecording){
						isRecording = true;
						executorService.execute(new Runnable() {
							@Override
							public void run() {
								mediaRecorder = new MediaRecorder();
								mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
								mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
								mediaRecorder.setOutputFile(getRecordingFilePath());
								path = getRecordingFilePath();
								mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

								try {
									mediaRecorder.prepare();
								} catch (IOException e) {
									e.printStackTrace();
								}

								mediaRecorder.start();
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										recordButton.setImageDrawable(ResourcesCompat.getDrawable(requireActivity().getResources(), R.drawable.ic_pause, null));
									}
								});
							}
						});
					}else{
						executorService.execute(new Runnable() {
							@Override
							public void run() {
								mediaRecorder.stop();
								mediaRecorder.release();
								mediaRecorder = null;
								playableSeconds = seconds;
								dummySeconds = seconds;
								seconds = 0;
								isRecording = false;
								recordButton.setImageDrawable(ResourcesCompat.getDrawable(requireActivity().getResources(), R.drawable.recording_in_active, null));
							}
						});
					}
				}else{
					requestRecordingPermission();
				}
			}
		});

		timePicker.setIs24HourView(DateFormat.is24HourFormat(requireContext()));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			timePicker.setHour(viewModel.getAlarmDateTime().getHour());
			timePicker.setMinute(viewModel.getAlarmDateTime().getMinute());
		} else {
			timePicker.setCurrentHour(viewModel.getAlarmDateTime().getHour());
			timePicker.setCurrentMinute(viewModel.getAlarmDateTime().getMinute());
		}

		setDate();

		displayAlarmMessage();

		timePicker.setOnTimeChangedListener(this);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			try {
				ViewGroup amPmView;
				ViewGroup v1 = (ViewGroup) timePicker.getChildAt(0);
				ViewGroup v2 = (ViewGroup) v1.getChildAt(0);
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
					ViewGroup v3 = (ViewGroup) v2.getChildAt(0);
					amPmView = (ViewGroup) v3.getChildAt(3);
				} else {
					amPmView = (ViewGroup) v2.getChildAt(3);
				}
				View.OnClickListener listener = v -> timePicker.setCurrentHour((timePicker.getCurrentHour() + 12) % 24);

				View am = amPmView.getChildAt(0);
				View pm = amPmView.getChildAt(1);

				am.setOnClickListener(listener);
				pm.setOnClickListener(listener);
			} catch (Exception ignored) {
			}
		}

		saveButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		alarmDateConstraintLayout.setOnClickListener(this);
		alarmToneConstraintLayout.setOnClickListener(this);
		alarmMessageConstraintLayout.setOnClickListener(this);

		if (isSavedInstanceStateNull) {
			isSavedInstanceStateNull = false;
		}

		return view;
	}

	private void requestRecordingPermission(){
		requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
	}

	public boolean checkRecordingPermission(){
		if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
			requestRecordingPermission();;
			return false;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(requestCode == REQUEST_AUDIO_PERMISSION_CODE){
			if(grantResults.length > 0){
				boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
				if(permissionToRecord){
					Toast.makeText(getContext(), "Permission Given", Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	public String getRecordingFilePath(){
		ContextWrapper contextWrapper = new ContextWrapper(getActivity().getApplicationContext());
		File music = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
		File file = new File(music, "textFile" + ".mp3");
		return file.getPath();
	}

	private void displayAlarmMessage(){
		if (viewModel.getAlarmMessage() == null){
			alarmMessageTV.setText(R.string.alarmMessage_default);
		} else {
			alarmMessageTV.setText(viewModel.getAlarmMessage());
		}
	}

	private void setDate() {
		alarmDateTV.setText(viewModel.getAlarmDateTime().format(DateTimeFormatter.ofPattern("dd MMMM, yyyy")));
	}

	@Override
	public void onClick(View view) {

		if (view.getId() == R.id.saveButton) {
			saveButtonClicked();
		} else if (view.getId() == R.id.cancelButton) {
			listener.onCancelButtonClick();
		} else if (view.getId() == R.id.alarmDateConstraintLayout) {
			listener.onRequestDatePickerFragCreation();
		} else if (view.getId() == R.id.alarmToneConstraintLayout) {

		} else if (view.getId() == R.id.alarmMessageConstraintLayout){
			listener.onRequestMessageFragCreation();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == 100 && resultCode == RESULT_OK){
			assert data != null;
			ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			viewModel.setAlarmMessage(result.get(0));
			displayAlarmMessage();
		}


		if (requestCode == RINGTONE_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {

				assert data != null;
				Uri uri = Objects.requireNonNull(data.getExtras()).getParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				assert uri != null;
				viewModel.setAlarmToneUri(uri);
			}
		}
	}

	@Override
	public void onTimeChanged(TimePicker timePicker, int hourOfDay, int minute) {

		viewModel.setAlarmDateTime(viewModel.getAlarmDateTime().withHour(hourOfDay));
		viewModel.setAlarmDateTime(viewModel.getAlarmDateTime().withMinute(minute));

		if (viewModel.getIsChosenDateToday()) {
			viewModel.setAlarmDateTime(LocalDateTime.of(LocalDate.now(), viewModel.getAlarmDateTime().toLocalTime()));

			if (! viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				viewModel.setAlarmDateTime(viewModel.getAlarmDateTime().plusDays(1));
				viewModel.setIsChosenDateToday(false);
				viewModel.setHasUserChosenDate(false);
			}

			viewModel.setMinDate(viewModel.getAlarmDateTime().toLocalDate());

		} else {
			if (! viewModel.getHasUserChosenDate()) {
				if (viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
					viewModel.setAlarmDateTime(LocalDateTime.of(LocalDate.now(),
							viewModel.getAlarmDateTime().toLocalTime()));
					viewModel.setIsChosenDateToday(true);
				}
			}

			if (! viewModel.getAlarmDateTime().toLocalTime().isAfter(LocalTime.now())) {
				viewModel.setMinDate(LocalDate.now().plusDays(1));
			} else {
				viewModel.setMinDate(LocalDate.now());
			}
		}

		setDate();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		viewModel.setAlarmVolume(progress);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
		viewModel.setAlarmType(position);
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
	}

	private void saveButtonClicked() {
		SharedPreferences sharedPreferences = requireContext()
				.getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);

		if (sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_AUTO_SET_TONE, true)) {

			sharedPreferences.edit()
					.remove(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI)
					.putString(ConstantsAndStatics.SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI,
							viewModel.getAlarmToneUri().toString())
					.commit();
		}

		listener.onSaveButtonClick();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

}
