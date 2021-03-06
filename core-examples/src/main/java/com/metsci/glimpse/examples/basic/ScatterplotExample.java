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
package com.metsci.glimpse.examples.basic;

import static com.metsci.glimpse.axis.tagged.Tag.TEX_COORD_ATTR;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.metsci.glimpse.axis.Axis1D;
import com.metsci.glimpse.axis.listener.mouse.AxisMouseListener;
import com.metsci.glimpse.axis.painter.label.GridAxisLabelHandler;
import com.metsci.glimpse.axis.tagged.TaggedAxis1D;
import com.metsci.glimpse.axis.tagged.TaggedAxisMouseListener1D;
import com.metsci.glimpse.axis.tagged.painter.TaggedPartialColorYAxisPainter;
import com.metsci.glimpse.axis.tagged.shader.TaggedPointShader;
import com.metsci.glimpse.examples.Example;
import com.metsci.glimpse.gl.attribute.GLFloatBuffer;
import com.metsci.glimpse.gl.attribute.GLFloatBuffer.Mutator;
import com.metsci.glimpse.gl.attribute.GLFloatBuffer2D;
import com.metsci.glimpse.gl.shader.Pipeline;
import com.metsci.glimpse.gl.texture.ColorTexture1D;
import com.metsci.glimpse.gl.texture.ColorTexture1D.MutatorColor1D;
import com.metsci.glimpse.gl.texture.FloatTexture1D;
import com.metsci.glimpse.gl.texture.FloatTexture1D.MutatorFloat1D;
import com.metsci.glimpse.layout.GlimpseLayoutProvider;
import com.metsci.glimpse.painter.decoration.BorderPainter;
import com.metsci.glimpse.painter.decoration.GridPainter;
import com.metsci.glimpse.painter.shape.ShadedPointPainter;
import com.metsci.glimpse.plot.MultiAxisPlot2D;
import com.metsci.glimpse.plot.MultiAxisPlot2D.AxisInfo;
import com.metsci.glimpse.support.colormap.ColorGradient;
import com.metsci.glimpse.support.colormap.ColorGradients;
import com.metsci.glimpse.util.math.stochastic.Generator;
import com.metsci.glimpse.util.math.stochastic.StochasticEngineMersenne;
import com.metsci.glimpse.util.math.stochastic.pdfcont.PdfCont;
import com.metsci.glimpse.util.math.stochastic.pdfcont.PdfContGaussianZiggurat;
import com.metsci.glimpse.util.math.stochastic.pdfcont.PdfContUniform;

/**
 * A scatter plot with adjustable point size and color. Usage of GLSL
 * shaders allows the color and size of millions of data points to be
 * adjusted dynamically based on the color and size scale axes to the
 * right of the plot.
 *
 * @author ulman
 */
public class ScatterplotExample implements GlimpseLayoutProvider
{
    public static int NUM_POINTS = 1000000;

    public static void main( String[] args ) throws Exception
    {
        Example.showWithSwing( new ScatterplotExample( ) );
    }

