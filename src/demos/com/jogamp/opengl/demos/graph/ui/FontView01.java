/**
 * Copyright 2023 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.demos.graph.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Margin;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.Vec2i;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;

/**
 * This may become a little font viewer application, having FontForge as its role model.
 */
public class FontView01 {
    private static final float GlyphGridWidth = 2/3f;
    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);
    // static CommandlineOptions options = new CommandlineOptions(1920, 1080, Region.VBAA_RENDERING_BIT);

    private static boolean VERBOSE_GLYPHS = false;
    private static boolean VERBOSE_UI = false;

    public static void main(final String[] args) throws IOException {
        float mmPerCell = 8f;
        String fontfilename = null;
        final Vec2i rawGridSize = new Vec2i(-1, -1);
        final boolean[] showUnderline = { false };

        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-font")) {
                    idx[0]++;
                    fontfilename = args[idx[0]];
                } else if(args[idx[0]].equals("-mmPerCell")) {
                    idx[0]++;
                    mmPerCell = MiscUtils.atof(args[idx[0]], mmPerCell);
                } else if(args[idx[0]].equals("-gridCols")) {
                    idx[0]++;
                    rawGridSize.setX( MiscUtils.atoi(args[idx[0]], rawGridSize.x()) );
                } else if(args[idx[0]].equals("-gridRows")) {
                    idx[0]++;
                    rawGridSize.setY( MiscUtils.atoi(args[idx[0]], rawGridSize.y()) );
                } else if(args[idx[0]].equals("-showUnderline")) {
                    showUnderline[0] = true;
                }
            }
        }
        System.err.println(options);

        Font font;
        if( null == fontfilename ) {
            font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                                   FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        } else {
            font = FontFactory.get( new File( fontfilename ) );
        }
        System.err.println("Font "+font.getFullFamilyName());

        final Font fontStatus = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        final Font fontInfo = FontFactory.get(FontFactory.UBUNTU).getDefault();
        System.err.println("Status Font "+fontStatus.getFullFamilyName());

        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        if( options.sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(options.sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(1*60, null); // System.err);
        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(FontView01.class.getSimpleName()+": "+font.getFullFamilyName()+", "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(FontView01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });
        animator.add(window);

        final Scene scene = new Scene(options.graphAASamples);
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setFrustumCullingEnabled(true);

        scene.attachInputListenerTo(window);
        window.addGLEventListener(scene);

        final float[] ppmm = window.getPixelsPerMM(new float[2]);
        {
            final float[] dpi = FontScale.ppmmToPPI( new float[] { ppmm[0], ppmm[1] } );
            System.err.println("DPI "+dpi[0]+" x "+dpi[1]+", "+ppmm[0]+" x "+ppmm[1]+" pixel/mm");

            final float[] hasSurfacePixelScale1 = window.getCurrentSurfaceScale(new float[2]);
            System.err.println("HiDPI PixelScale: "+hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
            System.err.println("mmPerCell "+mmPerCell);
        }
        if( 0 >= rawGridSize.x() ) {
            rawGridSize.setX( (int)( ( window.getSurfaceWidth() * GlyphGridWidth / ppmm[0] ) / mmPerCell ) );
        }
        if( 0 >= rawGridSize.y() ) {
            rawGridSize.setY( (int)( ( window.getSurfaceHeight() / ppmm[1] ) / mmPerCell ) );
        }
        final GridDim gridDim = new GridDim(font, rawGridSize, 1, 0);
        System.err.println("GridDim "+gridDim);

        final Group mainGrid;
        final Group glyphGrid;
        final Shape.MouseGestureListener glyphMouseListener;
        {
            final Group glyphShapeBox = new Group( new BoxLayout( 1f, 1f, Alignment.FillCenter, new Margin(0.025f) ) );
            glyphShapeBox.addShape( new GlyphShape(options.renderModes, 'A', font.getGlyph( font.getGlyphID('A')), 0, 0) );

            final Group glyphInfoBox = new Group( new BoxLayout( 1f, 1f, Alignment.FillCenter ) ); // , new Margin(0.05f, 0.025f, 0.05f, 0.025f) ) );
            final Label glyphInfo = new Label(options.renderModes, fontStatus, "Nothing there yet");
            setGlyphInfo(fontStatus, glyphInfo, 'A', font.getGlyph(font.getGlyphID('A')));
            glyphInfo.setColor(0.1f, 0.1f, 0.1f, 1.0f);
            glyphInfoBox.addShape(glyphInfo);

            glyphMouseListener = new Shape.MouseGestureAdapter() {
                private void handleEvent(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    // System.err.println("ShapeEvent "+shapeEvent);
                    final Shape shape = shapeEvent.shape;
                    if( !( shape instanceof Group ) ) {
                        return;
                    }
                    final Group group = (Group)shape;
                    final int last = group.getShapes().size()-1;
                    if( 0 > last ) {
                        return;
                    }
                    final Shape lastShape = group.getShapes().get(last);
                    if( !( lastShape instanceof GlyphShape ) ) {
                        return;
                    }
                    // Selected Glyph
                    final GlyphShape g0 = (GlyphShape)lastShape;
                    final boolean doScreenshot = e.isControlDown() && e.getButtonDownCount() > 0;
                    scene.invoke(false, (final GLAutoDrawable d) -> {
                        // Handle old one
                        if( 1 == glyphShapeBox.getShapes().size() ) {
                            final GlyphShape old = (GlyphShape) glyphShapeBox.getShapes().get(0);
                            if( null != old ) {
                                if( !doScreenshot && old.getSymbol() == g0.getSymbol() ) {
                                    // System.err.println("GlyphShape Same: "+old);
                                    return true; // abort - no change
                                }
                                // System.err.println("GlyphShape Old: "+old);
                                glyphShapeBox.removeShape(old);
                                old.destroy(d.getGL().getGL2ES2(), scene.getRenderer());
                            } else {
                                System.err.println("GlyphShape Old: Null");
                            }
                        } else {
                            System.err.println("GlyphShape Old: None");
                        }
                        // New Glyph
                        // System.err.println("GlyphShape New "+g0);
                        final GlyphShape gs = new GlyphShape( g0 ); // copy GlyphShape
                        gs.setColor(0, 0, 0, 1);
                        glyphShapeBox.addShape( gs );
                        setGlyphInfo(fontStatus, glyphInfo, gs.getSymbol(), gs.getGlyph());
                        glyphInfo.validate(d.getGL().getGL2ES2()); // avoid group re-validate
                        // System.err.println("GlyphInfo "+glyphInfo.getBounds());
                        if( doScreenshot ) {
                            printScreenOnGLThread(scene, window.getChosenGLCapabilities(), font, gs.getGlyph().getID());
                        }
                        return true;
                    });
                }
                @Override
                public void mouseMoved(final MouseEvent e) {
                    handleEvent(e);
                }
                @Override
                public void mousePressed(final MouseEvent e) {
                    handleEvent(e);
                }
            };
            {
                final float cellSize = gridDim.rawSize.x() > gridDim.rawSize.y() ? GlyphGridWidth/gridDim.rawSize.x() : GlyphGridWidth/gridDim.rawSize.y();
                // final float gapSizeX = ( gridDim.rawSize.x() - 1 ) * cellSize * 0.1f;
                System.err.println("Grid "+gridDim+", scale "+cellSize);
                glyphGrid = new Group(new GridLayout(gridDim.rawSize.x(), cellSize*0.9f, cellSize*0.9f, Alignment.FillCenter, new Gap(cellSize*0.1f)));
            }

            addGlyphs(reqGLP, font, glyphGrid, gridDim, showUnderline[0], fontStatus, glyphMouseListener);
            glyphGrid.validate(reqGLP);
            System.err.println("GlyphGrid "+glyphGrid);
            System.err.println("GlyphGrid "+glyphGrid.getLayout());

            final float infoCellWidth = 1f - GlyphGridWidth;
            final float infoCellHeight = glyphGrid.getBounds().getHeight() / 2f;
            final Group infoGrid = new Group( new GridLayout(1, infoCellWidth, infoCellHeight * 0.98f, Alignment.FillCenter, new Gap(infoCellHeight*0.02f, 0)) );
            infoGrid.setPaddding( new Padding(0, 0, 0, 0.02f) );
            infoGrid.addShape(glyphShapeBox.setBorder(0.005f).setBorderColor(0, 0, 0, 1));
            infoGrid.addShape(glyphInfoBox.setBorder(0.005f).setBorderColor(0, 0, 0, 1));
            if( VERBOSE_UI ) {
                infoGrid.validate(reqGLP);
                System.err.println("InfoGrid "+infoGrid);
                System.err.println("InfoGrid "+infoGrid.getLayout());
            }

            final Group glyphInfoGrid = new Group(new GridLayout(2, 0f, 0f, Alignment.None));
            glyphInfoGrid.addShape(glyphGrid);
            glyphInfoGrid.addShape(infoGrid);
            if( VERBOSE_UI ) {
                glyphInfoGrid.validate(reqGLP);
                System.err.println("GlyphInfoGrid "+glyphInfoGrid);
                System.err.println("GlyphInfoGrid "+glyphInfoGrid.getLayout());
            }

            mainGrid = new Group(new GridLayout(1, 0f, 0f, Alignment.None));
            mainGrid.addShape(glyphInfoGrid);
            final Label infoText = new Label(options.renderModes, fontInfo, "Key-Up/Down or Mouse-Scroll to move through glyph symbols. Page-Up/Down or Control + Mouse-Scroll to page through glyph symbols fast.");
            infoText.setColor(0.1f, 0.1f, 0.1f, 1f);
            {
                final float h = glyphGrid.getBounds().getHeight() / gridDim.rawSize.y() * 0.6f;
                final Group labelBox = new Group(new BoxLayout(1.0f, h*0.9f, new Alignment(Alignment.Bit.Fill.value | Alignment.Bit.CenterVert.value),
                                                               new Margin(h*0.1f, 0.005f)));
                labelBox.addShape(infoText);
                mainGrid.addShape(labelBox);
            }
            if( VERBOSE_UI ) {
                mainGrid.validate(reqGLP);
                System.err.println("MainGrid "+mainGrid);
                System.err.println("MainGrid "+mainGrid.getLayout());
            }
        }
        scene.addShape(mainGrid);

        window.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                if( VERBOSE_UI ) {
                    System.err.println("Scroll.0: "+gridDim);
                }
                if( e.getRotation()[1] < 0f ) {
                    // scroll down
                    if( e.isControlDown() ) {
                        gridDim.pageDown();
                    } else {
                        gridDim.lineDown();
                    }
                } else {
                    // scroll up
                    if( e.isControlDown() ) {
                        gridDim.pageUp();
                    } else {
                        gridDim.lineUp();
                    }
                }
                window.invoke(false, new GLRunnable() {
                    @Override
                    public boolean run(final GLAutoDrawable drawable) {
                        glyphGrid.removeAllShapes(drawable.getGL().getGL2ES2(), scene.getRenderer());
                        addGlyphs(reqGLP, font, glyphGrid, gridDim, showUnderline[0], fontStatus, glyphMouseListener);
                        if( VERBOSE_UI ) {
                            System.err.println("Scroll.X: "+gridDim);
                        }
                        return true;
                    }
                });
            }
        });
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                final short keySym = e.getKeySymbol();
                boolean gridScroll = false;
                if( keySym == KeyEvent.VK_DOWN ) {
                    if( VERBOSE_UI ) {
                        System.err.println("Scroll.0: "+gridDim);
                    }
                    gridScroll = true;
                    gridDim.lineDown();
                } else if( keySym == KeyEvent.VK_PAGE_DOWN ) {
                    if( VERBOSE_UI ) {
                        System.err.println("Scroll.0: "+gridDim);
                    }
                    gridScroll = true;
                    gridDim.pageDown();
                } else if( keySym == KeyEvent.VK_UP ) {
                    if( VERBOSE_UI ) {
                        System.err.println("Scroll.0: "+gridDim);
                    }
                    gridScroll = true;
                    gridDim.lineUp();
                } else if( keySym == KeyEvent.VK_PAGE_UP ) {
                    if( VERBOSE_UI ) {
                        System.err.println("Scroll.0: "+gridDim);
                    }
                    gridScroll = true;
                    gridDim.pageUp();
                } else if( keySym == KeyEvent.VK_F4 || keySym == KeyEvent.VK_ESCAPE || keySym == KeyEvent.VK_Q ) {
                    MiscUtils.destroyWindow(window);
                }
                if( gridScroll ) {
                    window.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(final GLAutoDrawable drawable) {
                            glyphGrid.removeAllShapes(drawable.getGL().getGL2ES2(), scene.getRenderer());
                            addGlyphs(reqGLP, font, glyphGrid, gridDim, showUnderline[0], fontStatus, glyphMouseListener);
                            if( VERBOSE_UI ) {
                                System.err.println("Scroll.X: "+gridDim);
                            }
                            return true;
                        }
                    });
                }
            }
        });

        animator.start();
        scene.waitUntilDisplayed();
        {
            final AABBox sceneBox = scene.getBounds();
            final AABBox mainGridBox = mainGrid.getBounds();
            final float sx = sceneBox.getWidth() / mainGridBox.getWidth();
            final float sy = sceneBox.getHeight() / mainGridBox.getHeight();
            final float sxy = Math.min(sx, sy);
            System.err.println("SceneBox "+sceneBox);
            System.err.println("MainGridBox "+mainGridBox);
            System.err.println("scale sx "+sx+", sy "+sy+", sxy "+sxy);
            mainGrid.scale(sxy, sxy, 1f).moveTo(sceneBox.getLow());
        }
        printScreenOnGLThread(scene, window.getChosenGLCapabilities(), font, 0);
        // stay open ..
    }

    static void printScreenOnGLThread(final Scene scene, final GLCapabilitiesImmutable caps, final Font font, final int glyphID) {
        final String fn = font.getFullFamilyName().replace(' ', '_').replace('-', '_');
        scene.screenshot(true, scene.nextScreenshotFile(null, FontView01.class.getSimpleName(), options.renderModes, caps, fn+"_gid"+glyphID));
    }

    static class GridDim {
        final Vec2i rawSize;
        final List<Character> contourChars;
        final int columns;
        final int rows;
        final int elemCount;
        int start;
        int nextLine;
        int nextPage;

        public GridDim(final Font font, final Vec2i gridSize, final int xReserve, final int yReserve) {
            this.rawSize = gridSize;
            contourChars = new ArrayList<Character>();
            columns = gridSize.x() - xReserve;
            rows = gridSize.y() - yReserve;
            elemCount = columns * rows;
            start = 0; nextLine = -1; nextPage = -1;
            scanContourGlyphs(font);
        }

        public int scanContourGlyphs(final Font font) {
            contourChars.clear();
            for(int i=0; i <= Character.MAX_VALUE; ++i) {
                final int glyphID = font.getGlyphID((char)i);
                final Font.Glyph fg = font.getGlyph(glyphID);
                if( !fg.isNonContour() ) {
                    contourChars.add((char)i);
                }
            }
            return contourChars.size();
        }

        void pageDown() {
            if( nextPage < contourChars.size() - elemCount ) {
                start = nextPage;
            } else {
                start = 0;
            }
        }
        void pageUp() {
            if( start >= elemCount ) {
                start -= elemCount;
            } else {
                start = contourChars.size() - 1 - elemCount;
            }
        }
        void lineDown() {
            if( nextLine < contourChars.size() - columns ) {
                start = nextLine;
            } else {
                start = 0;
            }
        }
        void lineUp() {
            if( start >= columns ) {
                start -= columns;
            } else {
                start = contourChars.size() - 1 - columns;
            }
        }

        @Override
        public String toString() { return "GridDim[contours "+contourChars.size()+", start "+start+", nextLine "+nextLine+", nextPage "+nextPage+", "+columns+"x"+rows+"="+elemCount+", raw "+rawSize+"]"; }
    }

    static void addGlyphs(final GLProfile glp, final Font font, final Group grid,
                          final GridDim gridDim, final boolean showUnderline,
                          final Font fontStatus, final Shape.MouseGestureListener glyphMouseListener) {
        int glyphID = -1; // startGlyphID;
        gridDim.nextLine = Math.min(gridDim.start + gridDim.columns,   gridDim.contourChars.size()-1);
        gridDim.nextPage = Math.min(gridDim.start + gridDim.elemCount, gridDim.contourChars.size()-1);
        for(int idx = gridDim.start; idx < gridDim.nextPage; ++idx) {
            final char charID = gridDim.contourChars.get(idx);
            glyphID = font.getGlyphID( charID );
            final Font.Glyph fg = font.getGlyph(glyphID);

            final GlyphShape g = new GlyphShape(options.renderModes, charID, fg, 0, 0);
            g.setColor(0.1f, 0.1f, 0.1f, 1);
            g.setDragAndResizeable(false);

            // grid.addShape( g ); // GridLayout handles bottom-left offset and scale
            // Group each GlyphShape with its bounding box Rectangle
            final AABBox gbox = g.getBounds(glp);
            final boolean addUnderline = showUnderline && gbox.getMinY() < 0f;
            final Group c = new Group( new BoxLayout( 1f, 1f, addUnderline ? Alignment.None : Alignment.Center) );
            c.setBorder(0.02f).setBorderColor(0, 0, 0, 1).setInteractive(true).setDragAndResizeable(false);
            if( addUnderline ) {
                final Shape underline = new Rectangle(options.renderModes, 1f, gbox.getMinY(), 0.01f).setInteractive(false).setColor(0f, 0f, 1f, 0.25f);
                c.addShape(underline);
            }
            c.addShape(g);
            c.addMouseListener(glyphMouseListener);
            if( 0 == ( idx - gridDim.start ) % gridDim.columns ) {
                addLabel(grid, fontStatus, String.format("%04x", (int)charID));
            }
            grid.addShape(c);
        }
    }
    static void addLabel(final Group c, final Font font, final String text) {
        c.addShape( new Label(options.renderModes, font, 1f, text).setColor(0, 0, 0, 1).setInteractive(false).setDragAndResizeable(false) );
    }

    static void setGlyphInfo(final Font font, final Label label, final char symbol, final Font.Glyph g) {
        label.setText( getGlyphInfo(symbol, g) );
        if( VERBOSE_GLYPHS ) {
            System.err.println( label.getText() );
        }
    }

    static String getGlyphInfo(final char symbol, final Font.Glyph g) {
        final OutlineShape os = g.getShape();
        final int osVertices = null != os ? os.getVertexCount() : 0;
        final String name_s = null != g.getName() ? g.getName() : "";
        final AABBox bounds = g.getBounds();
        final String box_s = String.format("Box %+.3f/%+.3f%n    %+.3f/%+.3f", bounds.getLow().x(), bounds.getLow().y(), bounds.getHigh().x(), bounds.getHigh().y());
        return String.format((Locale)null, "%s%nHeight: %1.3f%nLine Height: %1.3f%n%nSymbol: %04x, id %04x%nName: '%s'%nDim %1.3f x %1.3f%n%s%nAdvance %1.3f%nLS Bearings: %1.3f%nVertices: %03d",
                g.getFont().getFullFamilyName(),
                g.getFont().getMetrics().getAscent() - g.getFont().getMetrics().getDescent(), // font hhea table
                g.getFont().getLineHeight(), // font hhea table
                (int)symbol, g.getID(), name_s,
                bounds.getWidth(), bounds.getHeight(), box_s,
                g.getAdvance(),
                g.getLeftSideBearings(),
                osVertices);
    }
}
