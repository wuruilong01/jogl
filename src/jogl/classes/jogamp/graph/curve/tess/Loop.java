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
package jogamp.graph.curve.tess;

import java.util.ArrayList;


import com.jogamp.graph.geom.Vertex;
import com.jogamp.math.Vec3f;
import com.jogamp.math.VectorUtil;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.Winding;
import com.jogamp.graph.geom.Triangle;

public class Loop {
    private HEdge root = null;
    private final AABBox box = new AABBox();
    private GraphOutline initialOutline = null;

    private Loop(final GraphOutline polyline, final int edgeType){
        initialOutline = polyline;
        this.root = initFromPolyline(initialOutline, edgeType);
    }

    public static Loop create(final GraphOutline polyline, final int edgeType) {
        final Loop res = new Loop(polyline, edgeType);
        if( null == res.root ) {
            return null;
        } else if( HEdge.HOLE == edgeType ) {
            res.addConstraintCurveImpl(polyline);
        }
        return res;
    }

    public HEdge getHEdge(){
        return root;
    }

    public Triangle cut(final boolean delaunay){
        if(isSimplex()){
            return new Triangle(root.getGraphPoint().getPoint(), root.getNext().getGraphPoint().getPoint(),
                                root.getNext().getNext().getGraphPoint().getPoint(), checkVerticesBoundary(root));
        }
        final HEdge prev = root.getPrev();
        final HEdge next1 = root.getNext();

        final HEdge next2;
        if( CDTriangulator2D.DEBUG ) {
            next2 = isValidNeighborDbg(next1.getNext(), delaunay);
        } else {
            next2 = isValidNeighbor(next1.getNext(), delaunay);
        }
        if(next2 == null){
            root = root.getNext();
            return null;
        }

        final GraphVertex v1 = root.getGraphPoint();
        final GraphVertex v2 = next1.getGraphPoint();
        final GraphVertex v3 = next2.getGraphPoint();

        final HEdge v3Edge = new HEdge(v3, HEdge.INNER);

        HEdge.connect(v3Edge, root);
        HEdge.connect(next1, v3Edge);

        HEdge v3EdgeSib = v3Edge.getSibling();
        if(v3EdgeSib == null){
            v3EdgeSib = new HEdge(v3Edge.getNext().getGraphPoint(), HEdge.INNER);
            HEdge.makeSiblings(v3Edge, v3EdgeSib);
        }

        HEdge.connect(prev, v3EdgeSib);
        HEdge.connect(v3EdgeSib, next2);

        final Triangle t = createTriangle(v1.getPoint(), v2.getPoint(), v3.getPoint(), root);
        this.root = next2;
        return t;
    }

    public boolean isSimplex(){
        return (root.getNext().getNext().getNext() == root);
    }

    /**
     * Create a connected list of half edges (loop)
     * from the boundary profile
     * @param edgeType either {@link HEdge#BOUNDARY} requiring {@link Winding#CCW} or {@link HEdge#HOLE} using {@link Winding#CW} or even {@link Winding#CCW}
     */
    private HEdge initFromPolyline(final GraphOutline outline, final int edgeType) {
        final ArrayList<GraphVertex> vertices = outline.getGraphPoint();

        if(vertices.size()<3) {
            System.err.println( "Graph: Loop.initFromPolyline: GraphOutline's vertices < 3: " + vertices.size() );
            if( GraphOutline.DEBUG ) {
                Thread.dumpStack();
            }
            return null;
        }
        final Winding winding = outline.getOutline().getWinding();
        final Winding edgeWinding = HEdge.BOUNDARY == edgeType ? Winding.CCW : Winding.CW;

        if( HEdge.BOUNDARY == edgeType && Winding.CCW != winding ) {
            // XXXX
            System.err.println("Loop.init.xx.01: BOUNDARY req CCW but has "+winding);
            // outline.getOutline().print(System.err);
            Thread.dumpStack();
        }
        HEdge firstEdge = null;
        HEdge lastEdge = null;

        if( winding == edgeWinding || HEdge.BOUNDARY == edgeType ) {
            // Correct Winding or skipped CW -> CCW (no inversion possible here, too late ??)
            final int max = vertices.size() - 1;
            for(int index = 0; index <= max; ++index) {
                final GraphVertex v1 = vertices.get(index);
                box.resize(v1.x(), v1.y(), v1.z());

                final HEdge edge = new HEdge(v1, edgeType);

                v1.addEdge(edge);
                if(lastEdge != null) {
                    lastEdge.setNext(edge);
                    edge.setPrev(lastEdge);
                } else {
                    firstEdge = edge;
                }
                if(index == max ) {
                    edge.setNext(firstEdge);
                    firstEdge.setPrev(edge);
                }
                lastEdge = edge;
            }
        } else { // if( hasWinding == Winding.CW ) {
            // CCW <-> CW
            for(int index = vertices.size() - 1; index >= 0; --index) {
                final GraphVertex v1 = vertices.get(index);
                box.resize(v1.x(), v1.y(), v1.z());

                final HEdge edge = new HEdge(v1, edgeType);

                v1.addEdge(edge);
                if(lastEdge != null) {
                    lastEdge.setNext(edge);
                    edge.setPrev(lastEdge);
                } else {
                    firstEdge = edge;
                }

                if (index == 0) {
                    edge.setNext(firstEdge);
                    firstEdge.setPrev(edge);
                }
                lastEdge = edge;
            }
        }
        return firstEdge;
    }

