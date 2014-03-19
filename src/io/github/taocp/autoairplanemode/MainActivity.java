package io.github.taocp.autoairplanemode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * this is a day: 00:00 23:59
 * |--------------------------------------------------------------------------------------------------| 
 * 时刻： ^设置起、止时间  ^启用本程序nowTime  ^进入飞行模式beginAirplaneTime    ^退出飞行模式endAirplaneTime
 * 
 * @author hit.who
 */
public class MainActivity extends Activity implements OnClickListener {
	// TODO 参数移到下面
	private Button buttonStart;
	private Button buttonEnd;
	private TextView tvStartTime;
	private TextView tvEndTime;
	static final int TIME_DIALOG_ID = 1111;
	final static private long MICRO_SECOND_PER_DAY = 24 * 60 * 60 * 1000;
	private long nowTime;

	private int beginAirplaneHour;
	private int beginAirplaneMinute;
	private long beginAirplaneTime;

	private int endAirplaneHour;
	private int endAirplaneMinute;
	private long endAirplaneTime;

	private long firstTimeDelta;
	private long nextConnectTimeDelta;
	// private long nextAirplaneTimeDelta;

	private boolean isChangeStart = true;
	private ToggleButton enable;
	PendingIntent pi;
	PendingIntent piConnect;
	AlarmManager am;
	AlarmManager alarmConnect;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// TODO bad var name
		buttonStart = (Button) findViewById(R.id.button_change_start_time);
		buttonEnd = (Button) findViewById(R.id.button_change_end_time);
		tvStartTime = (TextView) findViewById(R.id.textview_start);
		tvEndTime = (TextView) findViewById(R.id.textview_end);
		enable = (ToggleButton) findViewById(R.id.toggleButton_isenable);
		// Add Button Click Listener
		addButtonClickListener();
		addToggleButtonClickListener();
		setup();
		setupConnectAlarm();
	}

	public void addToggleButtonClickListener() {
		enable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean on = ((ToggleButton) v).isChecked();
				if (on) {
					// TODO 检查参数
					setNowTime();
					setFirstTimeDelta();
					am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
							SystemClock.elapsedRealtime() + firstTimeDelta,
							1000 * 60 * 3, pi);

					setNextConnectTimeDelta();
					alarmConnect.setRepeating(
							AlarmManager.ELAPSED_REALTIME_WAKEUP,
							SystemClock.elapsedRealtime()
									+ nextConnectTimeDelta, 1000 * 60 * 3,
							piConnect);
				} else {
					am.cancel(pi);
					alarmConnect.cancel(piConnect);
				}
			}
		});
	}

	// XXX there must be a better way!
	public void addButtonClickListener() {

		buttonStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Toast.makeText(MainActivity.this, "b1",
				// Toast.LENGTH_LONG).show();
				isChangeStart = true;
				showDialog(TIME_DIALOG_ID);
			}
		});
		buttonEnd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isChangeStart = false;
				showDialog(TIME_DIALOG_ID);
			}
		});
	}

	public long aTimeInToday(int aHour, int aMinute) {
		Calendar c = Calendar.getInstance();
		int to_mintues = aMinute;
		int to_hours = aHour;
		int to_days = c.get(Calendar.DAY_OF_MONTH);
		int to_months = c.get(Calendar.MONTH) + 1;// XXX
		int to_years = c.get(Calendar.YEAR);
		String to_time = new StringBuilder().append(to_years).append('-')
				.append(to_months).append('-').append(to_days).append(' ')
				.append(to_hours).append(':').append(to_mintues).append(':')
				.append("00").toString();
		SimpleDateFormat sDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date date = sDate.parse(to_time);
			return date.getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(MainActivity.this, "=.=~ error !", Toast.LENGTH_LONG)
					.show();
			return 0L;
		}
	}

	private void setBeginTime() {
		beginAirplaneTime = aTimeInToday(this.beginAirplaneHour,
				this.beginAirplaneMinute);
	}

	private void setEndAirplaneTime() {
		endAirplaneTime = aTimeInToday(this.endAirplaneHour,
				this.endAirplaneMinute);
	}

	private void setNowTime() {
		nowTime = System.currentTimeMillis();
	}

	// 从开启定时器到进入飞行模式的时间差
	private void setFirstTimeDelta() {
		if (beginAirplaneTime < nowTime) {
			// TODO note user about this
			beginAirplaneTime += MICRO_SECOND_PER_DAY;// 设定的时间在今天已过，顺延一天
		}
		firstTimeDelta = beginAirplaneTime - nowTime;
	}

	// TODO rename
	// 从开启定时器到退出飞行模式的时间差
	private void setNextConnectTimeDelta() {
		nextConnectTimeDelta = endAirplaneTime - nowTime;
	}

	private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minutes) {
			String showTime;
			if (isChangeStart) {
				beginAirplaneHour = hourOfDay;
				beginAirplaneMinute = minutes;
				if (minutes < 10) {
					showTime = new StringBuilder().append("start: ")
							.append(hourOfDay).append(":0").append(minutes)
							.toString();
				} else {
					showTime = new StringBuilder().append("start: ")
							.append(hourOfDay).append(':').append(minutes)
							.toString();
				}
				tvStartTime.setText(showTime);
				setBeginTime();
			} else {
				endAirplaneHour = hourOfDay;
				endAirplaneMinute = minutes;
				if (minutes < 10) {
					showTime = new StringBuilder().append("end  : ")
							.append(hourOfDay).append(":0").append(minutes)
							.toString();
				} else {
					showTime = new StringBuilder().append("end  : ")
							.append(hourOfDay).append(':').append(minutes)
							.toString();
				}
				tvEndTime.setText(showTime);
				// TODO must know start time first!
				setEndAirplaneTime();

			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			// set time picker as current time
			return new TimePickerDialog(this, timePickerListener,
					beginAirplaneHour, beginAirplaneMinute, false);
		}
		return null;
	}

	public void onClick(View v) {
		// null;
	}

	private void setup() {
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i) {
				Toast.makeText(c, "starting Airplane Mode. Just wait.",
						Toast.LENGTH_LONG).show();
				startAirplaneMode();
			}
		};
		registerReceiver(br, new IntentFilter(
				"io.github.taocp.autoairplanemode"));
		pi = PendingIntent.getBroadcast(this, 0, new Intent(
				"io.github.taocp.autoairplanemode"), 0);
		am = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
	}

	private void setupConnectAlarm() {
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i) {
				Toast.makeText(c, "ending Airplane Mode. Just wait",
						Toast.LENGTH_LONG).show();
				endAirplaneMode();
			}
		};
		registerReceiver(br, new IntentFilter(
				"io.github.taocp.autoairplanemode.connect"));
		piConnect = PendingIntent.getBroadcast(this, 0, new Intent(
				"io.github.taocp.autoairplanemode.connect"), 0);
		alarmConnect = (AlarmManager) (this
				.getSystemService(Context.ALARM_SERVICE));
	}

	// 哪里说过通过参数控制程序流程是糟糕的风格？
	private void startAirplaneMode() {
		Settings.System.putInt(getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 1);
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", 1);
		sendBroadcast(intent);
	}

	private void endAirplaneMode() {
		Settings.System.putInt(getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0);
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", 0);
		sendBroadcast(intent);
	}

}

/**
 * ref , timepicker :
 * http://androidexample.com/Time_Picker_With_AM_PM_Values_-_Android_Example
 * /index.php?view=article_discription&aid=86&aaid=109
 * http://bbs.51cto.com/thread-835158-1.html
 */
