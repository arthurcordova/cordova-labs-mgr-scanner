package com.compa.recognizesheet;

import static com.compa.recognizesheet.utils.CommonUtils.TAG;
import static com.compa.recognizesheet.utils.CommonUtils.info;
import static org.opencv.core.Core.circle;
import static org.opencv.core.Core.line;
import static org.opencv.core.Core.rectangle;
import static org.opencv.highgui.Highgui.imwrite;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.RETR_CCOMP;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;
import static org.opencv.imgproc.Imgproc.warpPerspective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import com.compa.recognizesheet.utils.CommonUtils;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * 
 * @author Sea
 *
 */
public class ProcessImage {

	static {
		if (!OpenCVLoader.initDebug()) {
			Log.e(TAG, "Unable to load OpenCV");
		} else {
			Log.i(TAG, "OpenCV loaded");
		}
	}

	public static boolean DEBUG = false;
	public static boolean DRAW_GRID = true;

	public static int markedColor = 3; // 0: blue, 1: green, 2: red, other:
										// black

	public static int sourceWidth = 2500;// 1366;
	public static float numCol = 27;
	public static float numRow = 25;
	public static float numColId = 11;
	public static float numRowId = 12;
	public static int numberOfAnwser = 100;
	public static int thresholdMin = 85;
	public static int thresholdMax = 255;
	public static int circleMarked = 9;
	public static String[] answers = new String[] { "A", "B", "C", "D", "E" };
	public static Scalar colorGrid = new Scalar(30, 255, 30);
	public static Scalar colorMarked = new Scalar(255, 200, 00);

	public Map<String, MarkedPoint> mapAnswerPoint = new HashMap<String, MarkedPoint>();
	public Map<String, MarkedPoint> mapStudentIdPoint = new HashMap<String, MarkedPoint>();
	public String TP = "";

	public Mat subAnswer = null;
	public Mat subStudentId = null;
	public Mat subScreen = null;
	public Mat subThreshold = null;

	public Point TL = null;
	public Point TR = null;
	public Point ML = null;
	public Point MR = null;
	public Point ML2 = null;
	public Point MR2 = null;
	public Point BL = null;
	public Point BR = null;

	public static void writeImage(String name, Mat origin) {
		if (!DEBUG) {
			return;
		}
		String appPath = CommonUtils.APP_PATH;
		info("Writing " + appPath + name + ".jpg ...");
		imwrite(appPath + name + ".jpg", origin);
	}

	public boolean parseBitmap(Bitmap bitmap, int top, int bot, int right, int left) {
		try {
			Mat origin = new Mat();
			Utils.bitmapToMat(bitmap, origin);
			info("Mat size: "+ origin.width() + ":" + origin.height());
			// Crop image
			info("Crop T: " + top + " B: " + bot + " R: " + right + " L: " + left);
			if (top != 0 && bot != 0 && right != 0 && left != 0) {
				
				origin = origin.submat(new Rect(right, top, left, bot));
				String appPath = CommonUtils.APP_PATH;
				writeImage(appPath + "crop", origin);
			}

			boolean result = parseTable(origin);
			origin.release();
			return result;
		} catch (Exception e) {
			//Log.e(TAG, "Error parse image. Message: " + e.getMessage(), e);
		}
		return false;
	}
	