    public void addConstraintCurve(final GraphOutline polyline) {
        //        GraphOutline outline = new GraphOutline(polyline);
        /**needed to generate vertex references.*/
        if( null == initFromPolyline(polyline, HEdge.HOLE) ) {
            return;
        }
        addConstraintCurveImpl(polyline);
    }
    private void addConstraintCurveImpl(final GraphOutline polyline) {
        final GraphVertex v3 = locateClosestVertex(polyline);
        if( null == v3 ) {
            System.err.println( "Graph: Loop.locateClosestVertex returns null; root valid? "+(null!=root));
            if( GraphOutline.DEBUG ) {
                Thread.dumpStack();
            }
            return;
        }
        final HEdge v3Edge = v3.findBoundEdge();
        final HEdge v3EdgeP = v3Edge.getPrev();
        final HEdge crossEdge = new HEdge(root.getGraphPoint(), HEdge.INNER);

        HEdge.connect(root.getPrev(), crossEdge);
        HEdge.connect(crossEdge, v3Edge);

        HEdge crossEdgeSib = crossEdge.getSibling();
        if(crossEdgeSib == null) {
            crossEdgeSib = new HEdge(crossEdge.getNext().getGraphPoint(), HEdge.INNER);
            HEdge.makeSiblings(crossEdge, crossEdgeSib);
        }

        HEdge.connect(v3EdgeP, crossEdgeSib);
        HEdge.connect(crossEdgeSib, root);
    }

    /**
     * Locates the vertex and update the loops root
     * to have (root + vertex) as closest pair
     * @param polyline the control polyline to search for closestvertices in CW
     * @return the vertex that is closest to the newly set root Hedge.
     */
    private GraphVertex locateClosestVertex(final GraphOutline polyline) {
        HEdge closestE = null;
        GraphVertex closestV = null;

        // final Winding winding = polyline.getOutline().getWinding();
        // final Winding winding = getWinding( vertices ); // requires area-winding detection

        float minDistance = Float.MAX_VALUE;
        final ArrayList<GraphVertex> initVertices = initialOutline.getGraphPoint();
        final ArrayList<GraphVertex> vertices = polyline.getGraphPoint();

        for(int i=0; i< initVertices.size()-1; i++){
            final GraphVertex v = initVertices.get(i);
            final GraphVertex nextV = initVertices.get(i+1);
            for(int pos=0; pos<vertices.size(); pos++) {
                final GraphVertex cand = vertices.get(pos);
                final float distance = v.getCoord().dist( cand.getCoord() );
                if(distance < minDistance){
                    boolean inside = false;
                    for (final GraphVertex vert:vertices){
                        if(vert == v || vert == nextV || vert == cand) {
                            continue;
                        }
                        inside = VectorUtil.isInCircleVec2d(v.getPoint(), nextV.getPoint(), cand.getPoint(), vert.getPoint());
                        if(inside){
                            break;
                        }
                    }
                    if(!inside){
                        closestV = cand;
                        minDistance = distance;
                        closestE = v.findBoundEdge();
                    }
                }

            }
        }

        if(closestE != null){
            root = closestE;
        }

        return closestV;
    }

