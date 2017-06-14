package com.compa.recognizesheet.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.util.Log;

/**
 * 
 * @author Sea
 *
 */
public class CommonUtils {
	public static String TAG = "COMPA";
	public static String APP_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/RecogizeApp/";
	public static String FILE_CONFIG = "config.cfg";

	public static void cleanFolder() {
		info("Create or empty folder");
		String datapath = APP_PATH;
		File tenpPath = new File(datapath);
		if (!tenpPath.exists()) {
			if (!tenpPath.mkdir()) {
				// Can not create path /RecognizeApp/
			}
		} else {
			for (File child : tenpPath.listFiles()) {
				if (!child.getName().contains(".cfg")) {
					child.delete();
				}
			}
		}
	}

	public static void info(Object msg) {
		Log.i(TAG, msg.toString());
	}
	
	public static void writeThreshold(int threshold) {
		File f = new File(APP_PATH + FILE_CONFIG);
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "Fail to create file: " + APP_PATH + FILE_CONFIG, e);
			}
		}
		OutputStreamWriter output = null;
		try {
			output = new OutputStreamWriter(new FileOutputStream(f));
			output.write("threshold:" + threshold + "\n");

		} catch (IOException e) {
			Log.e("Exception", "Fail to write file " + APP_PATH + FILE_CONFIG, e);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
				}
			}
		}

	}
	
	public static int readThreshold() {
		int threshold = -1;
		InputStream inputStream = null;
		BufferedReader bufferedReader = null;
		try {
			inputStream = new FileInputStream(new File(APP_PATH + FILE_CONFIG));

			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				bufferedReader = new BufferedReader(inputStreamReader);
				String receiveString = "";

				while ((receiveString = bufferedReader.readLine()) != null) {
					String[] key_value =  receiveString.trim().split(":");
					if ("threshold".equalsIgnoreCase(key_value[0])) {
						threshold = Integer.parseInt(key_value[1]);
						break;
					}
					
				}
			}
		} catch (FileNotFoundException e) {
			Log.e("login activity", "File not found: " + e.toString());
		} catch (IOException e) {
			Log.e("login activity", "Can not read file: " + e.toString());
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
				}
			}

			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}

		return threshold;
	}

}
