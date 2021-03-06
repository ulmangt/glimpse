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
package com.metsci.glimpse.swt.canvas;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.metsci.glimpse.canvas.GlimpseCanvas;
import com.metsci.glimpse.context.GlimpseBounds;
import com.metsci.glimpse.context.GlimpseContext;
import com.metsci.glimpse.context.GlimpseContextImpl;
import com.metsci.glimpse.context.GlimpseTarget;
import com.metsci.glimpse.context.GlimpseTargetStack;
import com.metsci.glimpse.layout.GlimpseLayout;
import com.metsci.glimpse.support.settings.LookAndFeel;
import com.metsci.glimpse.swt.event.mouse.MouseWrapperSWTBridge;

public class SwtBridgeGlimpseCanvas extends Composite implements GlimpseCanvas
{
    protected java.awt.Frame glFrame;
    protected GLCanvas glCanvas;

    protected Composite parent;

    protected List<GlimpseTarget> unmodifiableList;
    protected List<GlimpseLayout> layoutList;

    protected MouseWrapperSWTBridge mouseHelper;
    protected boolean isEventConsumer = true;
    protected boolean isEventGenerator = true;

    public SwtBridgeGlimpseCanvas( Composite parent )
    {
        this( parent, null );
    }

    public SwtBridgeGlimpseCanvas( Composite parent, GLContext context )
    {
        this( parent, context, SWT.EMBEDDED );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public SwtBridgeGlimpseCanvas( Composite parent, GLContext context, int style )
    {
        super( parent, style | SWT.EMBEDDED );

        try
        {
            // alleviates some of the flicker that occurs during resizing (affects Windows only)
            System.setProperty( "sun.awt.noerasebackground", "true" );
        }
        catch ( SecurityException e )
        {
        }

        this.parent = parent;

        if ( context == null )
        {
            this.glCanvas = new GLCanvas( );
        }
        else
        {
            this.glCanvas = new GLCanvas( null, null, context, null );
        }

        this.layoutList = new ArrayList<GlimpseLayout>( );

        // this is typesafe because unmodifiableList is unmodifiable, so it's not
        // possible to corrupt layoutList with non GlimpseLayouts
        this.unmodifiableList = ( List ) Collections.unmodifiableList( this.layoutList );

        this.mouseHelper = new MouseWrapperSWTBridge( this );
        this.glCanvas.addMouseListener( this.mouseHelper );
        this.glCanvas.addMouseMotionListener( this.mouseHelper );
        this.glCanvas.addMouseWheelListener( this.mouseHelper );

        this.addFocusListener( );

        // use an AWT to SWT wrapper to embed the AWT GLCanvas into the SWT
        // application
        // this works much more cleanly than using
        // org.eclipse.swt.opengl.GLCanvas
        // which has bugs, particularly related to context sharing
        this.glFrame = SWT_AWT.new_Frame( this );
        this.glFrame.add( this.glCanvas );

        this.addGLEventListener( this.glCanvas );
    }

    public GLCanvas getGLCanvas( )
    {
        return this.glCanvas;
    }

    @Override
    public GlimpseContext getGlimpseContext( )
    {
        return new GlimpseContextImpl( this );
    }

    @Override
    public void setLookAndFeel( LookAndFeel laf )
    {
        for ( GlimpseLayout layout : layoutList )
        {
            layout.setLookAndFeel( laf );
        }
    }

    @Override
    public void addLayout( GlimpseLayout layout )
    {
        this.layoutList.add( layout );
    }

    @Override
    public void removeLayout( GlimpseLayout layout )
    {
        this.layoutList.remove( layout );
    }

    @Override
    public List<GlimpseTarget> getTargetChildren( )
    {
        return this.unmodifiableList;
    }

    @Override
    public String toString( )
    {
        return SwtGlimpseCanvas.class.getSimpleName( );
    }

    @Override
    public boolean isEventConsumer( )
    {
        return this.isEventConsumer;
    }

    @Override
    public void setEventConsumer( boolean consume )
    {
        this.isEventConsumer = consume;
    }

    @Override
    public boolean isEventGenerator( )
    {
        return this.isEventGenerator;
    }

    @Override
    public void setEventGenerator( boolean generate )
    {
        this.isEventGenerator = generate;
    }

    @Override
    public GlimpseBounds getTargetBounds( GlimpseTargetStack stack )
    {
        Dimension dimension = getDimension( );

        if ( dimension != null )
        {
            return new GlimpseBounds( getDimension( ) );
        }
        else
        {
            return null;
        }
    }

    public Dimension getDimension( )
    {
        return this.glCanvas.getSize( );
    }

    @Override
    public GlimpseBounds getTargetBounds( )
    {
        return getTargetBounds( null );
    }

    @Override
    public GLContext getGLContext( )
    {
        return this.glCanvas.getContext( );
    }

    @Override
    public void paint( )
    {
        glCanvas.display( );
    }

    // In linux, the component the mouse pointer is over receives mouse wheel
    // events
    // In windows, the component with focus receives mouse wheel events
    // These listeners emulate linux-like mouse wheel event dispatch for
    // important components
    // This causes the application to work in slightly un-windows-like ways
    // some of the time, but the effect is minor.
    protected void addFocusListener( )
    {
        glCanvas.addMouseListener( new MouseAdapter( )
        {
            public void requestFocus( )
            {
                // we want the glCanvas to have AWT focus and this Composite to have SWT focus
                Display.getDefault( ).syncExec( new Runnable( )
                {
                    public void run( )
                    {
                        forceFocus( );

                    }
                } );

                glCanvas.requestFocus( );
            }

            public void mouseClicked( MouseEvent e )
            {
                requestFocus( );
            }

            public void mousePressed( MouseEvent e )
            {
                requestFocus( );
            }

            public void mouseReleased( MouseEvent e )
            {
                requestFocus( );
            }

            public void mouseEntered( MouseEvent e )
            {
                requestFocus( );
            }

            public void mouseExited( MouseEvent e )
            {
            }

            public void mouseWheelMoved( MouseWheelEvent e )
            {
                requestFocus( );
            }

            public void mouseDragged( MouseEvent e )
            {
                requestFocus( );
            }

            public void mouseMoved( MouseEvent e )
            {
                requestFocus( );
            }
        } );
    }

    private void addGLEventListener( GLAutoDrawable drawable )
    {
        drawable.addGLEventListener( new GLEventListener( )
        {
            @Override
            public void init( GLAutoDrawable drawable )
            {
                // do nothing
            }

            @Override
            public void display( GLAutoDrawable drawable )
            {
                for ( GlimpseLayout layout : layoutList )
                {
                    layout.paintTo( getGlimpseContext( ) );
                }
            }

            @Override
            public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height )
            {
                for ( GlimpseLayout layout : layoutList )
                {
                    layout.layoutTo( getGlimpseContext( ) );
                }
            }

            @Override
            public void displayChanged( GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged )
            {
                // do nothing
            }
        } );
    }
}
