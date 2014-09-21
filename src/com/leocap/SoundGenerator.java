package com.leocap.nmeaview;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


public class SoundGenerator implements Runnable
{
    Thread _thrd;
    final int sr = 8000;	// a sample rate of 8k is good enough for frequencies upto 4kHz
    boolean _isRunning = false;
    double  _freq = 440f;
    int _lastUpdate = 0;

    public SoundGenerator()
    {
        // start a new thread to synthesise audio
        _thrd = new Thread( this );
        //setPriority(Thread.MAX_PRIORITY);
    }
    
    public void start()
    {
    	if( false == _thrd.isAlive() && false == _isRunning )
    	{
    		_thrd.start();
    		_isRunning = true;
    	}
    }

    public void setFrequency( double freq )
    {
    	_freq = freq;
    	_lastUpdate = 0;
    	start();
    	_thrd.interrupt();
    }

   	@Override
   	public void run()
   	{
        // set the buffer size
        int buffsize = AudioTrack.getMinBufferSize( sr,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC,
                                          sr, AudioFormat.CHANNEL_OUT_MONO,
                                  AudioFormat.ENCODING_PCM_16BIT, buffsize,
                                  AudioTrack.MODE_STREAM);

        short samples[] = new short[buffsize];
        double ph = 0.0;

        // start audio
       audioTrack.play();

		// synthesis loop
		while( _isRunning )
		{
			if( ++_lastUpdate < 6 )  // stop sounding after a while if updates stop
			{
				final double phDelta = 2. * Math.PI * _freq / sr;
		        int amp = 0;//Short.MAX_VALUE;
				//final int ampDelta = amp / buffsize;
				final int ampPosDelta = Short.MAX_VALUE / (buffsize/4); // 1/4 attack
				final int ampNegDelta = Short.MAX_VALUE / (buffsize*3/4); // 3/4 decay
	
				for( int i = 0; i < buffsize; i++ )
				{
					samples[i] = (short) (amp * Math.sin( ph ));
					ph += phDelta;
					// add amplitude envelope shaping
					amp = (buffsize/4 > i ? amp + ampPosDelta : amp - ampNegDelta);
				}
				audioTrack.write( samples, 0, buffsize ); // blocks until buffer written
			}
			try	{ Thread.sleep( (long) (1e6 / _freq) );	} catch (InterruptedException e) {}
		}
      audioTrack.stop();
      audioTrack.release();
      System.out.println( "Exiting sound gen run loop" );
   	}


   	// This method won't actually stop the thread unless it's called from another thread
    public void stop()
    {
    	if( _isRunning )
    	{
          _isRunning = false;
          try {
        	  _thrd.join();
           } catch (InterruptedException e) {
             e.printStackTrace();
           }
    	}
     }
}
