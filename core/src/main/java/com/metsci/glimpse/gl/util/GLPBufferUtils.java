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
package com.metsci.glimpse.gl.util;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;


public class GLPBufferUtils
{
    private GLPBufferUtils( )
    {
    }

    public static boolean canCreateGLPbuffer( )
    {
        return GLDrawableFactory.getFactory().canCreateGLPbuffer();
    }

    public static GLPbuffer createPixelBuffer( int width, int height )
    {
        return createPixelBuffer( width, height, null, null );
    }

    public static GLPbuffer createPixelBuffer( int width, int height, GLContext context )
    {
        return createPixelBuffer( width, height, context, null );
    }

    public static GLPbuffer createPixelBuffer( int width, int height, GLCapabilities cap )
    {
        return createPixelBuffer( width, height, null, cap );
    }

    public static GLPbuffer createPixelBuffer( int width, int height, GLContext context, GLCapabilities cap )
    {
        if( !canCreateGLPbuffer() )
        {
            return null;
        }

        GLDrawableFactory fac = GLDrawableFactory.getFactory();

        if( cap == null )
        {
            cap = new GLCapabilities();
            cap.setDoubleBuffered( false );
            cap.setDepthBits( 0 );
            cap.setAlphaBits( 0 );
            cap.setRedBits( 8 );
            cap.setGreenBits( 8 );
            cap.setBlueBits( 8 );
        }

        GLPbuffer buffer = fac.createGLPbuffer( cap, null, width, height, context );
        return buffer;
    }

    public static void destroyPixelBuffer( GLPbuffer buffer )
    {
        if( buffer != null )
        {
            GLContext context = buffer.getContext();
            if( context != null )
            {
                context.destroy();
            }

            buffer.destroy();
        }
    }
}
