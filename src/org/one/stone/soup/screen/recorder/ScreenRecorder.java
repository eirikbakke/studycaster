/*
 * Created on 24-Jan-05
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.one.stone.soup.screen.recorder;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import org.one.stone.soup.util.Queue;

public abstract class ScreenRecorder implements Runnable{

	private Rectangle recordArea;
	
	private int frameSize;
	private int[] rawData;
	
	private OutputStream oStream;
	
	private boolean recording = false;
	private boolean running = false;

	private long startTime;
	private long frameTime;
	private boolean reset;
	
	private ScreenRecorderListener listener;
	
	private class DataPack
	{
		public DataPack(int[] newData,long frameTime)
		{
			this.newData = newData;
			this.frameTime = frameTime;
		}
		public long frameTime;
		public int[] newData;
	}
	
	private class StreamPacker implements Runnable
	{	
		private Queue queue = new Queue();
		private FrameCompressor compressor;
		
		public StreamPacker( OutputStream oStream,int frameSize )
		{
			compressor = new FrameCompressor( oStream,frameSize );
			
			new Thread(this,"Stream Packer").start();
		}
		
		public void packToStream( DataPack pack )
		{
			while(queue.size()>2)
			{
				try{ Thread.sleep(10); }catch(Exception e){}
			}
			queue.post( pack );
		}
		
		public void run()
		{
			while(recording)
			{
				while( queue.isEmpty()==false )
				{
					DataPack pack = (DataPack)queue.get();
					try{
						compressor.pack( pack.newData,pack.frameTime,reset );
						
						if(reset==true)
						{
							reset=false;
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
						try{ oStream.close(); }catch(Exception e2){}
						return;
					}
				}
				while(queue.isEmpty()==true)
				{
					try{Thread.sleep(50);}catch(Exception e){}
				}
			}
		}
	}
	private StreamPacker streamPacker;
	
	public ScreenRecorder(OutputStream oStream,ScreenRecorderListener listener)
	{
		
		this.listener = listener;
		this.oStream = oStream;		
	}

	public void triggerRecordingStop()
	{
		recording = false;
	}
	
	public synchronized void run()
	{
		startTime = System.currentTimeMillis();
		
		recording = true;
		running = true;
		long lastFrameTime=0;
		long time = 0;
		
		frameSize = recordArea.width*recordArea.height;
		streamPacker = new StreamPacker( oStream,frameSize );
		
		while( recording )
		{
			time = System.currentTimeMillis();
			while(time-lastFrameTime<200)
			{
				try{Thread.sleep(50);}catch(Exception e){}
				time = System.currentTimeMillis();	
			}
			lastFrameTime = time;
			
			try{
				recordFrame();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				try{ oStream.close(); }catch(Exception e2){}
				break;
			}
		}
		
		running = false;
		recording = false;
		
		listener.recordingStopped();
	}

	public abstract Rectangle initialiseScreenCapture();	
	public abstract BufferedImage captureScreen(Rectangle recordArea);
	
	public void recordFrame() throws IOException
	{
		//long t1 = System.currentTimeMillis();
		BufferedImage bImage = captureScreen(recordArea);
		frameTime = System.currentTimeMillis()-startTime;
		//long t2 = System.currentTimeMillis();

		rawData = new int[frameSize];

		bImage.getRGB(0,0,recordArea.width,recordArea.height,rawData,0,recordArea.width);
		//long t3 = System.currentTimeMillis();
		
		//packToStream(rawData,newRawData);
		streamPacker.packToStream( new DataPack(rawData,frameTime) );
		//long t4 = System.currentTimeMillis();
		
		/*System.out.println("Times");
		System.out.println("  capture time:"+(t2-t1));
		System.out.println("  data grab time:"+(t3-t2));
		System.out.println("  pack time:"+(t4-t3));*/
		
		listener.frameRecorded(false);
	}

	public void startRecording()
	{		
		recordArea = initialiseScreenCapture();
		
		if(recordArea==null)
		{
			return;
		}
		try{
			oStream.write( (recordArea.width & 0x0000FF00) >>>8 );
			oStream.write( (recordArea.width & 0x000000FF) );
		
			oStream.write( (recordArea.height & 0x0000FF00) >>>8 );
			oStream.write( (recordArea.height & 0x000000FF) );
		}
		catch(Exception e)
		{
			e.printStackTrace();		
		}		
		
		new Thread(this,"Screen Recorder").start();
	}

	public void stopRecording()
	{
		triggerRecordingStop();
		
		int count = 0;
		while(running == true && count<10)
		{
			try{ Thread.sleep(100);
			}catch(Exception e){}
			count++;
		}
		
		try{
			oStream.flush();
			oStream.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public Rectangle getRecordArea() {
		return recordArea;
	}
	
	public boolean isRecording()
	{
		return recording;
	}
	
	public void sendKeyFrame()
	{
		reset=true;
	}

	public int getFrameSize() {
		return frameSize;
	}
}
