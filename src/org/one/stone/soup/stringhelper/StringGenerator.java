package org.one.stone.soup.stringhelper;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

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
// TODO
public class StringGenerator {
  private static final String[] constanants = new String[]{
    "b",
    "bl",
    "br",
    "by",
    "c",
    "ck",
    "cl",
    "cr",
    "d",
    "dr",
    "dy",
    "f",
    "fl",
    "fr",
    "g",
    "gl",
    "gr",
    "h",
    "j",
    "l",
    "m",
    "n",
    "p",
    "pr",
    "qu",
    "r",
    "s",
    "st",
    "str",
    "v",
    "vr",
    "w",
    "wr"
  };
  private static final String[] vowels = new String[]{
    "a",
    "ai",
    "au",
    "e",
    "ea",
    "ee",
    "eu",
    "i",
    "ie",
    "io",
    "iu",
    "o",
    "oa",
    "oe",
    "oi",
    "ou",
    "u",
    "ue",
    "ui"
  };
  private static boolean initialized = false;
  private static int idSet = 0;
  private static int idCount = 0;

  public static String asDec(byte[] data) {

    StringBuffer b = new StringBuffer();

    for (int loop = 0; loop < data.length; loop++) {
      b.append(data[loop] + " ");
    }

    return b.toString();
  }

  public static String asDec(String data) {

    StringBuffer b = new StringBuffer();

    for (int loop = 0; loop < data.length(); loop++) {
      b.append(data.charAt(loop) + "[" + (int) data.charAt(loop) + "]");
    }

    return b.toString();
  }

  public static String asHex(byte[] data) {

    StringBuffer b = new StringBuffer();

    for (int loop = 0; loop < data.length; loop++) {
      String txt = Integer.toHexString(data[loop]);
      if (txt.length() > 2) {
        txt = txt.substring(txt.length() - 2);
      }
      b.append("0x" + txt + " ");
    }

    return b.toString();
  }

  public static String asTime(long millis) {
    String text;

    if (millis < 1000) {
      text = millis + " milliseconds";
    } else if (millis < 60000) {
      text = (millis / 1000) + " seconds";
    } else {
      text = (millis / 360000) + " minutes " + ((millis % 360000) / 1000) + " seconds";
    }

    return text;
  }

  public static String convertHtmlToString(String data) {

    StringBuffer buffer = new StringBuffer();

    char c;

    for (int loop = 0; loop < data.length(); loop++) {
      c = data.charAt(loop);

      if (c == '&') {
        loop++;
        c = data.charAt(loop);
        String name = "";
        loop++;
        while (loop < data.length() && c != ';') {
          name = name + c;
          c = data.charAt(loop);
          loop++;
        }

        if (name.equals("lt")) {
          buffer.append("<");
        } else if (name.equals("gt")) {
          buffer.append(">");
        } else if (name.equals("tab")) {
          buffer.append("\t");
        } else if (name.equals("amp")) {
          buffer.append("&");
        } else if (name.equals("quote")) {
          buffer.append("\"");
        } else if (name.equals("pound")) {
          buffer.append("�");
        } else {
          buffer.append("&" + name + ";");
        }

        loop--;
      } else if (c == '<') {
        loop++;
        c = data.charAt(loop);
        String name = "";
        loop++;
        while (loop < data.length() && c != '>') {
          name = name + c;
          c = data.charAt(loop);
          loop++;
        }
        if (name.toLowerCase().equals("br/")) {
          buffer.append("\n");
        } else {
          buffer.append("<" + name + ">");
        }

        loop--;
      } else {
        buffer.append(c);
      }
    }

    return buffer.toString();
  }

  public static String convertToHtmlString(String data) {
    StringBuffer newData = new StringBuffer();

    for (int loop = 0; loop < data.length(); loop++) {
      /*	if(data.charAt(loop)=='[')
      {
      while(data.charAt(loop)!=']' && loop<data.length())
      {
      newData.append( data.charAt(loop) );
      loop++;
      }
      newData.append( data.charAt(loop) );

      continue;
      }*/

      switch (data.charAt(loop)) {
        case '\n':
          newData.append("<BR/>");
          break;
        case '\t':
          newData.append("&tab;");
//		newData.append("    ");
          break;
        case '"':
          newData.append("&quote;");
          break;
        case '&':
          newData.append("&amp;");
          break;
        case '<':
          newData.append("&lt;");
          break;
        case '>':
          newData.append("&gt;");
          break;
        default:
          newData.append(data.charAt(loop));
      }
    }

    return newData.toString();
  }

