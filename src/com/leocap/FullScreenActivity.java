package com.leocap.nmeaview;

import java.util.List;

import com.leocap.nmeaview.util.SystemUiHider;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus.Listener;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
//import android.content.pm.ActivityInfo;
//import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
// IOIO classes
import ioio.lib.util.android.IOIOActivity;
import ioio.lib.util.IOIOLooper;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.Uart.Parity;
import ioio.lib.api.Uart.StopBits;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
// MarineAPI classes
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.MWVSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
//import net.sf.marineapi.nmea.sentence.DBTSentence;
import net.sf.marineapi.nmea.sentence.DPTSentence;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.MTWSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.TalkerId;
import net.sf.marineapi.nmea.sentence.VHWSentence;
import net.sf.marineapi.nmea.sentence.VLWSentence;
import net.sf.marineapi.nmea.sentence.XDRSentence;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * TODO Add security mode that monitors GPS coords and sends them in an SMS if they change!
 * 
 * @see SystemUiHider
 */
public class FullscreenActivity extends IOIOActivity implements IOIOLooper,	SentenceListener, LocationListener, NmeaListener, Listener, SensorEventListener
{
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	ViewGroup _mainViewGroup = null;
	private PanelView _panelView;
	private TextView _textView2;
	private TextView _textView4;
	private TextView _textView6;
    private TextView _textDump;

    // define the display assembly compass picture
    private ImageView _imgCompass;
     
    // record the compass picture angle turned
    private float _currDegree = 0f;
 
    // device sensor manager
    private SensorManager _sensorManager;
    SoundGenerator _soundgen;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		SentenceFactory.getInstance().registerParser( "ARLIHS", new IHSParser(TalkerId.P).getClass() );

		//setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE );
		super.onCreate( savedInstanceState );
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		//setContentView( R.layout.activity_fullscreen );// this will inflate the layout
		// But do it explicitly so we can retain a reference to the resulting ViewGroup of layout.
		// That will allow us to iterate through child views later on
		_mainViewGroup = (ViewGroup)
		LayoutInflater.from(this).inflate(R.layout.activity_fullscreen, null);
		setContentView( _mainViewGroup );

		final View controlsView = findViewById( R.id.fullscreen_content_controls );
		final View _contentView = findViewById( R.id.fullscreen_content );
		_panelView = (PanelView) findViewById( R.id.panel1 );
		_textView2 = (TextView) findViewById( R.id.text2 );
		_textView4 = (TextView) findViewById( R.id.text4 );
		_textView6 = (TextView) findViewById( R.id.text6 );
		_textDump  = (TextView) findViewById( R.id.dump );
		//_imgCompass = (ImageView) findViewById( R.id.imageViewCompass );
		//_imgCompass.setAlpha( 80 );
        
