package org.one.stone.soup.stringhelper;

import java.io.*;
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
public class FileNameHelper {
/**
 *
 * @return java.lang.String
 * @param fileName java.lang.String
 */
public static String getExt(String fileName) {
	fileName = new File(fileName).getName();
	String ext = "";
	if(fileName.lastIndexOf(".")>-1)
	{
		ext = fileName.substring(fileName.lastIndexOf(".")+1);
	}
	return ext;
}
}
