package org.one.stone.soup.screen.recorder;

import java.awt.Image;

public interface ScreenPlayerListener {

	public void showNewImage(Image image);
	public void playerStopped();
	public void newFrame();

}
