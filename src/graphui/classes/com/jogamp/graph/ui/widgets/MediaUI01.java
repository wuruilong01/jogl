/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.widgets;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Clock;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.MediaButton;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventAdapter;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.EventMask;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * UI Media player factory, embedding a {@link MediaButton} and its controls within a {@link Group}.
 * @see MediaUI01#create(Scene, GLMediaPlayer, int, Uri, int, float, boolean, float, List)
 */
public class MediaUI01 {
    public static final int MediaTexUnitMediaPlayer = 1;

    /** Default texture count, value {@value}, same as {@link GLMediaPlayer#TEXTURE_COUNT_DEFAULT}. */
    public static final int MediaTexCount = GLMediaPlayer.TEXTURE_COUNT_DEFAULT;

    public static final Vec2f FixedSymSize = new Vec2f(0.0f, 1.0f);
    public static final Vec2f SymSpacing = new Vec2f(0f, 0.2f);
    public static final float CtrlButtonWidth = 1f;
    public static final float CtrlButtonHeight = 1f;
    public static final Vec4f CtrlCellCol = new Vec4f(0, 0, 0, 0);

    /** Returns the used info font or null if n/a */
    public static Font getInfoFont() {
        try {
            return FontFactory.get(FontFactory.UBUNTU).getDefault();
        } catch(final IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }
    /** Returns the used symbols font or null if n/a */
    public static Font getSymbolsFont() {
        try {
            return FontFactory.get(FontFactory.SYMBOLS).getDefault();
        } catch(final IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a {@link Group} containing a {@link MediaButton} and its controls.
     * @param scene the used {@link Scene} to query parameter and access rendering loop
     * @param mPlayer fresh {@link GLMediaPlayer} instance, maybe customized via e.g. {@link GLMediaPlayer#setTextureMinMagFilter(int[])}.
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param medium {@link Uri} stream source, either a file or network source
     * @param aid audio-id to start playing, may use {@link GLMediaPlayer#STREAM_ID_AUTO}
     * @param aratio aspect ratio of the resulting {@link Shape}, usually 16.0f/9.0f or 4.0f/3.0f, which also denotes the width of this shape while using height 1.0.
     * @param letterBox toggles {@link Region#COLORTEXTURE_LETTERBOX_RENDERING_BIT} on or off
     * @param zoomSize zoom-size (0..1] for zoom-out control
     * @param customCtrls optional custom controls, maybe an empty list
     */
    public static Shape create(final Scene scene, final GLMediaPlayer mPlayer, final int renderModes,
                               final Uri medium, final int aid,
                               final float aratio, final boolean letterBox, final float zoomSize,
                               final List<Shape> customCtrls)
    {
        final Font fontInfo = getInfoFont(), fontSymbols = getSymbolsFont();
        if( null == fontInfo || null == fontSymbols ) {
            return new Rectangle(renderModes, aratio, 1, 0.10f);
        }
        final float borderSz = 0.01f;
        final float borderSzS = 0.03f;
        final Vec4f borderColor = new Vec4f(0, 0, 0, 0.5f);
        final Vec4f borderColorA = new Vec4f(0, 0, 0.5f, 0.5f);
        final float alphaBlend = 0.3f;

        final float zEpsilon = scene.getZEpsilon(16);
        final float ctrlZOffset = zEpsilon * 20f;

        final int ctrlCellsInt = 9;
        final int ctrlCells = Math.max(customCtrls.size() + ctrlCellsInt, 13);

        final float ctrlCellWidth = (aratio-2*borderSzS)/ctrlCells;
        final float ctrlCellHeight = ctrlCellWidth;

        final Shape[] zoomReplacement = { null };
        final Vec3f[] zoomOrigScale = { null };
        final Vec3f[] zoomOrigPos = { null };

        final Group container = new Group(new BoxLayout(aratio, 1, Alignment.None));
        container.setName("container");
        container.setBorderColor(borderColor).setBorder(borderSz);
        container.setInteractive(true).setFixedARatioResize(true);

        final MediaButton mButton = new MediaButton(renderModes, aratio, 1, mPlayer);
        mButton.setName("mButton").setInteractive(false);
        mButton.setPerp().setPressedColorMod(1f, 1f, 1f, 0.85f);

        // mButton.setBorderColor(borderNormal).setBorder(borderSz);
        {
            mPlayer.setTextureUnit(MediaTexUnitMediaPlayer);
            mButton.setVerbose(false).addDefaultEventListener().setTextureLetterbox(letterBox);
            mPlayer.setAudioVolume( 0f );
            mPlayer.addEventListener( new GLMediaEventListener() {
                @Override
                public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

                @Override
                public void attributesChanged(final GLMediaPlayer mp, final EventMask eventMask, final long when) {
                    // System.err.println("MediaButton AttributesChanges: "+eventMask+", when "+when);
                    // System.err.println("MediaButton State: "+mp);
                    if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                        System.err.println(mp.toString());
                    }
                    if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.EOS) ) {
                        final StreamException err = mp.getStreamException();
                        if( null != err ) {
                            System.err.println("MovieSimple State: Exception: "+err.getMessage());
                        } else {
                            new InterruptSource.Thread() {
                                @Override
                                public void run() {
                                    mp.setPlaySpeed(1f);
                                    mp.seek(0);
                                    mp.resume();
                                }
                            }.start();
                        }
                    }
                }
            });
            mPlayer.playStream(medium, GLMediaPlayer.STREAM_ID_AUTO, aid, MediaTexCount);
            container.addShape(mButton);
        }

        Group ctrlGroup, infoGroup;
        Shape ctrlBlend;
        final Label muteLabel, infoLabel;
        final Button timeLabel;
        {
            muteLabel = new Label(renderModes, fontSymbols, aratio/6f, fontSymbols.getUTF16String("music_off")); // volume_mute, headset_off
            muteLabel.setName("muteLabel");
            {
                final float sz = aratio/6f;
                muteLabel.setColor(1, 0, 0, 1);
                muteLabel.setPaddding(new Padding(0, 0, 1f-sz, aratio-sz));

                muteLabel.setInteractive(false);
                muteLabel.setVisible( mPlayer.isAudioMuted() );
                container.addShape(muteLabel);
            }

            infoGroup = new Group(new BoxLayout());
            infoGroup.setName("infoGroup").setInteractive(false);
            container.addShape( infoGroup.setVisible(false) );
            {
                final float sz = 1/7f;
                final Rectangle rect = new Rectangle(renderModes, aratio, sz, sz/2f);
                rect.setName("info.blend").setInteractive(false);
                rect.setColor(0, 0, 0, alphaBlend);
                rect.setPaddding(new Padding(0, 0, 1f-sz, 0));
                infoGroup.addShape(rect);
            }
            {
                final int lines = 3;
                final String text = getInfo(mPlayer, false);
                infoLabel = new Label(renderModes, fontInfo, aratio/40f, text);
                infoLabel.setName("infoLabel");
                final float szw = aratio/40f;
                infoLabel.setPaddding(new Padding(0, 0, 1f-szw*lines, szw));
                infoLabel.setInteractive(false);
                infoLabel.setColor(1, 1, 1, 1);
                infoGroup.addShape(infoLabel);
            }
            {
                timeLabel = new Button(renderModes, fontInfo,
                        getMultilineTime(mPlayer), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                timeLabel.setName("timeLabel");
                timeLabel.setPerp().setColor(CtrlCellCol);
                timeLabel.setLabelColor(1, 1, 1);
            }
            scene.addGLEventListener(new GLEventAdapter() {
                long t0 = 0;
                @Override
                public void display(final GLAutoDrawable drawable) {
                    final GLAnimatorControl anim = drawable.getAnimator();
                    if( ( timeLabel.isVisible() || infoLabel.isVisible() ) &&
                        mPlayer.getState() != GLMediaPlayer.State.Uninitialized && null != anim )
                    {
                        final long t1 = anim.getTotalFPSDuration();
                        if( t1 - t0 >= 300) {
                            t0 = t1;
                            infoLabel.setText(getInfo(mPlayer, false));
                            timeLabel.setText(getMultilineTime(mPlayer));
                        }
                    }
                }
            } );


            ctrlBlend = new Rectangle(renderModes, aratio, ctrlCellHeight, ctrlCellHeight/2f);
            ctrlBlend.setName("ctrl.blend").setInteractive(false);
            ctrlBlend.setColor(0, 0, 0, alphaBlend);
            ctrlBlend.setPaddding(new Padding(0, 0, 0, 0));
            container.addShape( ctrlBlend.setVisible(false) );

            ctrlGroup = new Group(new GridLayout(ctrlCellWidth, ctrlCellHeight, Alignment.FillCenter, Gap.None, 1));
            ctrlGroup.setName("ctrlGroup").setInteractive(false);
            ctrlGroup.setPaddding(new Padding(0, borderSzS, 0, borderSzS));
            container.addShape( ctrlGroup.move(0, 0, ctrlZOffset).setVisible(false) );
            { // 1
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("play_arrow"),  fontSymbols.getUTF16String("pause"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("play");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onToggle((final Shape s) -> {
                    if( s.isToggleOn() ) {
                        mPlayer.resume();
                    } else {
                        mPlayer.pause(false);
                    }
                });
                button.setToggle(true); // on == play
                ctrlGroup.addShape(button);
            }
            { // 2
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("skip_previous"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("back");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.seek(0);
                });
                ctrlGroup.addShape(button);
            }
            { // 3
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("fast_rewind"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("frev");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.setPlaySpeed(mPlayer.getPlaySpeed() - 0.5f);
                });
                ctrlGroup.addShape(button);
            }
            { // 4
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("fast_forward"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("ffwd");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.setPlaySpeed(mPlayer.getPlaySpeed() + 0.5f);
                });
                ctrlGroup.addShape(button);
            }
            { // 5
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("replay_5"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("rew5");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.seek(mPlayer.getPTS().get(Clock.currentMillis()) - 5000);
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        final int pts0 = mPlayer.getPTS().get(Clock.currentMillis());
                        final int pts1 = pts0 + (int)(e.getRotation()[1]*1000f);
                        System.err.println("Seek: "+pts0+" -> "+pts1);
                        mPlayer.seek(pts1);
                    } } );
                ctrlGroup.addShape(button);
            }
            { // 6
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("forward_5"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("fwd5");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.seek(mPlayer.getPTS().get(Clock.currentMillis()) + 5000);
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        final int pts0 = mPlayer.getPTS().get(Clock.currentMillis());
                        final int pts1 = pts0 + (int)(e.getRotation()[1]*1000f);
                        System.err.println("Seek: "+pts0+" -> "+pts1);
                        mPlayer.seek(pts1);
                    } } );
                ctrlGroup.addShape(button);
            }
            { // 7
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("volume_up"), fontSymbols.getUTF16String("volume_mute"),  CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("mute");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                final float[] volume = { 1.0f };
                button.onToggle( (final Shape s) -> {
                    if( s.isToggleOn() ) {
                        mPlayer.setAudioVolume( volume[0] );
                    } else {
                        mPlayer.setAudioVolume( 0f );
                    }
                    muteLabel.setVisible( !s.isToggleOn() );
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        mPlayer.setAudioVolume( mPlayer.getAudioVolume() + e.getRotation()[1]/20f );
                        volume[0] = mPlayer.getAudioVolume();
                    } } );
                button.setToggle( !mPlayer.isAudioMuted() ); // on == volume
                ctrlGroup.addShape(button);
            }
            { // 8
                ctrlGroup.addShape(timeLabel);
            }
            for(int i=8; i<ctrlCells-1-customCtrls.size(); ++i) {
                final Button button = new Button(renderModes, fontInfo, " ", CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("ctrl_"+i);
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                ctrlGroup.addShape(button);
            }
            { // -1
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("zoom_out_map"), fontSymbols.getUTF16String("zoom_in_map"),  CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("zoom");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onToggle( (final Shape s) -> {
                    if( s.isToggleOn() ) {
                        final AABBox sbox = scene.getBounds();
                        final Group parent = container.getParent();
                        if( null != parent ) {
                            zoomReplacement[0] = new Label(renderModes, fontInfo, aratio/40f, "zoomed");
                            final boolean r = parent.replaceShape(container, zoomReplacement[0]);
                            if( r ) {
                                System.err.println("Zoom1: "+parent);
                                final float sxy = sbox.getWidth() * zoomSize / container.getScaledWidth();
                                scene.addShape(container);
                                System.err.println("cont1 "+container);
                                System.err.println("cbox1 "+container.getBounds());
                                System.err.println("sbox1 "+sbox);
                                System.err.println("sxy1 "+sxy);
                                container.scale(sxy, sxy, 1f);
                                container.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, ctrlZOffset);
                            } else {
                                System.err.println("Zoom1: failed");
                            }
                        } else {
                            zoomOrigScale[0] = container.getScale().copy();
                            zoomOrigPos[0] = container.getPosition().copy();
                            System.err.println("Zoom2: top");
                            final float sxy = sbox.getWidth() * zoomSize / container.getScaledWidth();
                            System.err.println("cont2 "+container);
                            System.err.println("cbox2 "+container.getBounds());
                            System.err.println("sbox2 "+sbox);
                            System.err.println("sxy2 "+sxy);
                            container.scale(sxy, sxy, 1f);
                            container.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, ctrlZOffset);
                        }
                    } else {
                        if( null != zoomReplacement[0] ) {
                            final Group parent = zoomReplacement[0].getParent();
                            container.moveTo(0, 0, 0);
                            parent.replaceShape(zoomReplacement[0], container);
                            scene.invoke(true, (drawable) -> {
                                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                                zoomReplacement[0].destroy(gl, scene.getRenderer());
                                return true;
                            });
                            zoomReplacement[0] = null;
                            System.err.println("Reset1: "+parent);
                        } else if( null != zoomOrigScale[0] && null != zoomOrigPos[0] ){
                            container.scale(zoomOrigScale[0]);
                            container.moveTo(zoomOrigPos[0]);
                            zoomOrigScale[0] = null;
                            zoomOrigPos[0] = null;
                            System.err.println("Reset2: top");
                        }
                    }
                });
                button.setToggle( false ); // on == zoom
                ctrlGroup.addShape(button);
            }
            for(final Shape cs : customCtrls ) {
                ctrlGroup.addShape(cs);
            }
        }
        container.setWidgetMode(true);

        container.onActivation( (final Shape s) -> {
            if( container.isActive() ) {
                container.setBorderColor(borderColorA);
            } else {
                container.setBorderColor(borderColor);
            }
            if( ctrlGroup.isActive() ) {
                ctrlBlend.setVisible(true);
                ctrlGroup.setVisible(true);
                infoGroup.setVisible(true);
            } else {
                ctrlBlend.setVisible(false);
                ctrlGroup.setVisible(false);
                infoGroup.setVisible(false);
            }
        });
        container.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                mButton.setPressedColorMod(1f, 1f, 1f, 1f);
            }
            @Override
            public void mouseDragged(final MouseEvent e) {
                mButton.setPressedColorMod(1f, 1f, 1f, 0.85f);
            }
        } );
        container.forAll((final Shape s) -> { s.setDraggable(false).setResizable(false); return false; });
        return container;
    }

    public static String millisToTimeStr(final long millis, final boolean addFractions) {
        final long h = TimeUnit.MILLISECONDS.toHours(millis);
        final long m = TimeUnit.MILLISECONDS.toMinutes(millis);
        if( addFractions ) {
            if( 0 < h ) {
                return String.format("%02d:%02d:%02d.%02d",
                    h,
                    m - TimeUnit.HOURS.toMinutes(h),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m),
                    millis%1000);
            } else {
                return String.format("%02d:%02d.%02d",
                    m,
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m),
                    millis%1000);
            }
        } else {
            if( 0 < h ) {
                return String.format("%02d:%02d:%02d",
                    h,
                    m - TimeUnit.HOURS.toMinutes(h),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m));
            } else {
                return String.format("%02d:%02d",
                    m,
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m));
            }
        }
    }
    public static String getInfo(final GLMediaPlayer mPlayer, final boolean full) {
        final String name;
        {
            final String basename;
            final String s = mPlayer.getUri().path.decode();
            final int li = s.lastIndexOf('/');
            if( 0 < li ) {
                basename = s.substring(li+1);
            } else {
                basename = s;
            }
            final int di = basename.lastIndexOf('.');
            if( 0 < di ) {
                name = basename.substring(0, di);
            } else {
                name = basename;
            }
        }
        final float aspect = (float)mPlayer.getWidth() / (float)mPlayer.getHeight();
        final int pts = mPlayer.getPTS().get(Clock.currentMillis());
        final long duration = mPlayer.getDuration();
        final float pct = (float)pts / (float)duration;
        if( full ) {
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.1fx, vol %1.2f), A/R %0.2f, fps %02.1f",
                    millisToTimeStr(pts, false), millisToTimeStr(duration, false), pct*100,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(), aspect, mPlayer.getFramerate());
            final String text2 = String.format("audio: id %d, kbps %d, codec %s",
                    mPlayer.getAID(), mPlayer.getAudioBitrate()/1000, mPlayer.getAudioCodec());
            final String text3 = String.format("video: id %d, kbps %d, codec %s",
                    mPlayer.getVID(), mPlayer.getVideoBitrate()/1000, mPlayer.getVideoCodec());
            return text1+"\n"+text2+"\n"+text3+"\n"+name;
        } else {
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.1fx, vol %1.2f), A/R %.2f",
                    millisToTimeStr(pts, false), millisToTimeStr(duration, false), pct*100,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(), aspect);
            return text1+"\n"+name;
        }
    }
    public static String getMultilineTime(final GLMediaPlayer mPlayer) {
        final int pts = mPlayer.getPTS().get(Clock.currentMillis());
        final long duration = mPlayer.getDuration();
        final float pct = (float)pts / (float)duration;
        return String.format("%.0f %%%n%s%n%s",
                    pct*100, millisToTimeStr(pts, false), millisToTimeStr(duration, false));
    }

}
