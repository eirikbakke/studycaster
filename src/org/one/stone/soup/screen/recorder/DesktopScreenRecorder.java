package org.one.stone.soup.screen.recorder;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

public class DesktopScreenRecorder extends ScreenRecorder{

	private Robot robot;
	
	public DesktopScreenRecorder(OutputStream oStream,ScreenRecorderListener listener)
	{
		super(oStream,listener);
	}

	public Rectangle initialiseScreenCapture()
	{		
		try{
			robot = new Robot();
		}
		catch(AWTException awe)
		{
			awe.printStackTrace();
			return null;
		}
		return new Rectangle( Toolkit.getDefaultToolkit ().getScreenSize() );

	}
	
	public Robot getRobot()
	{
		return robot;
	}

	public BufferedImage captureScreen(Rectangle recordArea)
	{
		BufferedImage image = robot.createScreenCapture(recordArea);
		
		Polygon pointer = new Polygon(new int[]{0,16,10,8},new int[]{0,8,10,16},4);
		Polygon pointerShadow = new Polygon(new int[]{6,21,16,14},new int[]{1,9,11,17},4);
		
    // On Windows XP, getPointerInfo() may return null for instance when the Windows Security screen is shown (Ctrl+Alt+Del).
    PointerInfo pinf = MouseInfo.getPointerInfo();
    if (pinf != null) {
      Point mousePosition = pinf.getLocation();
      Graphics2D grfx = image.createGraphics();
      grfx.translate(mousePosition.x,mousePosition.y);
      grfx.setColor( new Color(100,100,100,100) );
      grfx.fillPolygon( pointerShadow );
      grfx.setColor( new Color(100,100,255,200) );
      grfx.fillPolygon( pointer );
      grfx.setColor( Color.red );
      grfx.drawPolygon( pointer );
      grfx.dispose();
    }
		return image;
	}	
}