	public boolean parseBitmap(Mat mat, int top, int bot, int right, int left) {
		try {
			Mat origin = mat;
			info("Mat size: "+ mat.width() + ":" + mat.height());
			// Crop image
			info("Crop T: " + top + " B: " + bot + " R: " + right + " L: " + left);
			if (top != 0 && bot != 0 && right != 0 && left != 0) {
				
				origin = origin.submat(new Rect(right, top, left, bot));
				String appPath = CommonUtils.APP_PATH;
				writeImage(appPath + "crop", origin);
			}

			boolean result = parseTable(origin);
			origin.release();
			return result;
		} catch (Exception e) {
			//Log.e(TAG, "Error parse image. Message: " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Parse image
	 * 
	 * @param origin
	 * @return
	 */
	public boolean parseTable(Mat origin) {
		info("parseTable");
		subAnswer = null;
		subStudentId = null;
		subScreen = null;
		subThreshold = null;
		
		TP = "";

		TR = null;
		ML = null;
		MR = null;
		ML2 = null;
		MR2 = null;
		BL = null;
		BR = null;

		Size imgSize = origin.size();
		resize(origin, origin, new Size(sourceWidth, imgSize.height * sourceWidth / imgSize.width), 1.0, 1.0,
				INTER_CUBIC);

		subScreen = origin.clone();
		writeImage("resize", origin);

		Mat originGray = new Mat();
		cvtColor(origin, originGray, COLOR_BGR2GRAY);
		Mat element1 = getStructuringElement(MORPH_RECT, new Size(9, 9), new Point(3, 3));
		Mat element2 = getStructuringElement(MORPH_RECT, new Size(9, 9), new Point(3, 3));
		dilate(originGray, originGray, element1);
		erode(originGray, originGray, element2);

		GaussianBlur(originGray, originGray, new Size(5, 5), 0);
		threshold(originGray, originGray, thresholdMin, thresholdMax, THRESH_BINARY);
		
		info("thresholded");
		subThreshold = originGray.clone();
		writeImage("gray", originGray);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat grayTemp = originGray.clone();
		findContours(grayTemp, contours, new Mat(), RETR_CCOMP, CHAIN_APPROX_SIMPLE);
		grayTemp.release();
		List<MatOfPoint> lstContour = new ArrayList<MatOfPoint>();
		List<Point> lstPoint = new ArrayList<Point>();

		info("Recognizing squares");
		// Recognize grid
		for (int i = 0; i < contours.size(); i++) {
			MatOfPoint tempContour = contours.get(i);
			// Rect to check square
			Rect rect = boundingRect(tempContour);
			float ratioX = (float) rect.width / rect.height;
			float ratioY = (float) rect.height / rect.width;
			// rectangle(origin, rect.tl(), rect.br(), colorGrid, 3);
			// info(ratioX + " " + ratioY + " " + rect.width);
			if (ratioX < 1.35 && ratioX > 0.75 && ratioY < 1.35 && ratioY > 0.75 && rect.width > 50
					&& rect.width < 1000) {

				if (DEBUG)
					rectangle(origin, rect.tl(), rect.br(), new Scalar(0, 0, 255), 3);
				lstContour.add(tempContour);
				lstPoint.addAll(Arrays.asList(tempContour.toArray()));
			}
		}

		writeImage("contour", origin);

		info("List contours size: " + lstContour.size());

		Rect rectBounding = boundingRect(new MatOfPoint(lstPoint.toArray(new Point[lstPoint.size()])));

		if (DEBUG)
			rectangle(origin, rectBounding.tl(), rectBounding.br(), new Scalar(100, 255, 0), 4);

		// Loop to get 4 square around answer table,(not two on top)
		List<MatOfPoint> lstValidMoP = new ArrayList<MatOfPoint>();
		Iterator<MatOfPoint> iContour = lstContour.iterator();

		lstPoint.clear();
		info("get top bot right left");
		float minSquareX = (rectBounding.x + rectBounding.width) / numCol;
		while (iContour.hasNext()) {
			MatOfPoint tempMoP = iContour.next();

			Rect temp = boundingRect(tempMoP);

			// Left
			if (temp.x < (rectBounding.x + minSquareX * 1.3)) {
				if (temp.y > rectBounding.y + rectBounding.height / 6.5) {
					if (temp.y + temp.height > rectBounding.y + rectBounding.height - temp.height) {
						BL = new Point(temp.x + temp.width, temp.y);
					} else {
						if (ML == null) {
							ML = temp.br();
						} else if (ML.y < temp.y) {
							ML = temp.br();
						}

						if (ML2 == null) {
							ML2 = new Point(temp.x + temp.width, temp.y);
						} else if (ML2.y > temp.y) {
							ML2 = new Point(temp.x + temp.width, temp.y);
						}
					}

					lstValidMoP.add(tempMoP);
					lstPoint.addAll(Arrays.asList(tempMoP.toArray()));
				} else if (true) {
					// TOP Left
					TL = temp.br();
				}

				continue;
			}

			// Right
			if (temp.x > (rectBounding.br().x - minSquareX * 2)) {
				if (temp.y > rectBounding.y + rectBounding.height / 6.5) {
					if (temp.y + temp.height > rectBounding.y + rectBounding.height - temp.height) {
						BR = temp.tl();
					} else {
						if (MR == null) {
							MR = new Point(temp.x, temp.y + temp.height);
						} else if (MR.y > temp.y) {
							MR = new Point(temp.x, temp.y + temp.height);
						}

					}

					lstValidMoP.add(tempMoP);
					lstPoint.addAll(Arrays.asList(tempMoP.toArray()));
				}
			}
			if (temp.y < rectBounding.y + temp.height) {
				if (TR == null) {
					TR = new Point(temp.x, temp.y + temp.height);
				} else if (TR.y > temp.y) {
					TR = new Point(temp.x, temp.y + temp.height);
				}

			}
		}
		// iContour = lstContour.iterator();
		// while (iContour.hasNext()) {
		// MatOfPoint tempMoP = iContour.next();
		//
		// Rect temp = boundingRect(tempMoP);
		// if (temp.x > (rectBounding.x + minSquareX)) {
		// if ((TR != null && temp.y > rectBounding.y + temp.height && TR.x <
		// (temp.x + temp.width / 2)
		// && (temp.x + temp.width / 2) < (TR.x + temp.width))
		// || (ML2 != null && ML2.y < (temp.y + temp.height / 2)
		// && (temp.y + temp.height / 2) < (ML2.y + temp.height))) {
		// if (MR2 == null) {
		// MR2 = temp.tl();
		// }
		// }
		// }
		// }

		if (DEBUG) {
			if (TL != null)
				circle(origin, TL, 10, new Scalar(200, 255, 0), 5);
			if (TR != null)
				circle(origin, TR, 10, new Scalar(200, 255, 0), 5);
			if (ML != null)
				circle(origin, ML, 10, new Scalar(200, 255, 0), 5);
			if (MR != null)
				circle(origin, MR, 10, new Scalar(200, 255, 0), 5);
			if (BL != null)
				circle(origin, BL, 10, new Scalar(200, 255, 0), 5);
			if (BR != null)
				circle(origin, BR, 10, new Scalar(200, 255, 0), 5);
			if (ML2 != null)
				circle(origin, ML2, 10, new Scalar(200, 255, 0), 5);
			if (MR2 != null)
				circle(origin, MR2, 10, new Scalar(200, 255, 0), 5);
			writeImage("draw_cocern", origin);
		}

		if (MR == null || ML == null || BR == null || BL == null || TL == null || TR == null // ||
																								// MR2
																								// ==
																								// null
				|| ML2 == null) {
			info("Missing TOP or BOT or RIGHT or LEFT");
			originGray.release();
			originGray = null;
			return false;
		}

		info("Extrating marked answer");

		// Math calculate
		Mat subAnswerGray = cropSub(originGray, ML, MR, BL, BR);
		subThreshold.release();
		subThreshold = subAnswerGray.clone();
		subAnswer = cropSub(origin, ML, MR, BL, BR);

		int subAnsWidth = subAnswer.width();
		int subAnsHeight = subAnswer.height();
		info("Size sub answer: " + subAnsWidth + ":" + subAnsHeight);
		double avrgWidth = subAnsWidth / numCol;
		double halfWidth = avrgWidth / 2;

		double avrgHeight = subAnsHeight / numRow;
		double halfHeight = avrgHeight / 2;

		for (int j = 1; j <= numRow; j++) {

			// Re calculate math for Left to Right
			double posYMin = (j - 1) * avrgHeight + halfHeight;
			Point pl = new Point(0, posYMin);
			Point pr = new Point(subAnsWidth, posYMin);

			Point plMin = new Point(0, posYMin - halfHeight);
			Point prMin = new Point(subAnsWidth, plMin.y);

			if (DRAW_GRID)
				line(subAnswer, plMin, prMin, colorGrid, 2);

			for (int i = 2; i <= numCol; i++) {
				if (!((i >= 2 && i <= 7) || (i >= 9 && i <= 14) || (i >= 16 && i <= 21) || (i >= 23 && i <= 27))) {
					continue;
				}
				double posXMin = (i - 1) * avrgWidth + halfWidth;
				Point pt = new Point(posXMin, 0);
				Point pb = new Point(posXMin, subAnsHeight);

				Point ptMin = new Point(posXMin - halfWidth, 0);
				Point pbMin = new Point(ptMin.x, subAnsHeight);
				if (DRAW_GRID)
					line(subAnswer, ptMin, pbMin, colorGrid, 2);

				if (i == 7 || i == 14 || i == 21) {
					continue;
				}

				Point topLeftCorner = findIntersectionPoint(plMin, prMin, ptMin, pbMin);
				int colmn = -1;
				String alpha = "";

				if (i >= 2 && i <= 6) {
					colmn = 0;
					alpha = answers[i - 2];
				} else if (i >= 9 && i <= 13) {
					colmn = 1;
					alpha = answers[i - 9];
				} else if (i >= 16 && i <= 20) {
					colmn = 2;
					alpha = answers[i - 16];
				} else if (i >= 23 && i <= 27) {
					colmn = 3;
					alpha = answers[i - 23];
				}
				String k = (int) (j + colmn * numRow) + "";

				// Find Intersection point
				Point inter = findIntersectionPoint(pl, pr, pt, pb);
				boolean isMarked = isMarked(subAnswerGray, (int) inter.y, (int) inter.x);

				MarkedPoint p = new MarkedPoint(inter, topLeftCorner, topLeftCorner.x + avrgWidth,
						topLeftCorner.y + avrgHeight, Integer.parseInt(k), isMarked, alpha);
				mapAnswerPoint.put(k + alpha, p); // row 1: 1A, 1B..
			}
		}
		subAnswerGray.release();
		subAnswerGray = null;

		info("Extrating student Id & TP");

		Point ML_TopRight = new Point(ML.x, ML.y - avrgHeight);
		Point MR_Fake_TopLeft = new Point(TR.x, MR.y - avrgHeight);

		Mat subStudentIdGray = cropSub(originGray, TL, TR, ML_TopRight, MR_Fake_TopLeft);
		// subStudentId = cropSub(origin, TL, TR, ML_TopRight, MR_Fake_TopLeft);
		// writeImage("student", subStudentId);
		int subAnsWidthId = subStudentIdGray.width();
		int subAnsHeightId = subStudentIdGray.height();
		info("Size sub answer: " + subAnsWidthId + ":" + subAnsHeightId);
		double avrgWidthId = subAnsWidthId / numColId;
		double halfWidthId = avrgWidthId / 2;

		double avrgHeightId = subAnsHeightId / numRowId;
		double halfHeightId = avrgHeightId / 2;

		for (int j = 1; j <= numRowId; j++) {
			if (j == 11) {
				continue;
			}

			// Re calculate math for Left to Right
			double posYMin = (j - 1) * avrgHeightId + halfHeightId;
			Point pl = new Point(0, posYMin);
			Point pr = new Point(subAnsWidthId, posYMin);

			Point plMin = new Point(0, posYMin - halfHeightId);
			Point prMin = new Point(subAnsWidthId, plMin.y);

			for (int i = 2; i <= numColId; i++) {
				double posXMin = (i - 1) * avrgWidthId + halfWidthId;
				Point pt = new Point(posXMin, 0);
				Point pb = new Point(posXMin, subAnsHeightId);

				Point ptMin = new Point(posXMin - halfWidthId, 0);
				Point pbMin = new Point(ptMin.x, subAnsHeightId);

				// Find inter top left
				Point topLeftCorner = findIntersectionPoint(plMin, prMin, ptMin, pbMin);

				// Find Intersection point
				Point inter = findIntersectionPoint(pl, pr, pt, pb);
				boolean isMarked = isMarked(subStudentIdGray, (int) inter.y, (int) inter.x);

				// Check if TP
				if (j == 12) {
					if (isMarked) {
						TP += answers[i - 2];
					}
					if (i == 6) {
						break;
					}
				}
				String alpha = (i - 2) + "";
				String k = j + "";
				MarkedPoint p = new MarkedPoint(inter, topLeftCorner, j, isMarked, alpha);
				mapStudentIdPoint.put(k + alpha, p);// row 1: 10,11,12...
			}
		}
		// writeImage("gray_student", subStudentIdGray);

		subStudentIdGray.release();
		subStudentIdGray = null;

		info("Done recognize");
		originGray.release();
		originGray = null;

		// subAnswer.convertTo(subAnswer, -1, 1, 50);
		writeImage("sub_answer", drawAnswered(100));

		info("AS: " + getFinalAnsweredString(numberOfAnwser));
		info("ID: " + getFinalStudentIdString());
		info("TP: " + TP);
		return true;
	}

	public static Mat cropSub(Bitmap bitmap,  int top, int bot, int right, int left) {
		Mat origin = new Mat();
		Utils.bitmapToMat(bitmap, origin);
		info("Mat size: "+ origin.width() + ":" + origin.height());
		// Crop image
		info("Crop T: " + top + " B: " + bot + " R: " + right + " L: " + left);
		if (top != 0 && bot != 0 && right != 0 && left != 0) {
			
			origin = origin.submat(new Rect(right, top, left, bot));
			String appPath = CommonUtils.APP_PATH;
			writeImage(appPath + "crop", origin);
		}
		
		return origin;
	}
	
	/**
	 * Crop sub mat by four points.S
	 * 
	 * @param origin
	 * @param tl
	 * @param tr
	 * @param bl
	 * @param br
	 * @return
	 */
	public Mat cropSub(Mat origin, Point tl, Point tr, Point bl, Point br) {
		info("cropSub");

		int resultWidth = (int) (tr.x - tl.x);
		int bottomWidth = (int) (br.x - bl.x);
		if (bottomWidth > resultWidth)
			resultWidth = bottomWidth;

		int resultHeight = (int) (bl.y - tl.y);
		int bottomHeight = (int) (br.y - tr.y);
		if (bottomHeight > resultHeight)
			resultHeight = bottomHeight;

		List<Point> source = new ArrayList<Point>();
		source.add(tl);
		source.add(tr);
		source.add(bl);
		source.add(br);
		Mat startM = Converters.vector_Point2f_to_Mat(source);

		Point outTL = new Point(0, 0);
		Point outTR = new Point(resultWidth, 0);
		Point outBL = new Point(0, resultHeight);
		Point outBR = new Point(resultWidth, resultHeight);
		List<Point> dest = new ArrayList<Point>();
		dest.add(outTL);
		dest.add(outTR);
		dest.add(outBL);
		dest.add(outBR);
		Mat endM = Converters.vector_Point2f_to_Mat(dest);

		Mat subTrans = getPerspectiveTransform(startM, endM);
		Mat subMat = new Mat();
		warpPerspective(origin, subMat, subTrans, new Size(resultWidth, resultHeight));
		subTrans.release();
		return subMat;
	}

	/**
	 * Draw rect by 4 points.
	 * 
	 * @param origin
	 * @param tl
	 * @param tr
	 * @param bl
	 * @param br
	 * @param color
	 * @param size
	 * @return
	 */
	public Mat drawRect(Mat origin, Point tl, Point tr, Point bl, Point br, Scalar color, int size) {
		line(origin, tl, tr, color, size);
		line(origin, tl, bl, color, size);
		line(origin, tr, br, color, size);
		line(origin, bl, br, color, size);
		return origin;
	}

	/**
	 * Clear an answer row.
	 * 
	 * @param map
	 * @param index
	 */
	public void emptyAnswer(Map<String, MarkedPoint> map, int index) {
		for (String alpha : answers) {
			map.get(index + alpha).marked = false;
		}
	}

	/**
	 * Clear ID
	 * 
	 * @param map
	 * @param index
	 */
	public void emptyStudentId(Map<String, MarkedPoint> map, int index) {
		for (int j = 0; j <= numColId - 2; j++) {
			map.get(index + "" + j).marked = false;
		}
	}

	/**
	 * Draw marked answers.
	 * 
	 * @param origin
	 * @param map
	 * @param color
	 * @param radius
	 * @param thick
	 * @param numberOfAnswer
	 * @return
	 */
	public Mat drawAnswered(Mat origin, Map<String, MarkedPoint> map, Scalar color, int radius, int thick,
			int numberOfAnswer) {

		Mat temp = origin.clone();
		for (int i = 1; i <= numberOfAnswer; i++) {
			MarkedPoint validMarked = null;
			for (String alpha : answers) {
				MarkedPoint p = map.get(i + alpha);
				if (p != null && p.marked) {
					if (validMarked == null) {
						validMarked = p;
					} else {
						validMarked = null;
						emptyAnswer(map, i);
					}
				}
			}
			if (validMarked != null)
				circle(temp, validMarked.center, radius, color, thick);
		}
		return temp;
	}

	/**
	 * Draw marked answers.
	 * 
	 * @param origin
	 * @param map
	 * @param numberOfAnswer
	 * @return
	 */
	public Mat drawAnswered(int numberOfAnswer) {
		Mat temp = subAnswer.clone();
		for (int i = 1; i <= numberOfAnswer; i++) {
			MarkedPoint validMarked = null;
			for (String alpha : answers) {
				MarkedPoint p = mapAnswerPoint.get(i + alpha);
				if (p != null && p.marked) {
					if (validMarked == null) {
						validMarked = p;
					} else {
						validMarked = null;
						emptyAnswer(mapAnswerPoint, i);
						break;
					}
				}
			}
			if (validMarked != null)
				circle(temp, validMarked.center, 6, colorMarked, 12);
		}
		return temp;
	}

	/**
	 * Draw marked ID.
	 * 
	 * @param origin
	 * @param map
	 * @param color
	 * @param radius
	 * @param thick
	 * @return
	 */
	public Mat drawStId(Mat origin, Map<String, MarkedPoint> map, Scalar color, int radius, int thick) {
		Mat temp = origin.clone();
		for (int i = 1; i <= numRowId; i++) {
			MarkedPoint validMarked = null;
			for (int j = 0; j <= numColId - 2; j++) {
				MarkedPoint p = map.get(i + "" + j);
				if (p != null && p.marked) {
					if (validMarked == null) {
						validMarked = p;
					} else {
						validMarked = null;
						emptyStudentId(map, i);
						break;
					}
				}
			}
			if (validMarked != null)
				circle(temp, validMarked.center, radius, color, thick);
		}
		return temp;
	}

	/**
	 * Draw marked Id.
	 * 
	 * @param origin
	 * @param map
	 * @return
	 */
	public Mat drawStId(Mat origin, Map<String, MarkedPoint> map) {
		Mat temp = origin.clone();
		for (int i = 1; i <= numRowId; i++) {
			MarkedPoint validMarked = null;
			for (int j = 0; j <= numColId - 2; j++) {
				MarkedPoint p = map.get(i + "" + j);
				if (p != null && p.marked) {
					if (validMarked == null) {
						validMarked = p;
					} else {
						validMarked = null;
						emptyStudentId(map, i);
						break;
					}
				}
			}
			if (validMarked != null)
				circle(temp, validMarked.center, 8, colorMarked, 16);
		}
		return temp;
	}

	/**
	 * Get map answers
	 * 
	 * @param map
	 * @param numberOfAnswer
	 * @return
	 */
	public Map<String, String> getFinalAnswered(int numberOfAnswer) {
		Map<String, String> finalAnswer = new HashMap<String, String>();
		for (int i = 1; i <= numberOfAnswer; i++) {
			MarkedPoint validMarked = null;
			for (String alpha : answers) {
				MarkedPoint p = mapAnswerPoint.get(i + alpha);
				if (p != null && p.marked) {
					if (validMarked == null) {
						validMarked = p;
					} else {
						validMarked = null;
						emptyAnswer(mapAnswerPoint, i);
						break;
					}
				}
			}
			String as = "";
			if (validMarked != null)
				as = validMarked.leter;
			finalAnswer.put(i + "", as);

		}
		return finalAnswer;
	}

	/**
	 * Get answers as String.
	 * 
	 * @param map
	 * @param numberOfAnswer
	 * @return
	 */
	public String getFinalAnsweredString(int numberOfAnswer) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= numberOfAnswer; i++) {
			MarkedPoint validMarked = null;
			for (String alpha : answers) {
				MarkedPoint p = mapAnswerPoint.get(i + alpha);
				if (p != null && p.marked) {
					if (validMarked == null) {
						validMarked = p;
					} else {
						validMarked = null;
						emptyAnswer(mapAnswerPoint, i);
						break;
					}
				}
			}
			String as = "";
			if (validMarked != null)
				as = validMarked.leter;
			if (!as.isEmpty())
				sb.append(i + ":" + as).append(";");

		}

		return sb.substring(0, sb.length() - 1);
	}

