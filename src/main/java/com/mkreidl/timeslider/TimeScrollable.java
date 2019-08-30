package com.mkreidl.timeslider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.TimeZone;


public interface TimeScrollable
{
    interface OnTimeScrollListener
    {
        void onTimeScroll( long time, @Nullable TimeScrollable source );

        void onTimeChanged( long time, @Nullable TimeScrollable source );

        void onScrollUnitChanged( @Nullable TimeScrollable source );
    }

    void setOnTimeScrollListener( @NonNull OnTimeScrollListener listener );

    long getTime();

    void setTime( long time );

    void setTimeZone( TimeZone timeZone );

    void setLocale( Locale locale );

    void cycleTimeUnits();

    void resetScrolling();

    String getCurrentScrollUnitName();

    String getNextScrollUnitName();
}
