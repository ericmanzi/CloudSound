package com.tangible.cloudsound;

import java.io.File;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import at.abraxas.amarino.Amarino;
import at.abraxas.amarino.AmarinoIntent;

public class MainActivity extends ActionBarActivity {
	
	private ArduinoReceiver arduinoReceiver = new ArduinoReceiver();
	private static final String DEVICE_ADDRESS =  "00:06:66:03:73:7B"; //"00:06:66:03:73:7B";

	private final int INFLATE = 1;
	private final int DEFLATE = 2;
	private final int VIBRATE = 3;
	
	
	
	//recording/playing sound
	private MediaRecorder mRecorder;
	private MediaPlayer mPlayer;
	private boolean playing=false;
	String fileName = null; 
	
	//views
	FrameLayout frame;
	Button listenButton;
	Button recordButton;
	Button doneButton;
	Typeface robotoMedium;
	TextView lookup;
	Chronometer chronos;
	ViewSwitcher switcher;
	TextView pickInflatedText;
	ProgressBar bar;
	RelativeLayout rlBoxes;
	FrameLayout flCoverBoxes;
	
	private int mode; //to decide what happens when the squares are clicked
	private final int NEUTRAL=0;
	private final int RECORDING=1;
	private final int LISTENING=2;
	
	private int numSaved=0;
	
	private View currentRecordingSquare;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
//		android.app.ActionBar actionBar = getActionBar();
//		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#330000ff")));
//		actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#550000ff")));
//		
		
		
		
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		super.onCreate(savedInstanceState);
		
		
		setContentView(R.layout.activity_main);
		ActionBar actionBar = getSupportActionBar();
		
		
		fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
		frame = (FrameLayout) findViewById(R.id.frame2);
		listenButton = (Button) findViewById(R.id.listenButton);
		listenButton.setEnabled(false);
		listenButton.setBackgroundResource(R.drawable.button_shape);
		recordButton = (Button) findViewById(R.id.recordButton);
		doneButton = (Button) findViewById(R.id.doneRecording);
		doneButton.setBackgroundResource(R.drawable.button_shape);

		recordButton.setBackgroundResource(R.drawable.button_shape);
		chronos = (Chronometer) findViewById(R.id.chronometer1);
		lookup = (TextView) findViewById(R.id.lookup);
		switcher = (ViewSwitcher) findViewById(R.id.switcher);
		rlBoxes = (RelativeLayout) findViewById(R.id.rlBoxes);
		flCoverBoxes = (FrameLayout) findViewById(R.id.frameCoverBoxes);
		robotoMedium = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
		mode = NEUTRAL;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// in order to receive broadcasted intents we need to register our receiver
		registerReceiver(arduinoReceiver, new IntentFilter(AmarinoIntent.ACTION_RECEIVED));
		
		// this is how you tell Amarino to connect to a specific BT device from within your own code
		Amarino.connect(this, DEVICE_ADDRESS);
	}


	@Override
	protected void onStop() {
		super.onStop();
		
		// if you connect in onStart() you must not forget to disconnect when your app is closed
		Amarino.disconnect(this, DEVICE_ADDRESS);
		
		// do never forget to unregister a registered receiver
		unregisterReceiver(arduinoReceiver);
	}
	
	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) return true;
		return super.onOptionsItemSelected(item);
	}

	public void record(String square) {
		mRecorder=new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(fileName+"/"+square+".3gp");
		Log.i("RECORD", fileName+"/"+square+".3gp");
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			mRecorder.prepare();
			mRecorder.start();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("RECORDING", "prepare() failed");
		}
	} 
	
	public void play(String square, View v) {
		if (!playing) {
			final View vTemp = v;
			final String squareTemp = square;
			try {
				mPlayer = new MediaPlayer();
				mPlayer.setDataSource(fileName+"/"+square+".3gp");
				mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mPlayer) {
						donePlaying(vTemp);
						File file = new File(fileName+"/"+squareTemp+".3gp");
						boolean deleted = file.delete();
						bar.setVisibility(View.GONE);
					}
				});
				mPlayer.prepare();
				long totalDuration = mPlayer.getDuration();
				mPlayer.start();
				
				bar = (ProgressBar) findViewById(R.id.progress);
			    bar.setProgress(0);
			    bar.setVisibility(View.VISIBLE);