        /* draw a red arrow-type shape
        Path p = new Path();  
        RectF rect = new RectF(0f,0f,100f,100f); // left, top, right, bottom
        float fStep = 45f; // 30 degree steps
		for( float fAngle=0; fAngle<360f; fAngle+=fStep ) {
			 p.arcTo( rect, fAngle, fStep, true );
			 float sa = -10f * (float)Math.sin( (fAngle+fStep)*Math.PI/180.0 );
			 float ca = -10f * (float)Math.cos( (fAngle+fStep)*Math.PI/180.0 );
			 p.rLineTo( ca, sa );
			 //path.rMoveTo( -ca, -sa ); don't need this - arcTo will move cursor
		}
		 
        ShapeDrawable d = new ShapeDrawable (new PathShape(p, 100, 100));  
        d.setIntrinsicHeight(100);  
        d.setIntrinsicWidth(100);  
        d.getPaint().setColor(Color.WHITE);  
        //d.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);  
        d.getPaint().setStyle( Paint.Style.STROKE );
		d.getPaint().setStrokeWidth(2f);
		_textView4.setBackgroundDrawable( d );*/

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance( this, _contentView,	HIDER_FLAGS );
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener( new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible)
					{
						if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2 )
						{
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if( mControlsHeight == 0 )
							{
								mControlsHeight = controlsView.getHeight();
							}
							if( mShortAnimTime == 0 )
							{
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime );
							}
							controlsView
									.animate()
									.translationY(
											visible ? 0 : mControlsHeight )
									.setDuration( mShortAnimTime );
						} else
						{
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility( visible ? View.VISIBLE
									: View.GONE );
						}

						if( visible && AUTO_HIDE )
						{
							// Schedule a hide().
							delayedHide( AUTO_HIDE_DELAY_MILLIS );
						}
					}
				} );

		// Set up the user interaction to manually show or hide the system UI.
		_contentView.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				if( TOGGLE_ON_CLICK )
				{
					mSystemUiHider.toggle();
				} else
				{
					mSystemUiHider.show();
				}
			}
		} );

		final Button btn = (Button) findViewById( R.id.reset_button );
		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		btn.setOnTouchListener( mDelayHideTouchListener );
		
		LocationManager locationManager = (LocationManager) this.getSystemService( LOCATION_SERVICE );
		locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, this );
		locationManager.addNmeaListener( this );
		locationManager.addGpsStatusListener( this );

		_sensorManager = (SensorManager) getSystemService( SENSOR_SERVICE );
        _soundgen = new SoundGenerator();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate( savedInstanceState );

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide( 100 );
	}
	  
	@Override
    protected void onResume()
	{
          super.onResume();
   
          List<Sensor> sensors = _sensorManager.getSensorList( Sensor.TYPE_ALL );
          for( Sensor sensor: sensors )
        	  System.out.println( sensor );
          // for the system's orientation sensor registered listeners
//          _sensorManager.registerListener( this, _sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI );
    }
   
    @Override
    protected void onPause()
    {
          super.onPause();
   
          // to stop the listener and save battery
          _sensorManager.unregisterListener(this);
    }
   

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch( View view, MotionEvent motionEvent )
		{
			if( AUTO_HIDE )
			{
				delayedHide( AUTO_HIDE_DELAY_MILLIS );
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run()
		{
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide( int delayMillis )
	{
		mHideHandler.removeCallbacks( mHideRunnable );
		mHideHandler.postDelayed( mHideRunnable, delayMillis );
	}

	
	// OnClick handler for Config button
	public void openOtherActivity( View view ) 
	{
	    Intent intent = new Intent( this, SoundActivity.class );
	    intent.putExtra( "arg1", "value1" );
	    startActivity( intent );
	}


	//
	// IOIO stuff
	//
	
	private DigitalOutput _IOIOled;
	private PwmOutput _pwm1; // For moving-coil meter 1: Wind speed
	private PwmOutput _pwm2; // For moving-coil meter 2: Depth
	private Uart _uart1; // for NMEA device 1: DST-2
	private Uart _uart2; // for NMEA device 2: NASA Meteoman
	private SentenceReader _NMEASentenceReader1;
	private SentenceReader _NMEASentenceReader2;
	
	@Override
	public IOIOLooper createIOIOLooper( String connectionType, Object extra )
	{
        return this;
	}

	// Implementation of IOIOLooper
	@Override
	public void disconnected()
	{
		System.err.println( "IOIO disconnected - stopping NMEA sentence reader" );
		_NMEASentenceReader1.stop();
		_NMEASentenceReader2.stop();
	}

	@Override
	public void incompatible()
	{
		System.err.println( "IOIO incompatible!" );
	}

	@Override
	public void loop() throws ConnectionLostException, InterruptedException
	{
		Thread.sleep( 1000 ); // IOIO calls this in a tight loop
	}

	@Override
	public void setup( IOIO ioio ) throws ConnectionLostException, InterruptedException
	{
		// we'll blink the on-board red LED
		_IOIOled = ioio.openDigitalOutput( IOIO.LED_PIN, true );
		// set PWM pins to refresh at 1000 Hz. Plenty high enough for a moving-coil meter
    	_pwm1 = ioio.openPwmOutput( 13, 1000 /*Hz*/ );
    	_pwm2 = ioio.openPwmOutput( 14, 1000 /*Hz*/ );

		// open two UARTs, one receiving on pin 11 the other on 12 but neither transmitting
		_uart1 = ioio.openUart( 11, IOIO.INVALID_PIN /* no Tx */, 4800, Parity.NONE, StopBits.ONE );
		_uart2 = ioio.openUart( 12, IOIO.INVALID_PIN /* no Tx */, 4800, Parity.NONE, StopBits.ONE );
		// and attach a NMEA sentence reader to its input stream
		_NMEASentenceReader1 = new SentenceReader( _uart1.getInputStream() );
		_NMEASentenceReader1.addSentenceListener( this );
		_NMEASentenceReader1.start();
		_NMEASentenceReader2 = new SentenceReader( _uart2.getInputStream() );
		_NMEASentenceReader2.addSentenceListener( this );
		_NMEASentenceReader2.start();
	}

	// Implementation of SentenceListener
	@Override
	public void readingPaused()
	{
		System.out.println( "NMEA reading paused ..." );
	}

	@Override
	public void readingStarted()
	{
		System.out.println( "NMEA reading started ..." );
	}

	@Override
	public void readingStopped()
	{
		System.out.println( "NMEA reading stopped ..." );
	}

	@Override
	public void sentenceRead( SentenceEvent event )
	{
		// From within CustomTextView: Activity host = (Activity) view.getContext()
		
		// blink LED when sentence received by turning it on and then off at end of this method
		try	{ 
			_IOIOled.write( false );

		final Sentence sentence = event.getSentence();
		final String sentenceId = sentence.getSentenceId();

		if( "DPT".equals( sentenceId ) )
		{
			// Depth of water. +ve offset to transducer == to water line, -ve == to keel
			final DPTSentence DPT = (DPTSentence) sentence;
			System.out.println( "Depth: " + DPT.getDepth() + " offset: " + DPT.getOffset() );
			//if( _nDepthAlarm > DPT.getDepth() ) alarm! Or give continuous audio feedback
			// Set the value in the UI
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					_textView2.setText(String.format("%3.1f",DPT.getDepth() ));
              		_soundgen.setFrequency( 440 + 440/(DPT.getDepth()+0.1) ); } } );
		}
		else if( "GGA".equals( sentenceId ) )
		{
			final GGASentence GGA = (GGASentence) sentence;
			System.out.println( GGA.getPosition() );
		}
		else if( "ARLIHS".equals( sentenceId ) ) // Proprietary sentence: Actisense Research Ltd.
		{  // DST-2 also generates DBT/DPT (depth) VHW/VLW (speed/distance) and MTW (temp). 
			final IHSSentence IHS = (IHSSentence) sentence;
			System.out.println( IHS.getTotalOperatingTime() + "secs. v" + IHS.getAppFirmwareVersion() );
			if( _textDump != null )
				_textDump.append( IHS.getTotalOperatingTime() + "secs. v" + IHS.getAppFirmwareVersion() );
		}
		else if( "MWV".equals( sentenceId ) ) // Wind Velocity. 
		{ // This will be apparent we'd need boat velocity over ground in order to convert to true.
			final MWVSentence MWV = (MWVSentence) sentence;
			//System.out.println( "Wind angle: " + MWV.getAngle() );
			// rotate pointer graphic
			_panelView.setWindVelocity( (float)MWV.getSpeed(), (float)MWV.getAngle() );
			_pwm1.setDutyCycle( (float)MWV.getSpeed()/50f ); // FSD is 50 knots, let's assume NASA outputs in knots
		}
		else if( "MTW".equals( sentenceId ) ) // Temperature of Water. Sent by DST-2
		{
			final MTWSentence MTW = (MTWSentence) sentence;
			// TODO add max temperature alarm
			System.out.println( "Water temp: " + MTW.getTemperature() );
			// Set the value in the UI
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					_textView6.setText(String.format("%3.0f", MTW.getTemperature())); } } );
		}
		else if( "RMC".equals( sentenceId ) ) // Recommended Minimum navigation, type C
		{
			final RMCSentence RMC = (RMCSentence) sentence;
			System.out.println( RMC.getCourse() );
		}
		else if( "VHW".equals( sentenceId ) ) // Heading & Water speed. Sent by DST-2
		{
			final VHWSentence VHW = (VHWSentence) sentence;
			System.out.println( "Speed (knots): " + VHW.getSpeedKnots() );
			// Set the value in the UI TODO: factor this out
			/*runOnUiThread( new Runnable() {
				@Override
				public void run() {
					_textView4.setText(String.format("%2.2f", VHW.getSpeedKnots())); } } );
			Get custom view to implement Runnable. It can determine hosting Activity with:
			Activity host = (Activity) view.getContext()*/
			_textView4.post( new Runnable() {
			@Override
			public void run() {
				_textView4.setText(String.format("%2.2f", VHW.getSpeedKnots())); } } );
		}
		else if( "VLW".equals( sentenceId ) ) // Distance travelled through water, cumm + trip. Sent by DST-2
		{
			final VLWSentence VLW = (VLWSentence) sentence;
			System.out.println( "Trip ("+VLW.getTripUnits()+"): " + VLW.getTrip() );
			// TODO display log: VLW.getTotal();
			// TODO check trip against GPS ? diff due to tidal stream + log inaccuracy  
		}
		else if( "XDR".equals( sentenceId ) ) // Transducer measurements. NASA Meteoman for temp?
		{
			final XDRSentence XDR = (XDRSentence) sentence;
			System.out.println( "XDR measurement 1: " + XDR.getMeasurements().get(0).getValue() );
		}
		else
			System.out.println( sentence.getSentenceId() + ": " + sentence );
		
		_IOIOled.write( true );
		} catch( ConnectionLostException e ) { System.err.println( e ); } // TODO display error on Android device
	}


	// Implementation of Location listener
	@Override
	public void onLocationChanged( Location loc )
	{
		System.out.println( "onLocationChanged(): lat: " + loc.getLatitude() + " long: " + loc.getLongitude() + " bearing: " + loc.getBearing() );
		_panelView.setWindVelocity( (float)loc.getLatitude(), (float)loc.getLongitude() ); // use lat as speed, long as direction
	}

	@Override
	public void onProviderDisabled( String provider )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled( String provider )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged( String provider, int status, Bundle extras )
	{
		// TODO Auto-generated method stub
		
	}

	// Implementation of NMEA listener
	@Override
	public void onNmeaReceived( long timestamp, String strNMEA )
	{
		System.out.println( "onNmeaReceived(): " + strNMEA );		
	}

	// Implementation of GPS staus listener
	@Override
	public void onGpsStatusChanged( int event )
	{
		System.out.println( "onGpsStatusChanged(): " + event );				
	}
	
	
	  // Sensor listener (e.g. for magnetic compass)
	  @Override
	  public void onSensorChanged( SensorEvent event )
	  {
	      // get the angle around the z-axis rotated
          float degree = (null != event ? Math.round(event.values[0]) : (float)Math.random()*360f);
   
          ((TextView)findViewById( R.id.text5 )).setText("Heading: " + Float.toString(degree) + " degrees");
   
          // create a rotation animation (reverse turn degree degrees)
          RotateAnimation ra = new RotateAnimation(
                  _currDegree,
                  -degree,
                  Animation.RELATIVE_TO_SELF, 0.5f,
                  Animation.RELATIVE_TO_SELF, 0.5f );
   
          // how long the animation will take place
          ra.setDuration(210);
   
          // set the animation after the end of the reservation status
          ra.setFillAfter(true);
   
          // Start the animation
	      _imgCompass.startAnimation(ra);
	      _currDegree = -degree;
	  }
	  
	  @Override
	  public void onAccuracyChanged( Sensor sensor, int accuracy )
	  {
	      // not used
	  }
}
