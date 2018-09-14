package com.example.rtpdemo;

import android.os.Environment;

import com.socks.library.KLog;

import java.net.DatagramSocket;
import java.net.SocketException;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;

public class InitSession implements RTPAppIntf {
	
	public RTPSession rtpSession = null;
	public DataFullCallBack dataFullCallBack;
	
	public InitSession(int rtpPort, int rtcpPort, int prtpPort, int prtcpPort, String paddress , DataFullCallBack dataFullCallBack){
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;

		this.dataFullCallBack = dataFullCallBack;
		
		try {
			rtpSocket = new DatagramSocket(rtpPort);
			rtcpSocket = new DatagramSocket(rtcpPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
//		rtpSession.naivePktReception(true);
		rtpSession.RTPSessionRegister(this, null, null);
		Participant p = new Participant(paddress, prtpPort, prtcpPort);
		rtpSession.addParticipant(p);
	}


	byte[] buf;
	int i =0;

	@Override
	public void receiveData(DataFrame frame, Participant participant) {
		// TODO Auto-generated method stub
		KLog.i("标志位=" + frame.marked());

		KLog.i("当前线程 " + Thread.currentThread());
		KLog.i("sequenceNumbers = " + frame.sequenceNumbers()[frame.sequenceNumbers().length -1]);

		if (buf == null){
			KLog.i("buf");
			buf = frame.getConcatenatedData();
		} else {
			KLog.i("buf != null");

			buf = com.example.rtpdemo.Util.merge(buf, frame.getConcatenatedData());

		}
		KLog.i(buf.length);

		// 如果该包标记位为 true，说明为一帧的最后一包，开始进行解码操作
		if (frame.marked()){
			ImageDispose.getFileFromBytes(buf, Environment.getExternalStorageDirectory() + "/3");
			KLog.i(" buf data len =" + buf.length);
			dataFullCallBack.dataFullCB(buf);
			buf = null;
		}

//		String s = new String(recData);
//		KLog.i("received:"+s + " from:"+participant.getCNAME()+" ssrc:"+participant.getSSRC());



	}

	@Override
	public void userEvent(int type, Participant[] participant) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int frameSize(int payloadType) {
		// TODO Auto-generated method stub
		return 1;
	}

}
