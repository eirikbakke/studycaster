package org.one.stone.soup.screen.recorder;

import java.io.IOException;

public interface ScreenRecorderListener {

	public void frameRecorded(boolean fullFrame) throws IOException;
	public void recordingStopped();
}
