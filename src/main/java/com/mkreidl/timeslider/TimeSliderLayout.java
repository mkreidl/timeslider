package com.mkreidl.timeslider;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TimeSliderLayout extends LinearLayout
        implements TimeScrollable, TimeScrollable.OnTimeScrollListener
{
    private long time;
    private TimeZone timeZone = TimeZone.getDefault();
    private Locale locale = Locale.getDefault();

    protected List<TimeScrollable> subSliders = new ArrayList<>();
    protected TimeScrollable activeScrollable;
    protected TimeScrollable.OnTimeScrollListener listener = new OnTimeScrollListener()
    {
        // Null object pattern
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

    public TimeSliderLayout( Context context )
    {
        super( context );
    }

    public TimeSliderLayout( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }

    @Override
    public void onFinishInflate()
    {
        super.onFinishInflate();
        locale = Locale.getDefault();
        for ( int i = 0; i < getChildCount(); ++i )
            if ( getChildAt( i ) instanceof TimeScrollable )
            {
                final TimeScrollable subSlider = (TimeScrollable)getChildAt( i );
                subSliders.add( subSlider );
                subSlider.setOnTimeScrollListener( this );
                subSlider.setTimeZone( timeZone );
                subSlider.setLocale( locale );
            }
    }

    @Override
    public void onTimeScroll( long time, @Nullable TimeScrollable source )
    {
        synchronizeTime( time, source );
        listener.onTimeScroll( time, activeScrollable );
    }

    @Override
    public void onTimeChanged( long time, @Nullable TimeScrollable source )
    {
        synchronizeTime( time, source );
        listener.onTimeChanged( time, activeScrollable );
    }

    private void synchronizeTime( long time, @Nullable TimeScrollable source )
    {
        this.time = time;
        activeScrollable = source;
        for ( TimeScrollable subSlider : subSliders )
            if ( subSlider != activeScrollable )
                subSlider.setTime( time );
    }

    @Override
    public void onScrollUnitChanged( @Nullable TimeScrollable source )
    {
        activeScrollable = source;
        listener.onScrollUnitChanged( source );
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
        for ( TimeScrollable subSlider : subSliders )
            subSlider.setTime( time );
    }

    @Override
    public void setTimeZone( TimeZone timeZone )
    {
        this.timeZone = timeZone;
        for ( TimeScrollable subSlider : subSliders )
            subSlider.setTimeZone( timeZone );
    }

    @Override
    public void setLocale( Locale locale )
    {
        this.locale = locale;
        for ( TimeScrollable subSlider : subSliders )
            subSlider.setLocale( locale );
    }

    @Override
    public void cycleTimeUnits()
    {
        if ( activeScrollable != null )
            activeScrollable.cycleTimeUnits();
    }

    @Override
    public String getCurrentScrollUnitName()
    {
        return activeScrollable != null ? activeScrollable.getCurrentScrollUnitName() : null;
    }

    @Override
    public String getNextScrollUnitName()
    {
        return activeScrollable != null ? activeScrollable.getNextScrollUnitName() : null;
    }

    @Override
    public void setOnTimeScrollListener( @NonNull OnTimeScrollListener listener )
    {
        this.listener = listener;
    }

    @Override
    public void resetScrolling()
    {
        if ( activeScrollable != null )
            activeScrollable.resetScrolling();
    }
}
