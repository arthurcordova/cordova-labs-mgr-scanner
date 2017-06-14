package com.compa.recognizesheet;

import static com.compa.recognizesheet.ProcessImage.toBitmap;
import static com.compa.recognizesheet.utils.CommonUtils.info;

import org.opencv.core.Mat;

import com.compa.recognizesheet.utils.CommonUtils;
import com.compa.recognizesheet.views.TouchImageView;
import com.compa.recognizesheet.views.TouchImageView.OnTouchImageViewListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

/**
 * 
 * @author Sea
 *
 */
public class RecognizeActivity extends Activity {
	static int REQUEST_IMAGE_CAPTURE = 1;
	Button btnStartCamera;
	Button btnExit;
	Button btnSubmit;
	Button btnIncrease;
	Button btnDecrease;
	private TouchImageView image;
	private EditText studentIdCode;
	private EditText tpCode;

	private int sourceW = 0;
	private int sourceH = 0;
	private String lastFileName = "";
	private boolean isRecognized = false;

	// User input
	private int numberOfAnwser = 100;
	private String username = "";
	private String password = "";
	private boolean isFromCamera = false;

	// A Progressing bar
	ProgressDialog progressBar;

	static ProcessImage processImg = new ProcessImage();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recognize);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Bundle b = getIntent().getExtras();
		username = b.getString("username");
		password = b.getString("password");
		numberOfAnwser = Integer.parseInt(b.getString("numberOfAnwser"));
		ProcessImage.thresholdMin = Integer.parseInt(b.getString("threshold"));
		if (numberOfAnwser > 100) {
			numberOfAnwser = 100;
		}
		ProcessImage.numberOfAnwser = numberOfAnwser;

		btnStartCamera = (Button) findViewById(R.id.btnStartCamera);
		btnExit = (Button) findViewById(R.id.btnExit);
		btnSubmit = (Button) findViewById(R.id.btnSubmit);
		btnIncrease = (Button) findViewById(R.id.btnIncreaseThreshold);
		btnDecrease = (Button) findViewById(R.id.btnDecreaseThreshold);

		btnStartCamera.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					takePicture();
				}
				return false;
			}
		});

		btnExit.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					existApp();
				}
				return false;
			}
		});

		btnSubmit.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					submitResult();
				}
				return false;
			}
		});

		btnIncrease.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					increaseThreshold();
				}
				return false;
			}
		});

		btnDecrease.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					decreaseThreshold();
				}
				return false;
			}
		});

		studentIdCode = (EditText) findViewById(R.id.text_id);
		tpCode = (EditText) findViewById(R.id.text_tp);
		image = (TouchImageView) findViewById(R.id.grid_img);
		image.setScaleType(ScaleType.CENTER_INSIDE);
		image.setOnTouchImageViewListener(new OnTouchImageViewListener() {

			@Override
			public void onMove() {
				if (isRecognized && image.lastPointMark != null) {
					info("------");
					processImg.updateAsMarked(image.lastPointMark.x * sourceW / image.viewWidth,
							image.lastPointMark.y * sourceW / image.viewWidth, numberOfAnwser);
					image.setImageBitmap(toBitmap(processImg.drawAnswered(numberOfAnwser)));
					info(processImg.getFinalAnsweredString(numberOfAnwser));
					image.lastPointMark = null;
				}
			}
		});

		enableChangeThreshold(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recognize, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void takePicture() {
		Intent takePicIntent = new Intent(RecognizeActivity.this, AndroidCamera.class);
		lastFileName = CommonUtils.APP_PATH + "capture" + System.currentTimeMillis() + ".jpg";
		takePicIntent.putExtra("output", lastFileName);
		info(lastFileName);
		startActivityForResult(takePicIntent, REQUEST_IMAGE_CAPTURE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		enableChangeThreshold(false);
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			// options.inSampleSize = 6;
			lastFileName = data.getStringExtra("output");

			info("Recognize image: " + lastFileName);
			Bitmap imageBitmap = BitmapFactory.decodeFile(lastFileName, options);

			if (imageBitmap == null) {
				isRecognized = false;
				dialogTakePicture("Can not recognize sheet. Please try again", "Retry", "Exist");
				return;
			}

			final Bitmap finalImageBitmap = imageBitmap.getWidth() > imageBitmap.getHeight()
					? rotateBitmap(imageBitmap, 90) : imageBitmap;

			int top = data.getIntExtra("top", 0);
			int bot = data.getIntExtra("bot", 0);
			int right = data.getIntExtra("right", 0);
			int left = data.getIntExtra("left", 0);

			image.setImageBitmap(finalImageBitmap);
			Mat subMat = ProcessImage.cropSub(finalImageBitmap, top, bot, right, left);
			isFromCamera = true;
			runProcessSubScreen(subMat);
		}
	}

	/**
	 * Display draw sheet answers.
	 */
	public void displayThresholdImg() {
		if (processImg.subThreshold != null) {
			image.setImageBitmap(toBitmap(processImg.subThreshold));
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					AsyncTask.execute(new Runnable() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									if (isRecognized) {
										image.setImageBitmap(toBitmap(processImg.drawAnswered(numberOfAnwser)));
									}
									enableChangeThreshold(true);
								}
							});
						}
					});
				}
			}, 5000);

		}

	}

	public void runProcessSubScreen(Mat subScreen) {
		studentIdCode.setText("");
		tpCode.setText("");
		showProgressBar("", "In processing.Please wait a seconds");
		new AsyncTask<Mat, Mat, Void>() {

			@Override
			protected Void doInBackground(Mat... params) {
				displayResult(params[0]);
				return null;
			}

			@Override
			protected void onPostExecute(Void v) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						updateAfterRecognize();
						hideProcessBar();
					}
				});
			}

		}.execute(subScreen);
	}

	public void updateAfterRecognize() {
		if (isRecognized) {
			enableSubmit(true);
			studentIdCode.setText(processImg.getFinalStudentIdString());
			tpCode.setText(processImg.TP);
		} else {
			enableSubmit(false);
		}
		enableChangeThreshold(true);
		if (!isFromCamera) {
			displayThresholdImg();
		} else if (isRecognized) {
			image.setImageBitmap(toBitmap(processImg.drawAnswered(numberOfAnwser)));
			sourceW = processImg.subAnswer.width();
			sourceH = processImg.subAnswer.height();
		} else if (processImg.subAnswer != null) {
			image.setImageBitmap(toBitmap(processImg.subAnswer));
		} else if (processImg.subScreen != null) {
			image.setImageBitmap(toBitmap(processImg.subScreen));
		}
	}

	public void displayResult(Mat imageMat) {
		info("Start recognize.....................");
		if (processImg.parseBitmap(imageMat, 0, 0, 0, 0)) {
			isRecognized = true;
		} else {
			isRecognized = false;
		}
		info("End recognize..................... Result: " + isRecognized);
	}

	public Bitmap rotateBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	public void dialogTakePicture(String message, String btnName1, String btnName2) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setMessage(message);
		dialog.setPositiveButton(btnName1, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				takePicture();
			}
		});

		if (btnName2 != "") {
			dialog.setNegativeButton(btnName2, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					existApp();
				}
			});
		}

		AlertDialog alertDialog = dialog.create();
		alertDialog.show();
	}

	public void existApp() {
		CommonUtils.writeThreshold(ProcessImage.thresholdMin);
		CommonUtils.cleanFolder();
		this.finish();
	}

	public void submitResult() {
		enableSubmit(false);
		StringBuilder answers = new StringBuilder();
		answers.append("\nStudent Id: " + studentIdCode.getText().toString());
		answers.append("\nTP: " + tpCode.getText().toString());
		answers.append("\nAnswer: " + processImg.getFinalAnsweredString(numberOfAnwser));
		info(answers);
		info("Submit success!");
		dialogTakePicture(answers.toString() + "\nSubmit successful!", "Continue", "Exit");
		enableSubmit(true);
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

	public void increaseThreshold() {
		if (processImg.subScreen != null) {
			ProcessImage.thresholdMin += 5;
			Toast.makeText(this, "Bright: " + ProcessImage.thresholdMin, Toast.LENGTH_LONG).show();
			isFromCamera = false;
			runProcessSubScreen(processImg.subScreen);
		}
	}

	public void decreaseThreshold() {
		if (processImg.subScreen != null) {
			ProcessImage.thresholdMin -= 5;
			Toast.makeText(this, "Bright: " + ProcessImage.thresholdMin, Toast.LENGTH_LONG).show();
			isFromCamera = false;
			runProcessSubScreen(processImg.subScreen);
		}
	}

	public void enableChangeThreshold(boolean enabled) {
		btnIncrease.setEnabled(enabled);
		btnDecrease.setEnabled(enabled);
		if (!enabled) {
			btnIncrease.setBackgroundColor(Color.LTGRAY);
			btnDecrease.setBackgroundColor(Color.LTGRAY);
		} else {
			btnIncrease.setBackgroundColor(Color.WHITE);
			btnDecrease.setBackgroundColor(Color.WHITE);
		}
	}

	public void enableSubmit(boolean enabled) {
		btnSubmit.setEnabled(enabled);
		if (!enabled) {
			btnSubmit.setBackgroundColor(Color.LTGRAY);
		} else {
			btnSubmit.setBackgroundColor(Color.WHITE);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			existApp();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		existApp();
	}
}
