package org.one.stone.soup.util;

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
// TODO
public class Queue<E> extends java.util.Vector<E> {
  private static final long serialVersionUID = -5844905201439323287L;

  /**
   * Queue constructor comment.
   */
  public Queue() {
    super();
  }

  /**
   * Queue constructor comment.
   * @param initialCapacity int
   */
  public Queue(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Queue constructor comment.
   * @param initialCapacity int
   * @param capacityIncrement int
   */
  public Queue(int initialCapacity, int capacityIncrement) {
    super(initialCapacity, capacityIncrement);
  }

  /**
   *
   * @param data java.lang.Object
   */
  public E get() {
    if (isEmpty()) {
      return null;
    }
    E data = elementAt(0);
    removeElementAt(0);
    return data;
  }

  /**
   *
   * @param data java.lang.Object
   */
  public void post(E data) {
    addElement(data);
  }
}
