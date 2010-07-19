package org.one.stone.soup.swing;

import java.awt.Graphics;

/*
 * Wet-Wired.com Library Version 2.1
 *
 * Copyright 2000-2001 by Wet-Wired.com Ltd.,
 * Portsmouth England
 * This software is OSI Certified Open Source Software
 * This software is covered by an OSI approved open source licence
 * which can be found at http://www.onestonesoup.org/OSSLicense.html
 */
/**
 * @author nikcross
 *
 */
public class WetWiredJFrame extends javax.swing.JFrame {

  public WetWiredJFrame() {
    super();
    //setIconImage(ImageFactory.loadImage("jar://resources/dog.png"));
  }

  public WetWiredJFrame(String title) {
    setTitle(title);
    //setIconImage(ImageFactory.loadImage("jar://resources/dog.png"));
  }

  public void update(Graphics grfx) {
    paint(grfx);
  }
}
