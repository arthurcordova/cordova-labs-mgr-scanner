package com.compa.recognizesheet;

import static com.compa.recognizesheet.utils.CommonUtils.info;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.compa.recognizesheet.utils.CommonUtils;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * 
 * @author Sea
 *
 */
public class LoginActivity extends Activity {

	Button btnLogin;
	EditText usernameText;
	EditText passwordText;
	EditText numberAnswerText;
	Spinner thresholdText;
	final int MULTIPLE_PERMISSIONS = 10;

	ProgressDialog progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		String[] PERMISSIONS = { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.INTERNET };

		if (!hasPermissions(this, PERMISSIONS)) {
			ActivityCompat.requestPermissions(this, PERMISSIONS, MULTIPLE_PERMISSIONS);
		} else {
			CommonUtils.cleanFolder();
		}

		usernameText = (EditText) findViewById(R.id.usernameText);
		passwordText = (EditText) findViewById(R.id.passwordText);
		numberAnswerText = (EditText) findViewById(R.id.numberAnswerText);
		thresholdText = (Spinner) findViewById(R.id.thresholdText);

		btnLogin = (Button) findViewById(R.id.btnLogin);
		btnLogin.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					callLogin();
				}
				return false;
			}
		});
	}

	
	@Override
	public void onResume(){
	    super.onResume();
	    int oldThreshold = CommonUtils.readThreshold();
		if (oldThreshold == -1) {
			thresholdText.setSelection(4);
		} else {
			String[] threshold_values = getResources().getStringArray(R.array.threshold_array);
			for (int i = 0; i < threshold_values.length; i++) {
				if (Integer.valueOf(threshold_values[i]) >= oldThreshold) {
					thresholdText.setSelection(i);
					break;
				}
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// Call API to login
	public void callLogin() {
		// showProgressBar("In processing...", "Please wait a seconds.");
		// hideProcessBar();

		Intent recognizeActivity = new Intent(getApplicationContext(), RecognizeActivity.class);
		// Clears History of Activity
		recognizeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Bundle b = new Bundle();
		// Information to validate. Enter any to pass
		b.putString("username", usernameText.getText().toString());
		b.putString("password", passwordText.getText().toString());
		b.putString("numberOfAnwser", numberAnswerText.getText().toString());
		b.putString("threshold", thresholdText.getSelectedItem().toString());
		recognizeActivity.putExtras(b);
		startActivity(recognizeActivity);
	}

	public static boolean hasPermissions(Context context, String... permissions) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
		case MULTIPLE_PERMISSIONS: {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// permissions granted.
			} else {
				this.finish();
			}
			return;
		}
		}
	}

	public void showProgressBar(String title, String message) {
		progressBar = ProgressDialog.show(this, title, message, false, false);
	}

	public void hideProcessBar() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (progressBar != null && progressBar.isShowing()) {
					progressBar.dismiss();
				}
			}
		});
	}

	public void writeFile(String fileName) {
		// Create file in folde.
		byte[] data = "hello".getBytes();
		OutputStream imageFileOS = null;
		try {

			File file = new File(fileName);
			Uri testUri = Uri.fromFile(file);
			imageFileOS = this.getContentResolver().openOutputStream(testUri);
			imageFileOS.write(data);
			imageFileOS.flush();
			// imageFileOS.close();

			info("Data saved: " + testUri.toString());
		} catch (Exception e) {
			Log.e("COMPA", "Cannot create file1: " + fileName + ". Message: " + e.getMessage(), e);
		} finally {
			if (imageFileOS != null) {
				try {
					imageFileOS.close();
				} catch (Exception e) {

				}
			}
		}
	}

	public void writeFile2(String fileName) {
		// Create file in folde.
		byte[] data = "hello".getBytes();
		File file = new File(fileName);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(data);
			info("Data saved: " + fileName);
		} catch (Exception e) {
			Log.e("COMPA", "Cannot create file2: " + fileName + ". Message: " + e.getMessage(), e);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ioe) {

			}
		}

	}

	public void writeFile3(String fileName) {
		// Create file in folde.
		byte[] data = "hello".getBytes();
		FileOutputStream fos = null;
		try {
			fos = openFileOutput(fileName, Context.MODE_WORLD_READABLE);
			fos.write(data);
			info("Data saved: " + fileName);
		} catch (Exception e) {
			Log.e("COMPA", "Cannot create file3: " + fileName + ". Message: " + e.getMessage(), e);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ioe) {

			}
		}

	}

	public void tesFolderPermission() {
		Log.i("COMPA", "Create files");
		writeFile(CommonUtils.APP_PATH + "file11.txt");
		writeFile(Environment.getExternalStorageDirectory() + "/Download/file12.txt");
		writeFile(Environment.getExternalStorageDirectory() + "/file13.txt");
		writeFile2(CommonUtils.APP_PATH + "file21.txt");
		writeFile2(Environment.getExternalStorageDirectory() + "/Download/file22.txt");
		writeFile2(Environment.getExternalStorageDirectory() + "/file23.txt");

		writeFile3(CommonUtils.APP_PATH + "file31.txt");
		writeFile3(Environment.getExternalStorageDirectory() + "/Download/file32.txt");
		writeFile3(Environment.getExternalStorageDirectory() + "/file33.txt");

		Log.i("COMPA", "Delete file");
		deleteExist(CommonUtils.APP_PATH + "file11.txt");
		deleteExist(Environment.getExternalStorageDirectory() + "/Download/file12.txt");
		deleteExist(Environment.getExternalStorageDirectory() + "/file13.txt");
		deleteExist(CommonUtils.APP_PATH + "file21.txt");
		deleteExist(Environment.getExternalStorageDirectory() + "/Download/file22.txt");
		deleteExist(Environment.getExternalStorageDirectory() + "/file23.txt");
		deleteExist(CommonUtils.APP_PATH + "file31.txt");
		deleteExist(Environment.getExternalStorageDirectory() + "/Download/file32.txt");
		deleteExist(Environment.getExternalStorageDirectory() + "/file33.txt");

	}

	public boolean deleteExist(String fileName) {
		boolean rs = false;
		try {
			File f = new File(fileName);
			rs = f.delete();
			if (!rs) {
				Log.i("COMPA", "Cannot delete file " + fileName);
			} else {
				Log.i("COMPA", "Deleted file: " + fileName);
			}

		} catch (Exception e) {
			Log.e("COMPA", "Error delete file. Message: " + e.getMessage(), e);
		}

		return rs;
	}
}
