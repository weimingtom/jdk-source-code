/*
 * @(#)DuctusRenderer.java	1.20 04/03/02
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.java2d.pipe;

import java.awt.geom.AffineTransform;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import sun.dc.path.PathConsumer;
import sun.dc.path.PathException;
import sun.dc.pr.Rasterizer;
import sun.dc.pr.PathStroker;
import sun.dc.pr.PathDasher;
import sun.dc.pr.PRException;

/**
 * This is the superclass of any class that needs to use the Ductus
 * Rasterizer.
 * This class provides utility routines to create, cache, initialize
 * and release Rasterizer objects.
 */
public class DuctusRenderer {
    public static final float PenUnits = 0.01f;
    public static final int MinPenUnits = 100;
    public static final int MinPenUnitsAA = 20;
    public static final float MinPenSizeAA = PenUnits * MinPenUnitsAA;

    static final int RasterizerCaps[] = {
	Rasterizer.BUTT, Rasterizer.ROUND, Rasterizer.SQUARE
    };

    static final int RasterizerCorners[] = {
	Rasterizer.MITER, Rasterizer.ROUND, Rasterizer.BEVEL
    };

    private static Rasterizer theRasterizer;

    public synchronized static Rasterizer getRasterizer() {
	Rasterizer r = theRasterizer;
	if (r == null) {
	    r = new Rasterizer();
	} else {
	    theRasterizer = null;
	}
	return r;
    }

    public synchronized static void dropRasterizer(Rasterizer r) {
	r.reset();
	theRasterizer = r;
    }

    private static byte[] theTile;

    public synchronized static byte[] getAlphaTile() {	
	byte[] t = theTile;
	if (t == null) {
	    int dim = Rasterizer.TILE_SIZE;
	    t = new byte[dim * dim];
	} else {
	    theTile = null;
	}
	return t;
    }

    public synchronized static void dropAlphaTile(byte[] t) {
	theTile = t;
    }

    /*
     * writeAlpha is not threadsafe (it uses a global table without
     * locking), so we must use this static synchronized accessor
     * method to serialize accesses to it.
     */
    public synchronized static void getAlpha(Rasterizer r, byte[] alpha,
					     int xstride, int ystride,
					     int offset)
	throws PRException
    {
	try {
	    r.writeAlpha(alpha, xstride, ystride, offset);
	} catch (InterruptedException e) {
	    Thread.currentThread().interrupt();
	}
    }

    /*
     * Returns a new PathConsumer that will take a path trajectory and
     * feed the stroked outline for that trajectory to the supplied
     * PathConsumer.
     */
    public static PathConsumer createStroker(PathConsumer consumer,
					     BasicStroke bs, boolean thin,
					     AffineTransform transform)
    {
	PathStroker stroker = new PathStroker(consumer);
	consumer = stroker;

	float matrix[] = null;
	if (!thin) {
	    stroker.setPenDiameter(bs.getLineWidth());
	    if (transform != null) {
		matrix = getTransformMatrix(transform);
	    }
	    stroker.setPenT4(matrix);
	    stroker.setPenFitting(PenUnits, MinPenUnits);
	}
	stroker.setCaps(RasterizerCaps[bs.getEndCap()]);
	stroker.setCorners(RasterizerCorners[bs.getLineJoin()],
			   bs.getMiterLimit());
	float[] dashes = bs.getDashArray();
	if (dashes != null) {
	    PathDasher dasher = new PathDasher(stroker);
	    dasher.setDash(dashes, bs.getDashPhase());
	    if (transform != null && matrix == null) {
		matrix = getTransformMatrix(transform);
	    }
	    dasher.setDashT4(matrix);
	    consumer = dasher;
	}

	return consumer;
    }

    static float[] getTransformMatrix(AffineTransform transform) {
	float matrix[] = new float[4];
	double dmatrix[] = new double[6];
	transform.getMatrix(dmatrix);
	for (int i = 0; i < 4; i++) {
	    matrix[i] = (float) dmatrix[i];
	}
	return matrix;
    }