    private HEdge isValidNeighbor(final HEdge candEdge, final boolean delaunay) {
        final HEdge next = root.getNext();
        if( !VectorUtil.isCCW( root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(),
                               candEdge.getGraphPoint().getPoint()) ) {
            return null;
        }
        if( !delaunay ) {
            return candEdge;
        }
        final Vertex candPoint = candEdge.getGraphPoint().getPoint();
        HEdge e = candEdge.getNext();
        while (e != candEdge){
            final GraphVertex egp = e.getGraphPoint();
            if(egp != root.getGraphPoint() &&
               egp != next.getGraphPoint() &&
               egp.getPoint() != candPoint )
            {
                if( VectorUtil.isInCircleVec2d(root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(),
                                               candPoint, egp.getPoint()) ) {
                    return null;
                }
            }
            e = e.getNext();
        }
        return candEdge;
    }
    private HEdge isValidNeighborDbg(final HEdge candEdge, final boolean delaunay) {
        final HEdge next = root.getNext();
        if( !VectorUtil.isCCW( root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(),
                               candEdge.getGraphPoint().getPoint()) ) {
            return null;
        }
        if( !delaunay ) {
            return candEdge;
        }
        final Vertex candPoint = candEdge.getGraphPoint().getPoint();
        HEdge e = candEdge.getNext();
        while (e != candEdge){
            final GraphVertex egp = e.getGraphPoint();
            if(egp != root.getGraphPoint() &&
               egp != next.getGraphPoint() &&
               egp.getPoint() != candPoint )
            {
                final double v = VectorUtil.inCircleVec2dVal(root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(),
                                                             candPoint, egp.getPoint());
                if( v > VectorUtil.InCircleDThreshold ) {
                    if( CDTriangulator2D.DEBUG ) {
                        System.err.printf("Loop.isInCircle.1: %30.30f: %s, of%n- %s%n- %s%n- %s%n",
                                v, candPoint, root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(), egp.getPoint());
                    }
                    return null;
                }
                if( CDTriangulator2D.DEBUG ) {
                    System.err.printf("Loop.isInCircle.0: %30.30f: %s, of%n- %s%n- %s%n- %s%n",
                            v, candPoint, root.getGraphPoint().getPoint(), next.getGraphPoint().getPoint(), egp.getPoint());
                }
            }
            e = e.getNext();
        }
        System.err.printf("Loop.isInCircle.0: %s%n", candPoint);
        return candEdge;
    }

    /** Create a triangle from the param vertices only if
     * the triangle is valid. IE not outside region.
     * @param v1 vertex 1
     * @param v2 vertex 2
     * @param v3 vertex 3
     * @param root and edge of this triangle
     * @return the triangle iff it satisfies, null otherwise
     */
    private Triangle createTriangle(final Vertex v1, final Vertex v2, final Vertex v3, final HEdge rootT){
        return new Triangle(v1, v2, v3, checkVerticesBoundary(rootT));
    }

    private boolean[] checkVerticesBoundary(final HEdge rootT) {
        final boolean[] boundary = new boolean[3];
        if(rootT.getGraphPoint().isBoundaryContained()){
                boundary[0] = true;
        }
        if(rootT.getNext().getGraphPoint().isBoundaryContained()){
                boundary[1] = true;
        }
        if(rootT.getNext().getNext().getGraphPoint().isBoundaryContained()){
                boundary[2] = true;
        }
        return boundary;
    }

    public boolean checkInside(final Vertex v) {
        if(!box.contains(v.x(), v.y(), v.z())){
            return false;
        }

        boolean inside = false;
        HEdge current = root;
        HEdge next = root.getNext();
        do {
            final Vertex v2 = current.getGraphPoint().getPoint();
            final Vertex v1 = next.getGraphPoint().getPoint();

            if ( ((v1.y() > v.y()) != (v2.y() > v.y())) &&
                  (v.x() < (v2.x() - v1.x()) * (v.y() - v1.y()) / (v2.y() - v1.y()) + v1.x()) ){
                inside = !inside;
            }

            current = next;
            next = current.getNext();

        } while(current != root);

        return inside;
    }

    public int computeLoopSize(){
        int size = 0;
        HEdge e = root;
        do{
            size++;
            e = e.getNext();
        }while(e != root);
        return size;
    }
}