	/**
	 * Get map ID
	 * 
	 * @param map
	 * @return
	 */
	public Map<String, String> getFinalStudentId() {
		Map<String, String> finalId = new HashMap<String, String>();
		for (int i = 1; i <= numRowId - 1; i++) {
			MarkedPoint validMarked = null;
			for (int j = 0; j <= numColId - 2; j++) {
				MarkedPoint p = mapStudentIdPoint.get(i + "" + j);
				if (p != null && p.marked) {
					if (validMarked == null) {
						validMarked = p;
					} else {
						validMarked = null;
						emptyStudentId(mapStudentIdPoint, i);
						break;
					}
				}
			}
			String numberStr = "";
			if (validMarked != null)
				numberStr = validMarked.leter;

			finalId.put(i + "", numberStr);
		}
		return finalId;
	}

	/**
	 * Get ID as String.
	 * 
	 * @param map
	 * @return
	 */
	public String getFinalStudentIdString() {
		String result = "";
		for (int i = 1; i <= numRowId-1; i++) {
			String numberStr = "";
			for (int j = 0; j <= numColId - 2; j++) {
				MarkedPoint p = mapStudentIdPoint.get(i + "" + j);
				if (p != null && p.marked) {
					numberStr += j;
				}
			}
			if (numberStr.length() > 1) {
				numberStr = "";
			}
			result += numberStr;
		}
		return result;
	}