    /*
     * Disposes a PathConsumer chain created by createStroker.
     * This method will dispose all PathConsumers chained
     * together starting from the stroker argument until it
     * reaches the terminus object, or null.
     */
    public static void disposeStroker(PathConsumer stroker,
				      PathConsumer terminus)
    {
	while (stroker != null && stroker != terminus) {
	    PathConsumer next = stroker.getConsumer();
	    stroker.dispose();
	    stroker = next;
	}
    }

    /*
     * Feed a path from a PathIterator to a Ductus PathConsumer.
     */
    public static void feedConsumer(PathIterator pi, PathConsumer consumer,
				    boolean normalize, float norm)
	throws PathException
    {
	consumer.beginPath();
	boolean pathClosed = false;
	float mx = 0.0f;
	float my = 0.0f;
	float point[]  = new float[6];
	float rnd = (0.5f - norm);
	float ax = 0.0f;
	float ay = 0.0f;

	while (!pi.isDone()) {
	    int type = pi.currentSegment(point);
	    if (pathClosed == true) {
		pathClosed = false;
		if (type != PathIterator.SEG_MOVETO) {
		    // Force current point back to last moveto point
		    consumer.beginSubpath(mx, my);
		}
	    }
	    if (normalize) {
		int index;
		switch (type) {
		case PathIterator.SEG_CUBICTO:
		    index = 4;
		    break;
		case PathIterator.SEG_QUADTO:
		    index = 2;
		    break;
		case PathIterator.SEG_MOVETO:
		case PathIterator.SEG_LINETO:
		    index = 0;
		    break;
		case PathIterator.SEG_CLOSE:
		default:
		    index = -1;
		    break;
		}
		if (index >= 0) {
		    float ox = point[index];
		    float oy = point[index+1];
		    float newax = (float) Math.floor(ox + rnd) + norm;
		    float neway = (float) Math.floor(oy + rnd) + norm;
		    point[index] = newax;
		    point[index+1] = neway;
		    newax -= ox;
		    neway -= oy;
		    switch (type) {
		    case PathIterator.SEG_CUBICTO:
			point[0] += ax;
			point[1] += ay;
			point[2] += newax;
			point[3] += neway;
			break;
		    case PathIterator.SEG_QUADTO:
			point[0] += (newax + ax) / 2;
			point[1] += (neway + ay) / 2;
			break;
		    case PathIterator.SEG_MOVETO:
		    case PathIterator.SEG_LINETO:
		    case PathIterator.SEG_CLOSE:
			break;
		    }
		    ax = newax;
		    ay = neway;
		}
	    }
	    switch (type) {
	    case PathIterator.SEG_MOVETO:
		mx = point[0];
		my = point[1];
		consumer.beginSubpath(point[0], point[1]);
		break;
	    case PathIterator.SEG_LINETO:
		consumer.appendLine(point[0], point[1]);
		break;
	    case PathIterator.SEG_QUADTO:
		// Quadratic curves take two points
		consumer.appendQuadratic(point[0], point[1],
					 point[2], point[3]);
		break;
	    case PathIterator.SEG_CUBICTO:
		// Cubic curves take three points
		consumer.appendCubic(point[0], point[1],
				     point[2], point[3],
				     point[4], point[5]);
		break;
	    case PathIterator.SEG_CLOSE:
		consumer.closedSubpath();
		pathClosed = true;
		break;
	    }
	    pi.next();
	}

	consumer.endPath();
    }

