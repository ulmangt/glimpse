/*
 * Copyright (c) 2012, Metron, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Metron, Inc. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL METRON, INC. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.metsci.glimpse.axis.painter;

import static com.metsci.glimpse.util.units.time.TimeStamp.currentTime;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.TimeZone;

import javax.media.opengl.GL;

import com.metsci.glimpse.axis.Axis1D;
import com.metsci.glimpse.context.GlimpseBounds;
import com.metsci.glimpse.context.GlimpseContext;
import com.metsci.glimpse.plot.timeline.data.Epoch;
import com.metsci.glimpse.support.color.GlimpseColor;
import com.metsci.glimpse.util.units.time.Time;
import com.metsci.glimpse.util.units.time.TimeStamp;
import com.metsci.glimpse.util.units.time.format.TimeStampFormat;

/**
 * A horizontal (x) time axis painter.
 *
 * @author ulman
 * @see TimeAxisPainter
 */
public class TimeXAxisPainter extends TimeAxisPainter
{
    public TimeXAxisPainter( Epoch epoch )
    {
        this( defaultMinuteSecondFormat, defaultHourDayMonthFormat, defaultHourMinuteFormat, defaultDayMonthYearFormat, defaultDayFormat, defaultMonthFormat, defaultMonthYearFormat, defaultYearFormat, defaultTimeZone, epoch );
    }

    //@formatter:off
    public TimeXAxisPainter( TimeStampFormat minuteSecondFormat,
                             TimeStampFormat hourMinuteFormat,
                             TimeStampFormat hourDayMonthFormat,
                             TimeStampFormat dayMonthYearFormat,
                             TimeStampFormat dayFormat,
                             TimeStampFormat dayMonthFormat,
                             TimeStampFormat monthYearFormat,
                             TimeStampFormat yearFormat,
                             TimeZone timeZone, Epoch epoch )
    {
        super( minuteSecondFormat, hourMinuteFormat, hourDayMonthFormat, dayMonthYearFormat, dayFormat, dayMonthFormat, monthYearFormat, yearFormat, timeZone, epoch );
    }
    //@formatter:on

    @Override
    public void paintTo( GlimpseContext context, GlimpseBounds bounds, Axis1D axis )
    {
        GL gl = context.getGL( );

        int width = bounds.getWidth( );
        int height = bounds.getHeight( );

        if ( width == 0 || height == 0 ) return;

        gl.glMatrixMode( GL.GL_PROJECTION );
        gl.glLoadIdentity( );
        gl.glOrtho( -0.5, width - 1 + 0.5f, -0.5, height - 1 + 0.5f, -1, 1 );
        gl.glMatrixMode( GL.GL_MODELVIEW );
        gl.glLoadIdentity( );

        gl.glColor4fv( tickColor, 0 );

        List<TimeStamp> tickTimes = tickTimes( axis, width );
        double tickInterval = tickInterval( tickTimes );

        // Tick marks
        gl.glBegin( GL.GL_LINES );
        for ( TimeStamp t : tickTimes )
        {
            double x = axis.valueToScreenPixel( fromTimeStamp( t ) );
            gl.glVertex2d( x, height );
            gl.glVertex2d( x, height - tickLineLength );
        }
        gl.glEnd( );

        if ( showCurrentTimeLabel ) drawCurrentTimeTick( gl, axis, width, height );

        GlimpseColor.setColor( textRenderer, textColor );
        textRenderer.beginRendering( width, height );
        try
        {
            if ( tickInterval <= Time.fromMinutes( 1 ) )
            {
                // Time labels
                double jTimeText = printTickLabels( tickTimes, axis, minuteSecondFormat, width, height );

                // Date labels
                printHoverLabels( tickTimes, axis, hourDayMonthFormat, hourStructFactory, jTimeText, width, height );
            }
            else if ( tickInterval <= Time.fromHours( 12 ) )
            {
                // Time labels
                double jTimeText = printTickLabels( tickTimes, axis, hourMinuteFormat, width, height );

                // Date labels
                printHoverLabels( tickTimes, axis, dayMonthYearFormat, dayStructFactory, jTimeText, width, height );
            }
            else if ( tickInterval <= Time.fromDays( 10 ) )
            {
                // Date labels
                double jTimeText = printTickLabels( tickTimes, axis, dayFormat, width, height );

                // Year labels
                printHoverLabels( tickTimes, axis, monthYearFormat, monthStructFactory, jTimeText, width, height );
            }
            else if ( tickInterval <= Time.fromDays( 60 ) )
            {
                // Date labels
                double jTimeText = printTickLabels( tickTimes, axis, monthFormat, width, height );

                // Year labels
                printHoverLabels( tickTimes, axis, yearFormat, yearStructFactory, jTimeText, width, height );
            }
            else
            {
                // Date labels
                printTickLabels( tickTimes, axis, yearFormat, width, height );
            }
        }
        finally
        {
            textRenderer.endRendering( );
        }
    }

