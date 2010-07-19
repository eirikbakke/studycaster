package org.one.stone.soup.screen.player.application;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.one.stone.soup.screen.recorder.ScreenPlayer;
import org.one.stone.soup.screen.recorder.ScreenPlayerListener;
import org.one.stone.soup.stringhelper.StringGenerator;
import org.one.stone.soup.swing.JRootFrame;

/**
 * @author A364061
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JPlayer extends JRootFrame implements ScreenPlayerListener, ActionListener {

  private ScreenPlayer player;
  private ImageIcon icon;
  private JButton play;
  private JButton pause;
  private JButton fastForward;
  private JButton stop;
  private JLabel text;
  private JLabel frameLabel;
  private String target;
  private int frameCount;
  private long startTime;

  public JPlayer() {
    super("Screen Player", new String[]{});

    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(1, 4));
    Color green = new Color(0, 200, 0);
    Color blue = new Color(0, 0, 200);
    Color red = new Color(200, 0, 0);

    play = new JButton("Open Recording");
    play.setActionCommand("open");
    play.setForeground(green);
    play.addActionListener(this);

    pause = new JButton("Pause");
    pause.setActionCommand("pause");
    pause.setForeground(blue);
    pause.setEnabled(false);
    pause.addActionListener(this);

    fastForward = new JButton("Fast Forward");
    fastForward.setForeground(blue);
    fastForward.setActionCommand("fastForward");
    fastForward.setEnabled(false);
    fastForward.addActionListener(this);

    stop = new JButton("Stop");
    stop.setActionCommand("stop");
    stop.setForeground(red);
    stop.setEnabled(false);
    stop.addActionListener(this);

    panel.add(play);
    panel.add(pause);
    panel.add(fastForward);
    panel.add(stop);
    panel.doLayout();

    this.getContentPane().add(panel, BorderLayout.NORTH);

    panel = new JPanel();
    panel.setLayout(new GridLayout(1, 2));
    panel.setBackground(Color.black);

    frameLabel = new JLabel("Frame: 0 Time: 0");
    frameLabel.setBackground(Color.black);
    frameLabel.setForeground(Color.red);
    text = new JLabel("No recording selected");
    text.setBackground(Color.black);
    text.setForeground(Color.red);

    panel.add(text);
    panel.add(frameLabel);

    this.getContentPane().add(panel, BorderLayout.SOUTH);

    this.pack();
    this.setVisible(true);
  }

  /* (non-Javadoc)
   * @see wet.wired.swing.JRootFrame#destroy()
   */
  public boolean destroy(Object source) {
    return true;
  }

  public void actionPerformed(ActionEvent ev) {
    if (ev.getActionCommand().equals("open")) {
      JFileChooser chooser = new JFileChooser();
      if (target != null) {
        chooser.setSelectedFile(new File(target));
      }
      chooser.showOpenDialog(this);
      target = chooser.getSelectedFile().getAbsolutePath();

      open();
    } else if (ev.getActionCommand().equals("play")) {
      play();
    } else if (ev.getActionCommand().equals("fastForward")) {
      fastForward();
    } else if (ev.getActionCommand().equals("pause")) {
      pause();
    } else if (ev.getActionCommand().equals("stop")) {
      stop();
    }
  }

  public void playerStopped() {
    play.setEnabled(true);
    play.setText("Open Recording");
    play.setActionCommand("open");

    pause.setEnabled(false);
    fastForward.setEnabled(false);
    stop.setEnabled(false);

    text.setText("No recording selected");

    player = null;
  }

  public void showNewImage(Image image) {
    if (icon == null) {
      icon = new ImageIcon(image);
      JLabel label = new JLabel(icon);

      JScrollPane scrollPane = new JScrollPane(label);
      scrollPane.setSize(image.getWidth(this), image.getHeight(this));

      this.getContentPane().add(scrollPane, BorderLayout.CENTER);

      pack();
      setVisible(true);
    } else {
      icon.setImage(image);
    }

    repaint(0);
  }

  public void newFrame() {
//		if(frameCount==0)
//		{
//			player.pause();
//		}

    frameCount++;
    long time = System.currentTimeMillis() - startTime;
    String seconds = "" + time / 1000;
    String milliseconds = "" + time % 1000;
    milliseconds = StringGenerator.pad(milliseconds, 4, '0') + milliseconds;
    frameLabel.setText("Frame:" + frameCount + " Time:" + seconds + "." + milliseconds);
  }

  public static void main(String[] args) {
    JPlayer viewer = new JPlayer();
    if (args.length == 1) {
      viewer.target = new File(args[1]).getAbsolutePath();
      viewer.play();
    }
  }

  public boolean open() {
    if (target != null) {
      try {
        FileInputStream iStream = new FileInputStream(target);
        player = new ScreenPlayer(iStream, this);
        frameCount = 0;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }

    play.setActionCommand("play");
    play.setText("Play");

    text.setText("Ready to play " + new File(target).getName());

    return true;
  }

  public void play() {
    player.play();
    startTime = System.currentTimeMillis();

    play.setEnabled(false);
    pause.setEnabled(true);
    fastForward.setEnabled(true);
    stop.setEnabled(true);

    text.setText("Playing " + target);
  }

  public void fastForward() {
    play.setEnabled(true);
    pause.setEnabled(true);
    fastForward.setEnabled(false);

    player.fastforward();

    text.setText("Fast Forward " + target);
  }

  public void stop() {
    player.stop();

    pause.setEnabled(false);
    fastForward.setEnabled(false);
    stop.setEnabled(false);

    text.setText("No recording selected");
  }

  public void pause() {
    play.setEnabled(true);
    pause.setEnabled(false);
    fastForward.setEnabled(true);

    player.pause();

    text.setText("Paused " + target);
  }
}