	/**
	 * Update answer marked.
	 * 
	 * @param map
	 * @param x
	 * @param y
	 */
	public void updateAsMarked(double x, double y, int numberOfAnswer) {
		boolean isMatched = false;
		for (int i = 1; i <= numberOfAnswer && !isMatched; i++) {
			for (String alpha : answers) {
				MarkedPoint p = mapAnswerPoint.get(i + alpha);
				if (p != null && p.topLeft.x < x && x < p.maxX && p.topLeft.y < y && y < p.maxY) {
					info(i + alpha);
					isMatched = true;
					// Un-marked
					if (p.marked) {
						emptyAnswer(mapAnswerPoint, i);
					} else {
						// Marked
						emptyAnswer(mapAnswerPoint, i);
						p.marked = true;
					}
					break;
				}
			}
		}
	}

	//////////////////////

	// Check if marked
	public boolean isMarked(Mat src, int row, int col) {
		double channels = src.channels();
		// In case src is Gray
		if (channels == 1) {
			// Check around the point
			for (int i = 3; i <= circleMarked; i++) {
				if (src.get(row + i, col)[0] == 0 || src.get(row, col + i)[0] == 0 || src.get(row + i, col + i)[0] == 0
						|| src.get(row - i, col)[0] == 0 || src.get(row, col - i)[0] == 0
						|| src.get(row - i, col - i)[0] == 0) {
					return true;
				}
			}
		}
		return false;
	}

	// Math
	public double getLineYIntesept(Point p, double slope) {
		return p.y - slope * p.x;
	}

	public Point findIntersectionPoint(Point line1Start, Point line1End, Point line2Start, Point line2End) {

		double slope1 = (line1End.x - line1Start.x) == 0 ? (line1End.y - line1Start.y)
				: (line1End.y - line1Start.y) / (line1End.x - line1Start.x);
		double slope2 = (line2End.x - line2Start.x) == 0 ? (line2End.y - line2Start.y)
				: (line2End.y - line2Start.y) / (line2End.x - line2Start.x);

		double yinter1 = getLineYIntesept(line1Start, slope1);
		double yinter2 = getLineYIntesept(line2Start, slope2);

		if (slope1 == slope2 && yinter1 != yinter2)
			return null;

		double x = (yinter2 - yinter1) / (slope1 - slope2);
		double y = slope1 * x + yinter1;

		return new Point(x, y);
	}

	public static Bitmap toBitmap(Mat mat) {
		Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mat, bitmap);
		return bitmap;
	}

}