    private void printHoverLabels( List<TimeStamp> tickTimes, Axis1D axis, TimeStampFormat format, TimeStructFactory factory, double jTimeText, int width, int height )
    {
        // text heights vary slightly, making the labels appear unevenly spaced in height
        // just use the height of a fixed sample character
        Rectangle2D fixedBounds = textRenderer.getBounds( "M" );
        double textHeight = fixedBounds.getHeight( );

        // Date labels
        List<TimeStruct> timeStruts = timeStructs( axis, tickTimes, factory );
        for ( TimeStruct time : timeStruts )
        {
            String text = time.textCenter.toString( format );
            Rectangle2D textBounds = textRenderer.getBounds( text );
            double textWidth = textBounds.getWidth( );

            int iMin = axis.valueToScreenPixel( fromTimeStamp( time.start ) );
            int iMax = ( int ) Math.floor( axis.valueToScreenPixel( fromTimeStamp( time.end ) ) - textWidth );
            int iApprox = ( int ) Math.round( axis.valueToScreenPixel( fromTimeStamp( time.textCenter ) ) - 0.5 * textWidth );
            int i = Math.max( iMin, Math.min( iMax, iApprox ) );
            if ( i < 0 || i + textWidth > width ) continue;

            int j = ( int ) Math.floor( jTimeText - textHeight - hoverLabelOffset );

            textRenderer.draw( text, i, j );
        }
    }

    private double printTickLabels( List<TimeStamp> tickTimes, Axis1D axis, TimeStampFormat format, int width, int height )
    {
        // text heights vary slightly, making the labels appear unevenly spaced in height
        // just use the height of a fixed sample character
        Rectangle2D fixedBounds = textRenderer.getBounds( "M" );
        double textHeight = fixedBounds.getHeight( );

        // Time labels
        int jTimeText = Integer.MAX_VALUE;
        for ( TimeStamp t : tickTimes )
        {
            String string = t.toString( format );
            Rectangle2D textBounds = textRenderer.getBounds( string );

            double textWidth = textBounds.getWidth( );
            int i = ( int ) Math.round( axis.valueToScreenPixel( fromTimeStamp( t ) ) - 0.5 * textWidth );
            if ( i < 0 || i + textWidth > width ) continue;

            int j = ( int ) Math.round( height - tickLineLength - textHeight );
            jTimeText = Math.min( jTimeText, j );

            textRenderer.draw( string, i, j );
        }

        return jTimeText;
    }

    private void drawCurrentTimeTick( GL gl, Axis1D axis, int width, int height )
    {
        int iTick = axis.valueToScreenPixel( fromTimeStamp( currentTime( ) ) );

        gl.glColor4fv( currentTimeTickColor, 0 );
        gl.glLineWidth( currentTimeLineThickness );
        gl.glBegin( GL.GL_LINES );
        gl.glVertex2d( iTick, height );
        gl.glVertex2d( iTick, 0 );
        gl.glEnd( );

        String text = "NOW";

        GlimpseColor.setColor( textRenderer, currentTimeTextColor );
        textRenderer.beginRendering( width, height );
        try
        {
            textRenderer.draw( text, iTick + 3, 0 + 3 );
        }
        finally
        {
            textRenderer.endRendering( );
        }
    }
}
