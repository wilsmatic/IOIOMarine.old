package com.leocap.nmeaview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class PanelView extends View
{
	private int _dialSize = 300;
	private Path _pathDial;
	private Path _pathPointer;
	private Paint _paintDial;
	private Paint _paintPointer;
	private TextPaint _paintText = new TextPaint( Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG );
	private float _fSpeedTextX, _fSpeedTextY;
	private Matrix _matrix = new Matrix();
	private float _fPointerAngle = 0f;
	private float _fWindSpeed = 0f;

	public PanelView(Context context)
	{
		super( context );
		init();
	}

	public PanelView(Context context, AttributeSet attrs)
	{
		super( context, attrs );
		for( int i=0; i<attrs.getAttributeCount(); i++ )
			System.out.println( i + ": " + attrs.getAttributeName( i ) + " value: " + attrs.getAttributeValue( i ) );
		init();
	}

	public PanelView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super( context, attrs, defStyleAttr );
		init();
	}


	protected void init()
	{
		setMinimumHeight( _dialSize );
		setMinimumWidth( _dialSize );
		_paintText.setColor( Color.WHITE );
		_paintText.setTextSize( 120f );
		_paintText.setTextAlign( Paint.Align.CENTER );
		_paintText.setShadowLayer( 4f, 3f, 3f, Color.BLUE );

		final FontMetrics metrics = _paintText.getFontMetrics();
		final float fTextHeight = Math.abs(metrics.top - metrics.bottom);
		_fSpeedTextX = _dialSize / 2;
		//_fSpeedTextY = (_dialSize / 2) + (fTextHeight / 2);
		_fSpeedTextY = (_dialSize / 2) + 40f;
		
        _pathDial = new Path();  
		// draw a circular dial with marks every 30 degrees
        RectF rect = new RectF(0f,0f,_dialSize,_dialSize); // left, top, right, bottom
        float fStep = 30f; // 30 degree steps
		for( float fAngle=0; fAngle<360f; fAngle+=fStep ) {
			_pathDial.arcTo( rect, fAngle, fStep, true );
			 float sa = -10f * (float)Math.sin( (fAngle+fStep)*Math.PI/180.0 );
			 float ca = -10f * (float)Math.cos( (fAngle+fStep)*Math.PI/180.0 );
			 _pathDial.rLineTo( ca, sa );
			 //path.rMoveTo( -ca, -sa ); don't need this - arcTo will move cursor
		}
		
		_paintDial = new Paint();
		_paintDial.setColor( Color.WHITE );
		_paintDial.setStyle( Paint.Style.STROKE );
		_paintDial.setStrokeWidth( 2f );
		_paintDial.setAntiAlias( true );
		_paintDial.setTextSize( 20f );

		_pathPointer = new Path();
        // draw an arrow-type shape
		_pathPointer.moveTo( _dialSize/2, 0 );   
		_pathPointer.lineTo( _dialSize*4/5, _dialSize );   
		_pathPointer.lineTo( _dialSize/2 , _dialSize*4/5 );   
		_pathPointer.lineTo( _dialSize/5, _dialSize );   
		_pathPointer.lineTo( _dialSize/2, 0 );
		
		_paintPointer = new Paint();
        _paintPointer.setStyle(Paint.Style.FILL_AND_STROKE);  
		_paintPointer.setColor( Color.RED );
	}


	/**
	 * @param fWindAngle the pointer angle to set
	 */
	public void setWindVelocity( float fWindSpeed, float fWindAngle )
	{
		final float fRelativeAngle = fWindAngle - _fPointerAngle;
		_fPointerAngle = fWindAngle;
		_fWindSpeed = fWindSpeed;

		// rotate the pointer about its center to the desired angle via a matrix transformation
		_matrix.setRotate( fRelativeAngle, _dialSize/2, _dialSize/2 );
		_pathPointer.transform( _matrix );

		if( 180.0 > _fPointerAngle )
			_paintPointer.setColor( Color.GREEN );
		else
			_paintPointer.setColor( Color.RED );

		postInvalidate();	// we're not expecting to be called from the main thread 
		// invalidate just the dial: postInvalidate( left, top, right, bottom );
	}


	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw( canvas );
		
		canvas.drawPath( _pathDial, _paintDial );

		// and draw it
		canvas.drawPath( _pathPointer, _paintPointer );
		
		// draw wind speed in the middle of the pointer
		String strWindSpeed = String.format( "%.0f", _fWindSpeed );
		canvas.drawText( strWindSpeed, _fSpeedTextX, _fSpeedTextY, _paintText );

		canvas.drawText( "Log: 12345.7", _dialSize+20, 20f, _paintDial );
	}
}
