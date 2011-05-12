package com.flugtag.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
	
	/**
	 * Copies an InputStream to an OutputStream and closes both streams
	 * 
	 * @param is The source InputStream
	 * @param os The destination OutputStream
	 * 
	 * @throws IOException
	 */
	public static void copyFile(InputStream is, OutputStream os) throws IOException {
	    try {
	        byte[] buf = new byte[1024];
	        int i = 0;
	        while ((i = is.read(buf)) != -1) {
	            os.write(buf, 0, i);
	        }
	    } 
	    finally {
	        if (is != null) is.close();
	        if (os != null) os.close();
	    }
	  }
}