  public static String convertToJavaCodeString(String data) {
    StringBuffer newData = new StringBuffer();

    for (int loop = 0; loop < data.length(); loop++) {
      switch (data.charAt(loop)) {
        case '\t':
          newData.append("\\t");
          break;
        case '\n':
          newData.append("\\n");
          break;
        case '\r':
          newData.append("\\r");
          break;
        case '\"':
          newData.append("\\\"");
          break;
        case '\\':
          newData.append("\\\\");
          break;
        default:
          newData.append(data.charAt(loop));
      }
    }

    return newData.toString();
  }

  public static String generatePad(int length, char padChar) {

    if (length < 0) {
      return "";
    }

    StringBuffer buffer = new StringBuffer();

    while (length > 0) {
      buffer.append(padChar);
      length--;
    }

    if (buffer == null) {
      return "";
    }

    return buffer.toString();
  }

  public static String generatePassword() {

    StringBuffer password = new StringBuffer();

    for (int loop = 0; loop < 3; loop++) {
      int c = (int) (Math.random() * constanants.length);
      int v = (int) (Math.random() * vowels.length);

      password.append(constanants[c]);
      password.append(vowels[v]);
    }

    return password.toString();
  }

  public static String generateStringFromFile(String fileName) {
    try {
      InputStream in = new FileInputStream(fileName);

      return generateStringFromInputStream(in);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String generateStringFromInputStream(InputStream in) {
    StringBuffer data = new StringBuffer();

    try {
      int inC = in.read();

      while (inC != -1) {
        data.append((char) inC);

        inC = in.read();
      }

      in.close();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    return data.toString();
  }

  public static String generateStringWithMask(String template, Object[] data) {

    String msg = "";
    int count = 0;

    for (int loop = 0; loop < template.length(); loop++) {
      if (template.charAt(loop) == '#') {
        if (count < data.length) {
          if (data[count] == null) {
            msg += "[Null]";
          } else {
            msg += data[count];
          }
        } else {
          msg += "[EndOfRecord]";
        }
        count++;
      } else {
        msg += template.charAt(loop);
      }
    }

    return msg;
  }

  public static String generateStringWithMask(String template, Hashtable data) {
    return generateStringWithMask(template, data, '&', ';');
  }

  public static String generateStringWithMask(String template, Hashtable data, char start, char end) {

    StringBuffer result = new StringBuffer();

    for (int loop = 0; loop < template.length(); loop++) {
      if (template.charAt(loop) == start) {
        if (template.charAt(loop + 1) == start) // handle ##
        {
          result.append(start);
          loop++;
          continue;
        }

        loop++;
        StringBuffer name = new StringBuffer();
        while (loop < template.length() && template.charAt(loop) != end && template.charAt(loop) != start) {
          name.append(template.charAt(loop));
          loop++;
        }

        String value = (String) data.get(name.toString());
        if (loop < template.length() && template.charAt(loop) == start) {
          result.append("&" + name.toString());
          loop--;
        } else if (value == null) {
          //throw new NullPointerException("Value for key "+name.toString()+" not found.");
          result.append(start);
          result.append(name.toString());
          if (loop < template.length()) {
            result.append(end);
          }
        } else {
          result.append(value);
        }
      } else {
        result.append(template.charAt(loop));
      }
    }

    return result.toString();
  }

  public static String generateUniqueId() {

    if (!initialized) {
      idCount = (int) (Math.random() * Integer.MAX_VALUE);
      initialized = true;
    }

    idCount++;

    if (idCount == Integer.MAX_VALUE) {
      idCount = 0;
      idSet++;
    }

    return "" + idCount + "." + idSet;
  }

  public static String pad(String source, int size, char padChar) {

    String pad = generatePad(size - source.length(), padChar);

    return pad;
  }

  public static String replace(String source, String find, String replace) {

    StringBuffer result = new StringBuffer();
    StringBuffer holding = new StringBuffer();

    for (int loop = 0; loop < source.length(); loop++) {
      if (source.charAt(loop) == find.charAt(0)) {
        int offset = 0;
        while (loop < source.length() && offset < find.length() && find.charAt(offset) == source.charAt(loop + offset)) {
          holding.append(source.charAt(loop + offset));
          offset++;
        }
        if (offset == find.length()) {
          loop += find.length() - 1;
          result.append(replace);
        } else {
          result.append(source.charAt(loop));
        }
        holding = new StringBuffer();
      } else {
        result.append(source.charAt(loop));
      }
    }

    return result.toString();
  }

  public static String truncate(String source, int size, String postFix) {

    if (source.length() > size) {
      source = source.substring(0, size - postFix.length()) + postFix;
    }

    return source;
  }
}