    @Override
    public MultiAxisPlot2D getLayout( )
    {
        MultiAxisPlot2D plot = new MultiAxisPlot2D( );

        // create a x axis below the plot
        plot.createAxisBottom( "x_axis" );

        // create a y axis to the left of the plot
        plot.createAxisLeft( "y_axis" );

        // link "x_axis" and "y_axis" to the central plot axes, so that they move together
        plot.getAxis( "x_axis" ).setParent( plot.getCenterAxisX( ) );
        plot.getAxis( "y_axis" ).setParent( plot.getCenterAxisY( ) );

        // set the x and y axis bounds
        plot.getAxis( "x_axis" ).setMin( 0.0 );
        plot.getAxis( "x_axis" ).setMax( 4.0 );

        plot.getAxis( "y_axis" ).setMin( -200.0 );
        plot.getAxis( "y_axis" ).setMax( 1000.0 );
        
        // validate propagates the axis bounds set above to all linked axes
        // this should be called whenever axis bounds are set programmatically
        plot.getAxis( "x_axis" ).validate( );
        plot.getAxis( "y_axis" ).validate( );
        
        // create an axis with a custom Axis1D and AxisMouseListener (which support tags)
        TaggedAxis1D colorAxis = new TaggedAxis1D( );
        AxisMouseListener colorMouseListener = new TaggedAxisMouseListener1D( );

        // create a color scale axis to the right of the plot
        AxisInfo colorAxisInfo = plot.createAxisRight( "color_axis", colorAxis, colorMouseListener );

        // add a custom painter to display the axis tags
        GridAxisLabelHandler colorTickHandler = new GridAxisLabelHandler( );
        TaggedPartialColorYAxisPainter colorTagPainter = new TaggedPartialColorYAxisPainter( colorTickHandler );
        plot.getAxisInfo( "color_axis" ).setAxisPainter( colorTagPainter );

        // add some tags to the color axis
        colorAxis.addTag( "Tag1", 700.0 ).setAttribute( TEX_COORD_ATTR, 0.0f );
        colorAxis.addTag( "Tag2", 4000.0 ).setAttribute( TEX_COORD_ATTR, 1.0f );

        // set the bounds of the color axis
        colorAxis.setMin( -2000.0 );
        colorAxis.setMax( 6000.0 );
        
        // set the label and size of the color axis
        colorAxisInfo.getAxisPainter( ).setAxisLabel( "Color Axis" );
        colorAxisInfo.setSize( 80 );

        // create another tagged axis for the size axis (controls the size of the points)
        TaggedAxis1D sizeAxis = new TaggedAxis1D( );
        AxisMouseListener sizeMouseListener = new TaggedAxisMouseListener1D( );

        // create a color scale axis to the right of the plot
        AxisInfo as = plot.createAxisRight( "size_axis", sizeAxis, sizeMouseListener );

        // add a custom painter to display the axis tags
        GridAxisLabelHandler sizeTickHandler = new GridAxisLabelHandler( );
        TaggedPartialColorYAxisPainter sizeTagPainter = new TaggedPartialColorYAxisPainter( sizeTickHandler );
        plot.getAxisInfo( "size_axis" ).setAxisPainter( sizeTagPainter );

        // add some tags to the axis
        sizeAxis.addTag( "Tag1", 0.8 ).setAttribute( TEX_COORD_ATTR, 0.0f );
        sizeAxis.addTag( "Tag2", 4.0 ).setAttribute( TEX_COORD_ATTR, 1.0f );

        // set the bounds of the size axis
        sizeAxis.setMin( -2.0 );
        sizeAxis.setMax( 5.0 );
        
        // set the label and size of the size axis
        as.getAxisPainter( ).setAxisLabel( "Size Axis" );
        as.setSize( 65 );

        // setup the color map for the painter and axis
        ColorTexture1D colorMapTexture = new ColorTexture1D( 1024 );
        
        // use the predefined bathymetry color gradient (which is a dark
        // blue to light blue color gradient) but set the alpha value
        // to a constant 0.6
        colorMapTexture.setColorGradient( new ColorGradient( )
        {
            @Override
            public void toColor( float fraction, float[] rgba )
            {
                ColorGradients.bathymetry.toColor( fraction, rgba );
                rgba[3] = 0.6f;
            }
            
        } );

        // tell the color axis painter to use the color scale we just created
        colorTagPainter.setColorScale( colorMapTexture );

        // setup the color map for the size painter (simple flat color)
        ColorTexture1D sizeTexture = new ColorTexture1D( 1 );
        sizeTexture.mutate( new MutatorColor1D( )
        {
            @Override
            public void mutate( FloatBuffer floatBuffer, int dim )
            {
                floatBuffer.put( 0.0f );
                floatBuffer.put( 0.0f );
                floatBuffer.put( 1.0f );
                floatBuffer.put( 0.4f );
            }
        } );

        // tell the size axis painter to use the color scale we just created
        sizeTagPainter.setColorScale( sizeTexture );

        // setup the size map for the painter (determines how size attribute
        // values get mapped to pixel sizes of points)
        FloatTexture1D sizeMapTexture = new FloatTexture1D( 256 );
        final MutatorFloat1D sizeMutator = new MutatorFloat1D( )
        {
            @Override
            public void mutate( FloatBuffer data, int n0 )
            {
                float minSize = 0.0f;
                float maxSize = 20.0f;
                float dSize = maxSize - minSize;

                data.clear( );
                for ( int i = 0; i < data.capacity( ); i++ )
                {
                    data.put( ( float ) ( minSize + dSize * ( (float) i / (float) data.capacity( )) ) );
                }
            }
        };
        sizeMapTexture.mutate( sizeMutator );

        // create a grid painter and have the grid lines follow the "x_axis" and "y_axis" axes
        plot.addPainter( new GridPainter( plot.getAxisInfo( "x_axis" ).getTickHandler( ), plot.getAxisInfo( "y_axis" ).getTickHandler( ) ) );

        // add a painter to display the scatterplot data
        // because the MultiAxisPlot2D can have many axes we must
        // be explicit about which axes should be used for
        // x, y, size, and color by the ShadedPointPainter
        ShadedPointPainter painter;
        try
        {
            painter = new ShadedPointPainter( plot.getAxis( "color_axis" ), plot.getAxis( "size_axis" ) )
            {
                @Override
                protected void initShaderPipeline( Axis1D colorAxis, Axis1D sizeAxis ) throws IOException
                {
                    vertShader = new TaggedPointShader( 0, 1, colorAttributeIndex, sizeAttributeIndex, ( TaggedAxis1D ) colorAxis, ( TaggedAxis1D ) sizeAxis );
                    pipeline = new Pipeline( "pointshader", null, vertShader, null );
                }
            };
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new RuntimeException( e );
        }

        // add the painter to the plot
        plot.addPainter( painter );

        // add a simple border painter to the main plot area
        plot.addPainter( new BorderPainter( ) );

        // generate a fast source of randomness for generating data
        final Generator g = StochasticEngineMersenne.createEngine( 1234567890 ).getGenerator( );

        // setup the x y position data for the points
        GLFloatBuffer2D xyValues = new GLFloatBuffer2D( NUM_POINTS );
        xyValues.mutate( new Mutator( )
        {
            @Override
            public void mutate( FloatBuffer data, int length )
            {
                PdfCont dist = new PdfContGaussianZiggurat( 15, 20 );

                data.clear( );
                for ( int i = 0; i < NUM_POINTS; i++ )
                {
                    float x = 6.0f * i / (float) NUM_POINTS;
                    float y = f( x, dist );
                    
                    data.put( x );
                    data.put( y );
                }
            }
            
            public float f( float x, PdfCont rand )
            {
                return (float) ( Math.exp( x ) * 10.0 + rand.draw( g ) * x );
            }
        } );

        // setup the color value data for the points
        GLFloatBuffer colorValues = new GLFloatBuffer( NUM_POINTS, 1 );
        colorValues.mutate( new Mutator( )
        {
            @Override
            public void mutate( FloatBuffer data, int length )
            {
                PdfCont dist = new PdfContUniform( 0, 500 );

                data.clear( );
                for ( int i = 0; i < NUM_POINTS; i++ )
                {
                    float x = 6.0f * i / (float) NUM_POINTS;
                    float y = f( x, dist );
                    
                    data.put( (float) ( x * ( y + dist.draw( g ) ) ) );
                }
            }
            
            public float f( float x, PdfCont rand )
            {
                return (float) ( Math.exp( x ) * 10.0 + rand.draw( g ) );
            }
        } );

        // setup the size value data for the points
        GLFloatBuffer sizeValues = new GLFloatBuffer( NUM_POINTS, 1 );
        sizeValues.mutate( new Mutator( )
        {
            @Override
            public void mutate( FloatBuffer data, int length )
            {
                PdfCont dist = new PdfContUniform( 0, 1 );

                data.clear( );
                for ( int i = 0; i < NUM_POINTS; i++ )
                {
                    data.put( (float) dist.draw( g ) );
                }
            }
        } );

        // add the data arrays for xy position, color, and size attributes to the painter
        painter.useVertexPositionData( xyValues );
        painter.useColorAttribData( colorValues );
        painter.useSizeAttribData( sizeValues );

        // set the textures which determine how attribute values are mapped to point sizes and colors
        painter.useColorScale( colorMapTexture );
        painter.useSizeScale( sizeMapTexture );

        // set the painter to not draw points whose color attribute value is outside the range
        // defined by the axis tags (if false, the color would just saturate)
        painter.setDiscardAboveColor( true );
        painter.setDiscardBelowColor( true );

        return plot;
    }
}
