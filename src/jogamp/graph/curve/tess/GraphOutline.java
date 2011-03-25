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
package jogamp.graph.curve.tess;

import java.util.ArrayList;

import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Vertex;

public class GraphOutline <T extends Vertex> {
	final private Outline<T> outline;
	final private ArrayList<GraphVertex<T>> controlpoints = new ArrayList<GraphVertex<T>>(3);
	
	public GraphOutline(){
		this.outline = new Outline<T>();
	}
	
	/**Create a control polyline of control vertices
	 * the curve pieces can be identified by onCurve flag 
	 * of each cp the control polyline is open by default
	 */
	public GraphOutline(Outline<T> ol){
		this.outline = ol;
		ArrayList<T> vertices = this.outline.getVertices();
		for(T v:vertices){
			this.controlpoints.add(new GraphVertex<T>(v));
		}
	}

	public Outline<T> getOutline() {
		return outline;
	}

	/*public void setOutline(Outline<T> outline) {
		this.outline = outline;
	}*/
	

	public ArrayList<GraphVertex<T>> getGraphPoint() {
		return controlpoints;
	}
	
	public ArrayList<T> getPoints() {
		return outline.getVertices();
	}

	/*public void setControlpoints(ArrayList<GraphPoint<T>> controlpoints) {
		this.controlpoints = controlpoints;
	}*/

	public void addVertex(GraphVertex<T> v) {
		controlpoints.add(v);
		outline.addVertex(v.getPoint());
	}
	
}
