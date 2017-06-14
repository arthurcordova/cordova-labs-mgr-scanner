package com.compa.recognizesheet;

import org.opencv.core.Point;

/**
 * 
 * @author Sea
 *
 */
public class MarkedPoint {
	Point center;
	Point topLeft;
	double maxX;
	double maxY;
	boolean marked;
	int index;
	String leter;

	public MarkedPoint(Point center, Point topLeft, int index, boolean marked, String leter) {
		this.center = center;
		this.topLeft = topLeft;
		this.index = index;
		this.marked = marked;
		this.leter = leter;
	}

	public MarkedPoint(Point center, Point topLeft, double maxX, double maxY, int index, boolean marked,
			String leter) {
		this.center = center;
		this.topLeft = topLeft;
		this.maxX = maxX;
		this.maxY = maxY;
		this.index = index;
		this.marked = marked;
		this.leter = leter;
	}
}