//			    bar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
//			    bar.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
			    bar.setMax(100);
			    final long mDuration = mPlayer.getDuration();
			    Log.i("PLAYER", "duration: "+mDuration);
			    /* CountDownTimer starts with length of audio file and every onTick is 1 second */
			    CountDownTimer cdt = new CountDownTimer(mDuration-1000, 100) { 

			        public void onTick(long millisUntilFinished) {

			        	long dTotal = mDuration-millisUntilFinished;
			            int progVal = (int) ((dTotal * 100l)/mDuration);
			            if (millisUntilFinished<201) {
			            	bar.setProgress(100);
			            } else {
			            	bar.setProgress(progVal);
			            }
			            
			            Log.i("PLAYER", "dTotal: "+dTotal);
			            Log.i("PLAYER", "progVal: "+progVal);
			        }

			        public void onFinish() {
			             // DO something when 2 minutes is up
			        }
			    }.start();
				playing = true;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void showPickInflated(View v) { //record
		showText("PICK AN INFLATED BALLOON");
		flCoverBoxes.setVisibility(View.INVISIBLE);
		mode = LISTENING;
	}
	
	public void showPickAny(View v) { //listening
		showText("PICK A BALLOON");
		flCoverBoxes.setVisibility(View.INVISIBLE);
		mode = RECORDING;
	}
	
	public void showText(String text) {
		pickInflatedText = new TextView(this);
		pickInflatedText.setText(text);
		pickInflatedText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		pickInflatedText.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		pickInflatedText.setTextSize(45);
		pickInflatedText.setTextColor(Color.parseColor("#747675"));
		pickInflatedText.setBackgroundColor(Color.parseColor("#99FFFFFF"));
		pickInflatedText.setTypeface(robotoMedium);
		frame.addView(pickInflatedText);
		listenButton.setVisibility(View.GONE);
		recordButton.setVisibility(View.GONE);
	}

	private char convertNameToFlag(String name) {
		if (name.equals("square1")) return 'A';
		if (name.equals("square2")) return 'B';
		if (name.equals("square3")) return 'C';
		if (name.equals("square4")) return 'D';
		if (name.equals("square5")) return 'E';
		if (name.equals("square6")) return 'F';
		if (name.equals("square7")) return 'G';
		if (name.equals("square8")) return 'H';
		if (name.equals("square9")) return 'I';
		if (name.equals("square10")) return 'J';
		if (name.equals("square11")) return 'K';
		if (name.equals("square12")) return 'L';
		if (name.equals("square13")) return 'M';
		if (name.equals("square14")) return 'N';
		if (name.equals("square15")) return 'O';
		else return 'P'; //if (name.equals("square16"))  
		
	}
	
	public void onBoxClick(View v) {
		String name = v.getResources().getResourceName(v.getId()).split("/")[1];
		if (mode==LISTENING) {
			//start playing
			if (v.getTag(v.getId())!=null && v.getTag(v.getId()).toString()=="something") { //if audio saved here
				// SEND BLINKING CODE
				
				Amarino.sendDataToArduino(this, DEVICE_ADDRESS, convertNameToFlag(name), VIBRATE);
				Log.i("AMARINO", "sent VIBRATE to "+name+" at "+convertNameToFlag(name));

				
				
				lookup.setVisibility(View.VISIBLE);
				lookup.setText("\nPlaying...\nLook up at balloon\n");
				frame.setBackgroundColor(Color.parseColor("#70000000"));
				pickInflatedText.setVisibility(View.GONE);
				chronos.setVisibility(View.VISIBLE);
				chronos.setGravity(Gravity.RIGHT);
				chronos.setBase(SystemClock.elapsedRealtime());
				chronos.start();
				
				v.setBackgroundResource(R.drawable.cloud_shape);
//				int width = 70;
//				int height = 70;
//				RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(width,height);
//				v.setLayoutParams(parms);
				v.setTag(v.getId(), "nothing");//false if no audio saved here
				//play the audio
				play(name, v);
				mode=NEUTRAL;
			} else {
				Toast.makeText(this, "Nothing stored here", Toast.LENGTH_SHORT).show();
			}
			
		} else if (mode==RECORDING) { //start recording
			// SEND BLINKING CODE
			Amarino.sendDataToArduino(this, DEVICE_ADDRESS, convertNameToFlag(name), VIBRATE);
			Log.i("AMARINO", "sent VIBRATE to "+name+" at "+convertNameToFlag(name));
			currentRecordingSquare=v;
			lookup.setVisibility(View.VISIBLE);
			lookup.setText("\nRecording...\nLook up at balloon\n");
			frame.setBackgroundColor(Color.parseColor("#70000000"));
			pickInflatedText.setVisibility(View.GONE);
			switcher.showNext();
			chronos.setVisibility(View.VISIBLE);
			chronos.setGravity(Gravity.CENTER);
			chronos.setBase(SystemClock.elapsedRealtime());
			chronos.start();
//			int width = 70;
//			int height = 70;
//			RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(width,height);
//			v.setLayoutParams(parms);
			
			record(name);
			//fill square with circle
			v.setBackgroundResource(R.drawable.filled);
			v.setTag(v.getId(),"something");//true if audio saved here
			mode=NEUTRAL;
		} else {
			//do nothing
			
		}
		
	}
	
	
	
	public void doneRecording(View v) {
		
		//SEND INFLATE CODE
		String name = currentRecordingSquare.getResources().getResourceName(currentRecordingSquare.getId()).split("/")[1];

		Amarino.sendDataToArduino(this, DEVICE_ADDRESS, convertNameToFlag(name), INFLATE);
		Log.i("AMARINO", "sent INFLATE to "+name+" at "+convertNameToFlag(name));

		
		chronos.stop();
		flCoverBoxes.setVisibility(View.VISIBLE);
		lookup.setVisibility(View.INVISIBLE);
		chronos.setVisibility(View.INVISIBLE);
		frame.setVisibility(View.VISIBLE);
		frame.setBackgroundColor(Color.parseColor("#00000000"));

		switcher.showPrevious();
		listenButton.setVisibility(View.VISIBLE);
		recordButton.setVisibility(View.VISIBLE);
		//stop recorder
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
		
		mode=NEUTRAL;
		numSaved++;
		listenButton.setEnabled(numSaved>0);
	}
	
	public void donePlaying(View v) {
		//SEND DEFLATE CODE
		String name = v.getResources().getResourceName(v.getId()).split("/")[1];

		Amarino.sendDataToArduino(this, DEVICE_ADDRESS, convertNameToFlag(name), DEFLATE);
		Log.i("AMARINO", "sent DEFLATE to "+name+" at "+convertNameToFlag(name));
		chronos.stop();
		lookup.setVisibility(View.INVISIBLE);
		chronos.setVisibility(View.INVISIBLE);
		flCoverBoxes.setVisibility(View.VISIBLE);
		frame.setBackgroundColor(Color.parseColor("#00000000"));
		listenButton.setVisibility(View.VISIBLE);
		recordButton.setVisibility(View.VISIBLE);
		frame.removeView(pickInflatedText);

		if (mPlayer!=null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer=null;
			playing=false; 
		}
		mode = NEUTRAL;
		numSaved--;
		listenButton.setEnabled(numSaved>0);

	}
	
	
    
	

	/**
	 * ArduinoReceiver is responsible for catching broadcasted Amarino
	 * events.
	 * 
	 * It extracts data from the intent and updates the graph accordingly.
	 */
	public class ArduinoReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String data = null;
			
			// the device address from which the data was sent, we don't need it here but to demonstrate how you retrieve it
			final String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			
			// the type of data which is added to the intent
			final int dataType = intent.getIntExtra(AmarinoIntent.EXTRA_DATA_TYPE, -1);
			
			// we only expect String data though, but it is better to check if really string was sent
			// later Amarino will support differnt data types, so far data comes always as string and
			// you have to parse the data to the type you have sent from Arduino, like it is shown below
			if (dataType == AmarinoIntent.STRING_EXTRA){
				data = intent.getStringExtra(AmarinoIntent.EXTRA_DATA);
				
				if (data != null){
					// Not receiving anything
				}
			}
		}
	}
	
	
}
