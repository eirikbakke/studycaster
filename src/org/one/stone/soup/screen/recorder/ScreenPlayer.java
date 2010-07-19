/*
 * Created on 24-Jan-05
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.one.stone.soup.screen.recorder;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.io.InputStream;

public class ScreenPlayer implements Runnable{

	private ScreenPlayerListener listener;

	private MemoryImageSource mis = null;
	private Rectangle area;

	private FrameDecompressor decompressor;
	
	private long startTime;
	private long frameTime;
	
	private boolean running;
	private boolean paused;
	private boolean fastForward;
	private boolean realtime = false;

	public ScreenPlayer(InputStream iStream,ScreenPlayerListener listener)
	{
		this.listener = listener;
		
		try{
			int width = iStream.read();
			width = width << 8;
			width += iStream.read();
			
			int height = iStream.read();
			height = height << 8;
			height += iStream.read();
			
			area = new Rectangle(width,height);
			
			decompressor = new FrameDecompressor(iStream,width*height);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void play()
	{
		fastForward=false;
		paused=false;
		
		if(running==false)
		{
			new Thread(this,"Screen Player").start();
		}
	}
	
	public void pause()
	{
		if(realtime==false)
		{
			paused=true;
		}
	}
	
	public void stop()
	{
		paused=false;
		running=false;
	}
	
	public void fastforward()
	{
		fastForward=true;
		paused=false;
	}
	
	public synchronized void run()
	{
		startTime = System.currentTimeMillis();
		long lastFrameTime = 0;
		
		running=true;
		while(running==true)
		{
			while(paused==true && realtime==false)
			{
				try{ Thread.sleep(50); }catch(Exception e){}					
				startTime += 50;
			}
			
			try{
				readFrame();
				listener.newFrame();
			}
			catch(IOException ioe)
			{
				//ioe.printStackTrace();
				listener.showNewImage(null);
				break;
			}
			
			if(fastForward==true)
			{
				startTime -= (frameTime-lastFrameTime);
			}
			else
			{
				while( (System.currentTimeMillis()-startTime<frameTime && realtime==false) && running )
				{
					try{ Thread.sleep(100); }catch(Exception e){}
				}
				
				//System.out.println( "FrameTime:"+frameTime+">"+(System.currentTimeMillis()-startTime));
			}
			
			lastFrameTime=frameTime;
		}
		running = false;
		
		listener.playerStopped();
	}
	
	private void readFrame() throws IOException
	{
		FrameDecompressor.FramePacket frame = decompressor.unpack();
		
		frameTime = frame.getTimeStamp();
		
		int result = frame.getResult();
		if( result==0 )
		{
			return;
		}
		else if( result==-1 )
		{
			running=false;
			return;
		}
		
		if(mis==null)
		{
			mis = new MemoryImageSource(area.width,area.height,frame.getData(),0,area.width);
			mis.setAnimated(true);
			listener.showNewImage( Toolkit.getDefaultToolkit().createImage(mis) );
			return;
		}
		else
		{
			mis.newPixels(frame.getData(),ColorModel.getRGBdefault(),0,area.width);
			return;
		}		
	}

	public boolean isRealtime() {
		return realtime;
	}

	public void setRealtime(boolean realtime) {
		this.realtime = realtime;
	}
}
