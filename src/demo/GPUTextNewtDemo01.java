/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package demo;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.text.HwTextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

public class GPUTextNewtDemo01 {
	private static void create(){
		new TextNewtWindow1();
	}
	public static void main(String[] args) {
		create();
	}
}

class TextNewtWindow1 {
	Vertex.Factory<SVertex> pointFactory = SVertex.factory();
	
	TextGLListener textGLListener = null; 

	public TextNewtWindow1(){
		createWindow();
	}
	private void createWindow() {
		GLProfile.initSingleton(true);
		GLProfile glp = GLProfile.getGL2ES2();
		GLCapabilities caps = new GLCapabilities(glp);
		caps.setAlphaBits(4);
	    caps.setSampleBuffers(true); // FIXME: sure ?
		caps.setNumSamples(4); // FIXME: sure ?
		System.out.println("Requested: "+caps);
		
		final GLWindow window = GLWindow.create(caps);
		
		window.setPosition(10, 10);
		window.setSize(500, 500);

		window.setTitle("GPU Text Newt Demo 01");
		textGLListener = new TextGLListener();
		window.addGLEventListener(textGLListener);

		window.setVisible(true);

		window.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_1){
					textGLListener.zoomIn();
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_2){
					textGLListener.zoomOut();
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_UP){
					textGLListener.move(0, -1);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_DOWN){
					textGLListener.move(0, 1);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_LEFT){
					textGLListener.move(1, 0);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_RIGHT){
					textGLListener.move(-1, 0);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_0){
					textGLListener.rotate(1);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_9){
					textGLListener.rotate(-1);
				}
			}
			public void keyTyped(KeyEvent arg0) {}
			public void keyReleased(KeyEvent arg0) {}
		});

		FPSAnimator animator = new FPSAnimator(60);
		animator.add(window);
		window.addWindowListener(new WindowAdapter() {
			public void windowDestroyNotify(WindowEvent arg0) {
				System.exit(0);
			};
		});
		animator.start();
	}
	private class TextGLListener implements GLEventListener{
		HwTextRenderer textRenderer = null;

		public TextGLListener(){

		}

		public void init(GLAutoDrawable drawable) {
			GL2ES2 gl = drawable.getGL().getGL2ES2();
			gl.setSwapInterval(1);
			gl.glEnable(GL2ES2.GL_DEPTH_TEST);
			textRenderer = new HwTextRenderer(drawable.getContext(), pointFactory, Region.SINGLE_PASS);
			
			System.out.println("Realised: "+drawable.getChosenGLCapabilities());			
			System.out.println("MS: " + gl.glIsEnabled(GL2ES2.GL_MULTISAMPLE));
		}

		float ang = 0;
		float zoom = -70;
		float xTran = -10;
		float yTran = 10;

		public void display(GLAutoDrawable drawable) {
			GL2ES2 gl = drawable.getGL().getGL2ES2();

			gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

			textRenderer.resetMatrix();
			textRenderer.translate(xTran, yTran, zoom);
			textRenderer.rotate(ang, 0, 1, 0);

			String text1 = "abcdef\nghijklmn\nopqrstuv\nwxyz\n0123456789";
			String text2 = text1.toUpperCase();

			Font font = textRenderer.createFont(pointFactory, "Lucida Sans Regular",40);		
			float[] position = new float[]{0,0,0};

			try {
				textRenderer.setAlpha(1.0f);
				textRenderer.setColor(0.0f, 0.0f, 1.0f);
				gl.glSampleCoverage(0.75f, false);
				textRenderer.renderString3D(font, text2, position, 0);
			} catch (Exception e) { 
				e.printStackTrace();
			}
		}
		public void reshape(GLAutoDrawable drawable, int xstart, int ystart, int width, int height){
			GL2ES2 gl = drawable.getGL().getGL2ES2();
			gl.glViewport(xstart, ystart, width, height);

			textRenderer.reshape(drawable, 45.0f, width, height, 0.1f, 7000.0f);
		}

		public void zoomIn(){
			zoom++;
		}
		public void zoomOut(){
			zoom--;
		}
		public void move(float x, float y){
			xTran += x;
			yTran += y;
		}
		public void rotate(float delta){
			ang+= delta;
			ang%=360;
		}
		public void dispose(GLAutoDrawable arg0) {
			textRenderer.clearCached();
		}
	}
}