    /*
     * Returns a new Rasterizer that is ready to rasterize the given Shape.
     */
    public static Rasterizer createShapeRasterizer(PathIterator pi,
						   AffineTransform transform,
						   BasicStroke stroke,
						   boolean thin,
						   boolean normalize,
						   float norm) {
	Rasterizer r = getRasterizer();

        if (stroke != null) {
	    float matrix[] = null;
            r.setUsage(Rasterizer.STROKE);
	    if (thin) {
		r.setPenDiameter(MinPenSizeAA);
	    } else {
		r.setPenDiameter(stroke.getLineWidth());
		if (transform != null) {
		    matrix = getTransformMatrix(transform);
		    r.setPenT4(matrix);
		}
		r.setPenFitting(PenUnits, MinPenUnitsAA);
	    }
	    r.setCaps(RasterizerCaps[stroke.getEndCap()]);
	    r.setCorners(RasterizerCorners[stroke.getLineJoin()],
			 stroke.getMiterLimit());
	    float[] dashes = stroke.getDashArray();
	    if (dashes != null) {
		r.setDash(dashes, stroke.getDashPhase());
		if (transform != null && matrix == null) {
		    matrix = getTransformMatrix(transform);
		}
		r.setDashT4(matrix);
	    }
        } else {
            r.setUsage(pi.getWindingRule() == PathIterator.WIND_EVEN_ODD
                       ? Rasterizer.EOFILL
                       : Rasterizer.NZFILL);
        }

	r.beginPath();
	{
            boolean pathClosed = false;
            float mx = 0.0f;
            float my = 0.0f;
            float point[]  = new float[6];
	    float rnd = (0.5f - norm);
	    float ax = 0.0f;
	    float ay = 0.0f;

	    while (!pi.isDone()) {
		int type = pi.currentSegment(point);
                if (pathClosed == true) {
                    pathClosed = false;
                    if (type != PathIterator.SEG_MOVETO) {
                        // Force current point back to last moveto point
                        r.beginSubpath(mx, my);
                    }
                }
		if (normalize) {
		    int index;
		    switch (type) {
		    case PathIterator.SEG_CUBICTO:
			index = 4;
			break;
		    case PathIterator.SEG_QUADTO:
			index = 2;
			break;
		    case PathIterator.SEG_MOVETO:
		    case PathIterator.SEG_LINETO:
			index = 0;
			break;
		    case PathIterator.SEG_CLOSE:
		    default:
			index = -1;
			break;
		    }
		    if (index >= 0) {
			float ox = point[index];
			float oy = point[index+1];
			float newax = (float) Math.floor(ox + rnd) + norm;
			float neway = (float) Math.floor(oy + rnd) + norm;
			point[index] = newax;
			point[index+1] = neway;
			newax -= ox;
			neway -= oy;
			switch (type) {
			case PathIterator.SEG_CUBICTO:
			    point[0] += ax;
			    point[1] += ay;
			    point[2] += newax;
			    point[3] += neway;
			    break;
			case PathIterator.SEG_QUADTO:
			    point[0] += (newax + ax) / 2;
			    point[1] += (neway + ay) / 2;
			    break;
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
			case PathIterator.SEG_CLOSE:
			    break;
			}
			ax = newax;
			ay = neway;
		    }
		}
		switch (type) {
		case PathIterator.SEG_MOVETO:
                    mx = point[0];
                    my = point[1];
		    r.beginSubpath(point[0], point[1]);
		    break;
		case PathIterator.SEG_LINETO:
		    r.appendLine(point[0], point[1]);
		    break;
		case PathIterator.SEG_QUADTO:
                    // Quadratic curves take two points
		    r.appendQuadratic(point[0], point[1],
				      point[2], point[3]);
		    break;
		case PathIterator.SEG_CUBICTO:
                    // Cubic curves take three points
		    r.appendCubic(point[0], point[1],
				  point[2], point[3],
				  point[4], point[5]);
		    break;
		case PathIterator.SEG_CLOSE:
		    r.closedSubpath();
                    pathClosed = true;
		    break;
		}
		pi.next();
	    }
	}

        try {
            r.endPath();
        } catch (PRException e) {
            /*
             * This exeption is thrown from the native part of the Ductus
             * (only in case of a debug build) to indicate that some
             * segments of the path have very large coordinates.
             * See 4485298 for more info.
             */
            System.err.println("DuctusRenderer.createShapeRasterizer: " +  e);
        }

        return r;
    }
}
