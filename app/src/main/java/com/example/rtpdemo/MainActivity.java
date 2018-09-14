package com.example.rtpdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.socks.library.KLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class MainActivity extends Activity implements OnClickListener {

	final static String TAG = "MainActivity";

	private Button btn_init_sender;
	private Button btn_init_receiver;
	private Button btn_send;
	private Button btn_clear;
	private ImageView imageView;

	private InitSession session;

	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 100) {
				byte[] data = (byte[]) msg.obj;
				Glide.with(getApplicationContext()).load(data).into(imageView);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btn_init_sender = (Button) findViewById(R.id.btn_init_sender);
		btn_init_receiver = (Button) findViewById(R.id.btn_init_receiver);
		btn_send = (Button) findViewById(R.id.btn_send);
		btn_clear = (Button) findViewById(R.id.btn_clear);
		imageView = (ImageView) findViewById(R.id.imageView);

		btn_init_sender.setOnClickListener(this);
		btn_init_receiver.setOnClickListener(this);
		btn_send.setOnClickListener(this);
		btn_clear.setOnClickListener(this);

	}

	public void openSession() {
		long teststart = System.currentTimeMillis();
		String str = "1"
				;

		byte[] picData = null;
		File file = new File(Environment.getExternalStorageDirectory() + "/250k.jpg");
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			picData = ImageDispose.readStream(fileInputStream);
			KLog.i(TAG, "要发送的文件大小为picData len=" + picData.length);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}


		byte[] data = str.getBytes();
		KLog.i(TAG, "data len =" + data.length);
		int i = 0;
//		while (i < data.length) {
//			KLog.i("send " + i);
////			session.rtpSession.sendData(data);
//			i++;
//		}

		//分包发送
//		sendData(data);
		sendData(picData);

		long testend = System.currentTimeMillis();
		KLog.i("cost:" + (testend - teststart));
		KLog.i("start:" + teststart);
		KLog.i("end:" + testend);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn_init_sender:
			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					session = new InitSession(9020, 9030, 9000, 9010, "10.12.115.16", new DataFullCallBack() {
						@Override
						public void dataFullCB(byte[] data) {


						}
					});
					KLog.i(TAG, "========btn_init_sender=======");
				}
			}).start();
			break;
		case R.id.btn_init_receiver:
			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					session = new InitSession(9000, 9010, 9020, 9030, "10.12.114.37", new DataFullCallBack() {
						@Override
						public void dataFullCB(byte[] data) {
							Message msg = new Message();
							msg.what = 100;
							msg.obj = data;

							if (handler != null) {
								handler.sendMessage(msg);
							}


						}
					});
					// session = new InitSession(9020, 9030, 9000, 9010, "10.45.8.61");

					KLog.i(TAG, "========btn_init_receiver=======");

				}
			}).start();
			break;
		case R.id.btn_send:
			Thread sendThread = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						openSession();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			sendThread.start();
			break;

			case R.id.btn_clear:
				imageView.setImageDrawable(null);
				break;

		default:
			break;
		}
	}

	/**
	 * 将每帧进行分包并发送数据
	 * @param bytes
	 */
	private void sendData(byte[] bytes) {
		int dataLength = (bytes.length - 1) / 1480 + 1;
		KLog.i("dataLength包个数=" + dataLength);
		final byte[][] data = new byte[dataLength][];
		final boolean[] marks = new boolean[dataLength];

		int seqNumber = 0;
		long[] seqNumbers = new long[dataLength];
		for (int i = 0;i < dataLength;i++) {
			seqNumbers[i] = seqNumber;
			try {
				seqNumber++;
			} catch (Throwable t) {
				seqNumber = 0;
			}
		}

		marks[marks.length - 1] = true;
		int x = 0;
		int y = 0;
		int length = bytes.length;
		for (int i = 0; i < length; i++){
			if (y == 0){
				data[x] = new byte[length - i > 1480 ? 1480 : length - i];
			}
			data[x][y] = bytes[i];
			y++;
			if (y == data[x].length){
				y = 0;
				x++;
			}
		}
		// TODO: 17/6/15
		KLog.d("send data len=发送包的包个数" + data.length);

		session.rtpSession.sendData(data, null, marks, -1, seqNumbers);
		Log.e(TAG, "sendData: " + Arrays.deepToString(data));
	}

	/**
	 * 将每帧进行分包并发送数据  性能比较低
	 * @param bytes
	 */
//	private void sendData(byte[] bytes) {
//		int seqNumber = 0;
//		int dataLength = (bytes.length - 1) / 1480 + 1;
//		final byte[][] data = new byte[dataLength][];
//		final boolean[] marks = new boolean[dataLength];
//		marks[marks.length - 1] = true;
//		long[] seqNumbers = new long[dataLength];
//		for (int i = 0;i < dataLength;i++){
//			seqNumbers[i] = seqNumber;
//			try{
//				seqNumber++;
//			}catch (Throwable t){
//				seqNumber = 0;
//			}
//		}
//		int num = 0;
//		do{
//			int length = bytes.length > 1480 ? 1480 : bytes.length;
//			data[num] = Arrays.copyOf(bytes,length);
//			num++;
//			byte[] b = new byte[bytes.length - length];
//			for(int i = length; i < bytes.length; i++){
//				b[i - length] = bytes[i];
//			}
//			bytes = b;
//		} while (bytes.length > 0);
//
//		session.rtpSession.sendData(data, null, marks, System.currentTimeMillis(), null);
//	}

}
