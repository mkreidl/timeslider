package com.mkreidl.timeslider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class TimeSlider extends View implements TimeScrollable
{
    public enum Orientation
    {
        UP, DOWN, LEFT, RIGHT
    }

    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );

    /**
     * Determine an approximate number of milliseconds for given time unit.
     * This is used to determine, from a given distance of pointer movement
     * on the screen (as a multiple of time unit), the change in time
     * (in milliseconds) that should be attributed to this scroll.
     *
     * @param unit A time unit as defined in Calendar class
     * @return milliseconds that should be attributed to a scroll
     * by one display item <unit> (=<minItemWidth> pixels)
     */
    private static long convertToMillis( int unit )
    {
        long returnVal = 1L;
        switch ( unit )
        {
            case Calendar.YEAR:
                returnVal *= 12;
            case Calendar.MONTH:
                returnVal *= 30;
            case Calendar.DAY_OF_MONTH:
                returnVal *= 24;
            case Calendar.HOUR_OF_DAY:
                returnVal *= 60;
            case Calendar.MINUTE:
                returnVal *= 60;
            case Calendar.SECOND:
                returnVal *= 1000;
            case Calendar.MILLISECOND:
                returnVal *= 1;
        }
        return returnVal;
    }

    private static int getYear( Calendar calendar )
    {
        return calendar.get( Calendar.ERA ) == GregorianCalendar.AD ?
                calendar.get( Calendar.YEAR ) : 1 - calendar.get( Calendar.YEAR );
    }

    private static void setYear( Calendar calendar, int yearNumber )
    {
        if ( yearNumber > 0 )
        {
            calendar.set( Calendar.ERA, GregorianCalendar.AD );
            calendar.set( Calendar.YEAR, yearNumber );
        }
        else
        {
            calendar.set( Calendar.ERA, GregorianCalendar.BC );
            calendar.set( Calendar.YEAR, 1 - yearNumber );
        }
    }

    private static void addTimeUnits( Calendar calendar, int unit, int count )
    {
        final boolean revertSign =
                unit == Calendar.YEAR && calendar.get( Calendar.ERA ) == GregorianCalendar.BC;
        calendar.add( unit, revertSign ? -count : count );
    }

    private final Scroller scroller = new Scroller( getContext() );
    private final GestureDetector gestureDetector = new GestureDetector( getContext(), new GestureListener() );
    private OnTimeScrollListener listener = new OnTimeScrollListener()
    {
        @Override
        public void onTimeScroll( long time, @Nullable TimeScrollable source )
        {
        }

        @Override
        public void onTimeChanged( long time, @Nullable TimeScrollable source )
        {
        }

        @Override
        public void onScrollUnitChanged( @Nullable TimeScrollable source )
        {
        }
    };

    // Date and time data
    private long time;
    private TimeZone timeZone = TimeZone.getDefault();
    private Locale locale = Locale.getDefault();
    private final Calendar calendar = Calendar.getInstance( UTC );
    private final Calendar tmpCalendar = Calendar.getInstance( UTC );

    // Metric of the view
    private float centerX, centerY;

    // The following are variables used during scrolling animations to track a continuous
    // progress, while in the other variables only discrete progress (e.g. in steps of a year)
    // is stored.
    private long continuousTime;  // used internally to track progress while scrolling/flinging
    private long flingStartTime;

    // The following correspond to attributes definable in xml
    private Orientation orientation = Orientation.DOWN;
    private int minItemWidth = 150;
    private int minItemHeight = 60;
    private float scrollSpeed = 1f;
    private int numberItemsBefore = 2;
    private int numberItemsAfter = 2;

    private float fontSizeSelected = 18;
    private float fontSizeNormal = 12;
    private boolean fontSizeLinearGradient = false;
    private boolean fontColorLinearGradient = false;

    private String[] timeUnits;
    private String[] timeUnitNames;
    private String[] dateFormatStrings;
    private SimpleDateFormat[] dateFormats;
    private int unitIndex = 0;
    private int numItems;
    private final Paint normalPaint = new Paint();
    private final Paint selectedPaint = new Paint();

    private int timeUnit;
    private int timeUnitFactor;
    private SimpleDateFormat dateFormat;
    private float millisPerScrolledPixel;

    public TimeSlider( Context context )
    {
        this( context, null );
    }

    public TimeSlider( Context context, AttributeSet attrs )
    {
        this( context, attrs, 0 );
    }

    public TimeSlider( Context context, AttributeSet attrs, int defStyleAttr )
    {
        super( context, attrs, defStyleAttr );
        initLocaleTimeZone( attrs, defStyleAttr );
        initTimeSliderStyle( attrs, defStyleAttr );
        init();
    }

    private void init()
    {
        numItems = 1 + numberItemsAfter + numberItemsBefore;
        normalPaint.setTextAlign( Paint.Align.CENTER );
        selectedPaint.setTextAlign( Paint.Align.CENTER );
        initializeDateFormats( timeZone, locale );
        parseTimeUnitString( timeUnits[unitIndex] );
        setTime( time );
    }

    private void initLocaleTimeZone( AttributeSet attrs, int defStyleAttr )
    {
//        final TypedArray a = getContext().getTheme().obtainStyledAttributes( attrs, R.styleable.TimeScrollable, defStyleAttr, 0 );
//        try
//        {
//            if ( a.getString( R.styleable.TimeScrollable_locale ) != null )
//                locale = new Locale( a.getString( R.styleable.TimeScrollable_locale ) );
//            if ( a.getString( R.styleable.TimeScrollable_time_zone ) != null )
//                timeZone = TimeZone.getTimeZone( a.getString( R.styleable.TimeScrollable_time_zone ) );
//        }
//        finally
//        {
//            a.recycle();
//        }
    }

    private void initTimeSliderStyle( AttributeSet attrs, int defStyleAttr )
    {
        final TypedArray a = getContext().getTheme().obtainStyledAttributes( attrs, R.styleable.TimeSlider_TimeScrollable, defStyleAttr, 0 );
        try
        {
            orientation = Orientation.values()[
                    a.getInt( R.styleable.TimeSlider_TimeScrollable_direction, orientation.ordinal() )];
            minItemWidth = (int)a.getDimension( R.styleable.TimeSlider_TimeScrollable_item_width, minItemWidth );
            minItemHeight = (int)a.getDimension( R.styleable.TimeSlider_TimeScrollable_item_height, minItemHeight );
            scrollSpeed = a.getFloat( R.styleable.TimeSlider_TimeScrollable_scroll_speed, scrollSpeed );

            if ( a.getString( R.styleable.TimeSlider_TimeScrollable_time_unit ) != null )
                timeUnits = a.getString( R.styleable.TimeSlider_TimeScrollable_time_unit ).split( ";" );
            else
                timeUnits = new String[]{"second", "minute", "hour"};

            if ( a.getString( R.styleable.TimeSlider_TimeScrollable_time_unit_names ) != null )
                timeUnitNames = a.getString( R.styleable.TimeSlider_TimeScrollable_time_unit_names ).split( ";" );
            else
                timeUnitNames = new String[]{null};

            if ( a.getString( R.styleable.TimeSlider_TimeScrollable_format_string ) != null )
                dateFormatStrings = a.getString( R.styleable.TimeSlider_TimeScrollable_format_string ).split( ";" );
            else
                dateFormatStrings = new String[]{"HH:mm:ss;HH:mm;HH"};

            fontSizeNormal = a.getDimension( R.styleable.TimeSlider_TimeScrollable_font_size, fontSizeNormal );
            fontSizeSelected = a.getDimension( R.styleable.TimeSlider_TimeScrollable_font_size_selected, fontSizeSelected );

            normalPaint.setColor( a.getColor( R.styleable.TimeSlider_TimeScrollable_font_color, Color.GRAY ) );
            normalPaint.setTextSize( a.getDimension( R.styleable.TimeSlider_TimeScrollable_font_size, 30 ) );

            selectedPaint.setColor( a.getColor( R.styleable.TimeSlider_TimeScrollable_font_color_selected, Color.BLACK ) );
            selectedPaint.setTextSize( a.getDimension( R.styleable.TimeSlider_TimeScrollable_font_size_selected, 40 ) );

            fontSizeLinearGradient = a.getBoolean( R.styleable.TimeSlider_TimeScrollable_font_size_linear_gradient, fontSizeLinearGradient );
            fontColorLinearGradient = a.getBoolean( R.styleable.TimeSlider_TimeScrollable_font_color_linear_gradient, fontColorLinearGradient );

            numberItemsAfter = a.getInt( R.styleable.TimeSlider_TimeScrollable_number_items_after, numberItemsAfter );
            numberItemsBefore = a.getInt( R.styleable.TimeSlider_TimeScrollable_number_items_before, numberItemsBefore );
        }
        finally
        {
            a.recycle();
        }
    }

    private void initializeDateFormats( TimeZone timeZone, Locale locale )
    {
        dateFormats = new SimpleDateFormat[dateFormatStrings.length];
        for ( int i = 0; i < dateFormats.length; i++ )
        {
            dateFormats[i] = new SimpleDateFormat( dateFormatStrings[i], locale );
            if ( !isInEditMode() )
                dateFormats[i].setTimeZone( timeZone );
        }
        dateFormat = dateFormats[unitIndex];
    }

    private void parseTimeUnitString( String timeUnitString )
    {
        timeUnitFactor = 1;
        switch ( timeUnitString )
        {
            case "millennium":
                timeUnitFactor *= 10;
            case "century":
                timeUnitFactor *= 10;
            case "decade":
                timeUnitFactor *= 10;
            case "year":
                timeUnit = Calendar.YEAR;
                break;
            case "month":
                timeUnit = Calendar.MONTH;
                break;
            case "day":
                timeUnit = Calendar.DAY_OF_MONTH;
                break;
            case "hour":
                timeUnit = Calendar.HOUR_OF_DAY;
                break;
            case "minute":
                timeUnit = Calendar.MINUTE;
                break;
            case "second":
                timeUnit = Calendar.SECOND;
                break;
            case "millisecond":
                timeUnit = Calendar.MILLISECOND;
                break;
            default:
                timeUnit = Calendar.MILLISECOND;
                break;
        }
    }

    @Override
    protected synchronized void onDraw( Canvas canvas )
    {
        float posX = centerX;
        float posY = centerY;
        int direction = 1;

        if ( orientation == Orientation.LEFT || orientation == Orientation.UP )
            direction = -1;
        if ( orientation == Orientation.RIGHT || orientation == Orientation.DOWN )
            direction = 1;
        if ( isHorizontal() )
            posX -= direction * ( 1f + numberItemsBefore + numberItemsAfter ) / 2f * minItemWidth;
        if ( isVertical() )
            posY -= direction * ( 1f + numberItemsBefore + numberItemsAfter ) / 2f * minItemHeight;
        posY -= normalPaint.getTextSize() / 2f;

        tmpCalendar.setTimeInMillis( time );
        addTimeUnits( tmpCalendar, timeUnit, -numberItemsBefore * timeUnitFactor );

        for ( int i = -numberItemsBefore; i <= numberItemsAfter; i++ )
        {
            final Date tmpTime = tmpCalendar.getTime();
            if ( fontSizeLinearGradient )
                normalPaint.setTextSize( fontSizeSelected + Math.abs( i ) * ( fontSizeNormal - fontSizeSelected ) );
            if ( i == 0 )
                canvas.drawText( dateFormat.format( tmpTime ), posX, posY, selectedPaint );
            else
                canvas.drawText( dateFormat.format( tmpTime ), posX, posY, normalPaint );
            if ( isHorizontal() )
                posX += direction * minItemWidth;
            if ( isVertical() )
                posY += direction * minItemHeight;
            addTimeUnits( tmpCalendar, timeUnit, timeUnitFactor );
        }
    }

    @Override
    public void onMeasure( int widthMeasureSpec, int heightMeasureSpec )
    {
        final int widthMode = MeasureSpec.getMode( widthMeasureSpec );
        final int widthSize = MeasureSpec.getSize( widthMeasureSpec );
        final int heightMode = MeasureSpec.getMode( heightMeasureSpec );
        final int heightSize = MeasureSpec.getSize( heightMeasureSpec );

        int itemHeight = minItemHeight;
        int itemWidth = minItemWidth;
        for ( int i = dateFormatStrings.length - 1; i >= 0; i-- )
        {
            final SimpleDateFormat dateFormat = dateFormats[i];
            itemWidth = Math.max( itemWidth, (int)selectedPaint.measureText( dateFormat.format( calendar.getTime() ) ) );
            itemWidth = Math.max( itemWidth, (int)normalPaint.measureText( dateFormat.format( calendar.getTime() ) ) );
        }

        int myWidth = itemWidth * ( isHorizontal() ? numItems : 1 );
        int myHeight = itemHeight * ( isVertical() ? numItems : 1 );

        if ( widthMode == MeasureSpec.EXACTLY )
            myWidth = widthSize;
        else if ( widthMode == MeasureSpec.AT_MOST )
            myWidth = Math.min( itemWidth * ( isHorizontal() ? numItems : 1 ), widthSize );

        if ( heightMode == MeasureSpec.EXACTLY )
            myHeight = heightSize;
        else if ( heightMode == MeasureSpec.AT_MOST )
            myHeight = Math.min( itemHeight * ( isVertical() ? numItems : 1 ), heightSize );

        setMeasuredDimension( myWidth, myHeight );
    }

    @Override
    protected void onSizeChanged( int w, int h, int oldW, int oldH )
    {
        centerX = w / 2f;
        centerY = h / 2f;
    }

    @Override
    public void setOnTimeScrollListener( @NonNull OnTimeScrollListener listener )
    {
        this.listener = listener;
        listener.onTimeScroll( time, this );
    }

    @Override
    public long getTime()
    {
        return time;
    }

    @Override
    public void setTime( long time )
    {
        this.time = time;
        calendar.setTimeInMillis( time );
        postInvalidate();
    }

    @Override
    public void setTimeZone( TimeZone timeZone )
    {
        this.timeZone = timeZone;
        initializeDateFormats( timeZone, locale );
        postInvalidate();
    }

    @Override
    public void setLocale( Locale locale )
    {
        this.locale = locale;
        initializeDateFormats( timeZone, locale );
    }

    @Override
    public void cycleTimeUnits()
    {
        setTimeUnitIndex( ( unitIndex + 1 ) % timeUnits.length, true );
    }

    @Override
    public String getCurrentScrollUnitName()
    {
        return timeUnitNames[unitIndex % timeUnitNames.length];
    }

    @Override
    public String getNextScrollUnitName()
    {
        return timeUnitNames[( unitIndex + 1 ) % timeUnitNames.length];
    }

    @Override
    public void resetScrolling()
    {
        setTimeUnitIndex( 0, false );
    }

    private boolean isHorizontal()
    {
        return orientation == Orientation.LEFT || orientation == Orientation.RIGHT;
    }

    private boolean isVertical()
    {
        return orientation == Orientation.UP || orientation == Orientation.DOWN;
    }

    private void setTimeUnitIndex( int unitIndex, boolean notifyListener )
    {
        this.unitIndex = unitIndex;
        dateFormat = dateFormats[unitIndex];
        parseTimeUnitString( timeUnits[unitIndex] );
        if ( notifyListener )
        {
            updateTime( time );
            listener.onTimeScroll( time, this );  // this is to notify the parents that we want to switch to manual time mode
            listener.onScrollUnitChanged( this );
        }
        postInvalidate();
    }

    private synchronized boolean updateTime( long continuousTime )
    {
        tmpCalendar.setTimeInMillis( continuousTime );
        switch ( timeUnit )
        {
            case Calendar.MILLISECOND:
                calendar.set( Calendar.MILLISECOND, tmpCalendar.get( Calendar.MILLISECOND ) );
            case Calendar.SECOND:
                calendar.set( Calendar.SECOND, tmpCalendar.get( Calendar.SECOND ) );
            case Calendar.MINUTE:
                calendar.set( Calendar.MINUTE, tmpCalendar.get( Calendar.MINUTE ) );
            case Calendar.HOUR_OF_DAY:
                calendar.set( Calendar.HOUR_OF_DAY, tmpCalendar.get( Calendar.HOUR_OF_DAY ) );
            case Calendar.DAY_OF_MONTH:
                calendar.set( Calendar.DAY_OF_MONTH, tmpCalendar.get( Calendar.DAY_OF_MONTH ) );
            case Calendar.MONTH:
                calendar.set( Calendar.MONTH, tmpCalendar.get( Calendar.MONTH ) );
            case Calendar.YEAR:
                int year = getYear( tmpCalendar );
                if ( year <= 0 )
                    year -= timeUnitFactor - 1;
                setYear( calendar, year / timeUnitFactor * timeUnitFactor );
        }
        final boolean timeChanged = calendar.getTimeInMillis() != time;
        time = calendar.getTimeInMillis();
        return timeChanged;
    }

    @Override
    public void computeScroll()
    {
        super.computeScroll();
        if ( scroller.computeScrollOffset() && updateTime( flingStartTime + (long)( millisPerScrolledPixel * scroller.getCurrX() ) ) )
            listener.onTimeChanged( time, this );
    }

    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        switch ( event.getAction() )
        {
            case MotionEvent.ACTION_DOWN:
                // Disallow ScrollView to intercept touch events.
                getParent().requestDisallowInterceptTouchEvent( true );
                break;
            case MotionEvent.ACTION_UP:
                // Allow ScrollView to intercept touch events.
                getParent().requestDisallowInterceptTouchEvent( false );
                break;
        }
        return gestureDetector.onTouchEvent( event ) || super.onTouchEvent( event );
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDown( MotionEvent event )
        {
            continuousTime = time;
            tmpCalendar.setTimeInMillis( time );
            scroller.forceFinished( true );
            postInvalidateOnAnimation();
            millisPerScrolledPixel = scrollSpeed * convertToMillis( timeUnit ) * timeUnitFactor
                    / ( isHorizontal() ? minItemWidth : minItemHeight );
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed( MotionEvent event )
        {
            listener.onScrollUnitChanged( TimeSlider.this );
            return true;
        }

        @Override
        public boolean onDoubleTap( MotionEvent event )
        {
            cycleTimeUnits();
            return true;
        }

        @Override
        public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY )
        {
            setTimeUnitIndex( unitIndex, true );
            switch ( orientation )
            {
                case LEFT:
                    continuousTime -= (long)( millisPerScrolledPixel * distanceX );
                    break;
                case RIGHT:
                    continuousTime += (long)( millisPerScrolledPixel * distanceX );
                    break;
                case UP:
                    continuousTime -= (long)( millisPerScrolledPixel * distanceY );
                    break;
                case DOWN:
                    continuousTime += (long)( millisPerScrolledPixel * distanceY );
                    break;
            }
            if ( updateTime( continuousTime ) )
            {
                listener.onTimeScroll( time, TimeSlider.this );
                postInvalidateOnAnimation();
            }
            return true;
        }

        @Override
        public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY )
        {
            flingStartTime = time;
            int velocity = 0;
            switch ( orientation )
            {
                case LEFT:
                    velocity = (int)velocityX;
                    break;
                case RIGHT:
                    velocity = -(int)velocityX;
                    break;
                case UP:
                    velocity = (int)velocityY;
                    break;
                case DOWN:
                    velocity = -(int)velocityY;
                    break;
            }
            scroller.fling( 0, 0, velocity, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0 );
            postInvalidateOnAnimation();
            return true;
        }
    }
}
