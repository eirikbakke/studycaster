package org.one.stone.soup.swing;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.WindowConstants;

/*
 * Wet-Wired.com Library Version 2.1
 *
 * Copyright 2000-2001 by Wet-Wired.com Ltd.,
 * Portsmouth England
 * This software is OSI Certified Open Source Software
 * This software is covered by an OSI approved open source licence
 * which can be found at http://www.wet-wired.co.uk/pages/licence-os.html
 */
/**
 * @author nikcross
 *
 */
public abstract class JRootFrame extends WetWiredJFrame implements WindowListener, KeyListener, MouseListener, MouseMotionListener {
  public static boolean DEBUG = false;
  public boolean printScreen = false;
  public static final int PLAF_METAL = 0;
  public static final int PLAF_WINDOWS = 1;
  public static final int PLAF_MOTIF = 2;

  public JRootFrame(String title, String args[]) {
    super(title);

    readSwitches(args);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(this);
    addKeyListener(this);
    getContentPane().addKeyListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public JRootFrame(String title, String iconFile, String args[]) {
    super(title);

    readSwitches(args);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(this);
    addKeyListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  /**
   *
   */
  public abstract boolean destroy(Object source);

  public void keyPressed(KeyEvent e) {

    if (e.isControlDown()) {
      if (e.getKeyCode() == 69) {
        destroy(this);
        System.exit(0);
      }
    }

    if (e.getKeyCode() == KeyEvent.VK_PRINTSCREEN) {
      printScreen = true;
    }
  }

  /**
   * keyReleased method comment.
   */
  public void keyReleased(KeyEvent e) {
  }

  /**
   * keyTyped method comment.
   */
  public void keyTyped(KeyEvent e) {
  }

  /**
   * mouseClicked method comment.
   */
  public void mouseClicked(MouseEvent e) {
  }

  /**
   * mouseDragged method comment.
   */
  public void mouseDragged(MouseEvent e) {
  }

  /**
   * mouseEntered method comment.
   */
  public void mouseEntered(MouseEvent e) {
  }

  /**
   * mouseExited method comment.
   */
  public void mouseExited(MouseEvent e) {
  }

  /**
   * mouseMoved method comment.
   */
  public void mouseMoved(MouseEvent e) {
  }

  /**
   * mousePressed method comment.
   */
  public void mousePressed(MouseEvent e) {
  }

  /**
   * mouseReleased method comment.
   */
  public void mouseReleased(MouseEvent e) {
  }

  private void readSwitches(String[] args) {

    if (args == null) {
      return;
    }

    for (int loop = 0; loop < args.length; loop++) {
      if (args[loop].equals("-d")) {
        DEBUG = true;
      }
    }
  }

  /**
   * Insert the method's description here.
   * Creation date: (26/01/04 07:21:17)
   * @param plafId int
   */
  public void setPlaf(int plaf) {
    String laf = null;

    if (plaf == PLAF_METAL) {
      laf = "javax.swing.plaf.metal.MetalLookAndFeel";
    } else if (plaf == PLAF_WINDOWS) {
      laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    } else if (plaf == PLAF_MOTIF) {
      laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
    }
    try {
      javax.swing.UIManager.setLookAndFeel(laf);
    } catch (Exception e2) {
      System.out.println(e2);
    }
  }

  /**
   * windowActivated method comment.
   */
  public void windowActivated(WindowEvent e) {
  }

  /**
   * windowClosed method comment.
   */
  public void windowClosed(WindowEvent e) {
  }

  /**
   * windowClosing method comment.
   */
  public void windowClosing(WindowEvent e) {

    if (destroy(e.getSource()) == false) {
      return;
    }

//	destroy();
    System.exit(0);
  }

  /**
   * windowDeactivated method comment.
   */
  public void windowDeactivated(WindowEvent e) {
  }

  /**
   * windowDeiconified method comment.
   */
  public void windowDeiconified(WindowEvent e) {
  }

  /**
   * windowIconified method comment.
   */
  public void windowIconified(WindowEvent e) {
  }

  /**
   * windowOpened method comment.
   */
  public void windowOpened(WindowEvent e) {
  }
}
