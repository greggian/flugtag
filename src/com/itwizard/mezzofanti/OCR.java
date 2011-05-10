/*
 * Copyright (C) 2009 IT Wizard.
 * http://www.itwizard.ro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itwizard.mezzofanti;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;

/**
 * The wrapper for the JNI-OCR library
 */
public final class OCR 
{
	public static OCRConfig mConfig = new OCRConfig();	

	private static OCR m_OCRinstance = null;		// there is only one ocr instance, publicly access it with get()
	private static final String TAG = "MLOG: OCR.java: ";

	// Java-lib variables
	public final static int PSM_AUTO = 0;           // Fully automatic page segmentation. (Default.)
	public final static int PSM_SINGLE_COLUMN = 1;  // Assume a single column of text of variable sizes.
	public final static int PSM_SINGLE_BLOCK = 2;   // Assume a single uniform block of text.
	public final static int PSM_SINGLE_LINE = 3;    // Treat the image as a single text line.
	public final static int PSM_SINGLE_WORD = 4;    // Treat the image as a single word.
	public final static int PSM_SINGLE_CHAR = 5;    // Treat the image as a single character.
	public final static int PSM_COUNT = 6;          // Number of enum entries.

	public static Word[] m_asWords = null;				// vector to keep all info about each word in the results
	public static SpannableString m_ssOCRResult = null;	// the parsed result (all errors are changed to '#', all non-dictionary words become 'italic')
	public static int m_iMeanConfidence = 0;			// the mean overall confidence

	private boolean m_bLineMode = false;
	public boolean m_bIsLibraryInitialized = false;	// is the library initialized

	/**
	 * Class to keep info about each OCRed word.
	 */
	public final class Word
	{
		String m_sBody;			// the word body
		int m_iConfidence;		// the ocr confidence for the word
		boolean m_bIsValidWord;	// is a valid dictionary word
		boolean m_bNewLine;		// hold an endline after it
		int m_iEnd;				// the last position in the overall result

		/**
		 * constructor
		 */
		public Word()
		{
			m_sBody = "";
			m_iConfidence = 0;
			m_bIsValidWord = false;
			m_bNewLine = false;
			m_iEnd = 0;
		}

		/**
		 * 2nd constructor
		 * @param body the word-body
		 * @param conf the confidence
		 */
		public Word(String body, int conf)
		{		
			if (!mConfig.m_bReplaceUnknownChars)
				m_sBody = body;
			else
			{
				m_sBody = body.replaceAll("[\n\r]+", "");				
				m_sBody = m_sBody.replaceAll("[^A-Za-z0-9!?.,%$@&*(){}<>:;'\"-]+", "#");
			}
			m_iConfidence = conf;

			if (m_sBody.contains("#"))
				m_bIsValidWord = false;
			else m_bIsValidWord = MyIsValidDictionaryWord(body);

			m_bNewLine = body.contains("\n");
			Log.v(TAG, "Word("+m_sBody+","+conf+")");
		}		
	} // end class Word

	// -------------------------------------------------------------------

	/**
	 * stores the OCR configuration, load/store the config
	 */
	public final static class OCRConfig
	{
		public int m_iMinOveralConfidence;		// the overall confidence for all decoded text
		public int m_iMinWordConfidence;		// the confidence per each word
		public int m_iPSMMode = PSM_AUTO; 		// the OCR scanning mode
		public boolean m_bUseBWFilter;			// enable or not the BW filter
		public boolean m_bShowValidWordsOnly;	// if enabled, we will display only the dictionary-valid words after OCR
		public boolean m_bReplaceUnknownChars;	// replace strange characters with '#'

		private byte m_bImgDivisor = 2;			// the image divisor factor
		private String m_sLanguage = "eng";		// current language
		public String m_asLanguages[] = null; 	// all available languages

		/**
		 * constructor
		 */
		public OCRConfig()
		{
			LoadFabricDefaults();			
		}

		/**
		 * load fabric default values for the config
		 */
		public void LoadFabricDefaults()
		{
			m_iMinOveralConfidence = 60;
			m_iMinWordConfidence = 50;
			m_iPSMMode = OCR.PSM_AUTO;
			SetImgDivisor(2);
			m_bUseBWFilter = false;
			m_bShowValidWordsOnly = false;
			m_bReplaceUnknownChars = true;
			m_sLanguage = "eng";
			Log.v(TAG, "LoadFabricDefaults(): " + m_sLanguage);
		}

		/**
		 * get setings from the shared preferences
		 * @param prefs the shared preferences
		 * @return success/fail
		 */
		public boolean GetSettings(SharedPreferences prefs)
		{
			Log.v(TAG, "GetSettings() -----------------------------------------------");

			try
			{
                OCR.mConfig.m_iMinOveralConfidence = Integer.parseInt("60");
                OCR.mConfig.m_iMinWordConfidence = Integer.parseInt("50");
                OCR.mConfig.SetImgDivisor(Integer.parseInt("2"));
                OCR.mConfig.m_bUseBWFilter = false;
                OCR.mConfig.m_sLanguage = "eng";
			}
			catch (Exception ex)
			{
				Log.v(TAG, "exception: GetSettings()" + ex.toString());
				return false;
			}
			
			Log.v(TAG, "GetSettings: [" + 
					OCR.mConfig.m_iMinOveralConfidence +","+ 
					OCR.mConfig.m_iMinWordConfidence +","+ 
					OCR.mConfig.GetImgDivisor() +","+
					OCR.mConfig.m_bUseBWFilter +","+
					OCR.mConfig.m_sLanguage  +"]"
					);
			
			return true;
		}

		/**
		 * set the image divisor for the OCR
		 * @param d 2/4
		 */
		public void SetImgDivisor(int d)
		{
			if (d!=2 && d!=4)
				return;
			m_bImgDivisor = (byte)d;		
		}

		/**
		 * @return the image divisor
		 */
		public byte GetImgDivisor()
		{
			return m_bImgDivisor;		
		}

		/**
		 * @return OCR dictionary language
		 */
		public String GetLanguage()
		{
			Log.v(TAG, "GetLanguage(): " + m_sLanguage);
			return m_sLanguage;
		}

		/**
		 * @return a string containing all available OCR dictionaries
		 */
		public String GetLanguages()
		{
			String ret = "";
			for (int i=0; i<m_asLanguages.length; i++)
			{
				ret += m_asLanguages[i];
				if (i!=m_asLanguages.length-1)
					ret += ",";
			}
			return ret;
		}

		/**
		 * @return a vector of strings, each string is a valid OCR-dictionary language
		 */
		public String[] GetvLanguages()
		{
			if (m_asLanguages == null)
				return null;

			String ret[] = new String[m_asLanguages.length];
			for (int i=0; i<m_asLanguages.length; i++)
				ret[i] = LanguageMore(m_asLanguages[i]);
			return ret;
		}

		/**
		 * Check if a language is installed.
		 * @param lang the language to check for
		 * @return if language is installed or not
		 */
		public boolean IsLanguageInstalled(String lang)
		{
			if (m_asLanguages == null)
				return false;

			for (int i=0; i<m_asLanguages.length; i++)
			{
				if (m_asLanguages[i].compareTo(lang) == 0)
					return true;
				if (LanguageMore(m_asLanguages[i]).compareTo(lang) == 0)
					return true;				
			}
			return false;
		}

		/**
		 * @param lang the language in short format
		 * @return the language in long-string format
		 */
		public String LanguageMore(String lang)

		{
			if ( lang.compareTo("eng") == 0 )
				return "English";			
			if ( lang.compareTo("spa") == 0 )
				return "Spanish";			
			if ( lang.compareTo("fra") == 0 )
				return "French";			
			if ( lang.compareTo("ita") == 0 )
				return "Italian";			
			if ( lang.compareTo("deu") == 0 )
				return "German";			
			return "error";
		}

		/** 
		 * @return the current OCR language in long-string format
		 */
		public String GetLanguageMore()
		{
			return LanguageMore(m_sLanguage);
		}

	} // end class OCRConfig




	// -------------------------------------------------------------------
	/**
	 * constructor
	 */
	private OCR()
	{
		String text = libVer();

		classInitNative();
		initializeNativeDataNative();

		mConfig.m_asLanguages = getLanguagesNative(); 	// this should be called at the beginning, 
		// such a way nobody calls without a valid language
		// if not called, the recognizeNative will not work
		Log.v(TAG, "OCR Initialize() done - libver "+ text + " no-langs-installed=" + mConfig.m_asLanguages.length);
	}

	public static void Initialize()
	{
		if (m_OCRinstance == null) 
		{
			m_OCRinstance = new OCR();
		}		
	}

	/**
	 * @return the ocr instance
	 */
	public static OCR get()
	{
		return m_OCRinstance;
	}

	/**
	 * cleanup function
	 */
	public void Destructor()  
	{
		Log.v(TAG, "OCR - finalize");
		if (m_bIsLibraryInitialized)
			OCRClean();
		m_bIsLibraryInitialized = false;
	}

	/**
	 * read the available OCR languages in the local vector
	 */
	public static void ReadAvailableLanguages()
	{
		mConfig.m_asLanguages = getLanguagesNative();
	}

	/**
	 * Set the OCR-dictionary language
	 * @param lang the language to be set
	 * @return true if language exists and was installed properly
	 */
	public boolean SetLanguage(String lang)
	{
		Log.v(TAG, "setLanguage to "+lang);
		if (mConfig.m_asLanguages == null)
			return false;

		Log.v(TAG, "noLangs=" + mConfig.m_asLanguages.length);

		for (int i=0; i<mConfig.m_asLanguages.length; i++)
			if (mConfig.m_asLanguages[i].compareTo(lang) == 0)
			{
				mConfig.m_sLanguage = lang;
				openNative(lang);
				Log.v(TAG, "setLang succeded");
				return true;
			}

		Log.v(TAG, "setLang failed");
		return false;
	}


	/**
	 * clear the API and the buffers used by the last OCR execution
	 */
	public void OCRClean()
	{
		clearResultsNative();		// api clear
		releaseImageNative();		// free the image buffers

		//cleanupNativeDataNative(); 	// free internal buffers (test for crash if image not allocated)
		//closeNative(); 				// end api - called by the destructor itself
		closeDebug();				// clean close debug	
	}

	/**
	 * Local store of mean confidence
	 */
	public void SaveMeanConfidence() 
	{
		m_iMeanConfidence = meanConfidenceNative();		
	}

	/**
	 *  from unsigned byte to int value
	 * @param b value to be converted
	 * @return the according int value
	 */
	public int unsignedByte2Int(byte b) 
	{
		return (int) b & 0xFF;
	}

	/**
	 *  read bytes from file into a byte buffer
	 * @param file the image-file to be read
	 * @param width the image width
	 * @param height the image height
	 * @param bpp bytes per pixel
	 * @return a vector with the image, no compression
	 * @throws IOException
	 */
	public byte[] GetBytesFromFile(File file, int width, int height, int bpp) throws IOException 
	{
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();        
		if (length < width*height*bpp)
		{
			// file is too small
			Log.v(TAG, "warning: file len is too small "+length);
			//return null; //bpp in the tif case is fractional 1b per pixel, meaning 1/8B per pixel, so this warning is wrong
		}


		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
			Log.v(TAG, "warning: length more than max allowed value");
			return null;
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int)length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "+file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}	

	/*
	 * ----------------------------------------------------------------------------------------
	 * OCR functions
	 * ----------------------------------------------------------------------------------------
	 */

	/**
	 * Transforms an image to bw 1bpp.
	 * @param bw_img the image
	 * @param width image width
	 * @param height image height
	 * @param bpp bytes per pixel
	 * @return the new bpp
	 */
	public int bwFilterImage(byte[] bw_img, int width, int height, int bpp)
	{
		//!! TBD if bpp=1 

		final int THOLD_WHITE = 0x70;
		final int THOLD_BLACK = 0x30;
		final int BLK_SZ = width/16; //=128 (this should be an int, otherwise we will get noise at the margins)

		final int RED_P = 3;
		final int GREEN_P = 6;
		final int BLUE_P = 1;

		if (bpp != 1) 
		{
			int local_min = THOLD_WHITE;
			int local_max = THOLD_BLACK;
			int local = 0;

			for (int i=0; i<width * height; i++) 
			{
				// compute the intensity of the pixel
				int red = unsignedByte2Int(bw_img[i*3+0]);
				int green = unsignedByte2Int(bw_img[i*3+1]);
				int blue = unsignedByte2Int(bw_img[i*3+2]);
				int intensity = red*RED_P + green*GREEN_P + blue*BLUE_P;
				intensity /= 10;

				// rarely: compute in advance the local min and max (for one BLK_SZ line)
				if ( local == 0 )
				{
					local_min = THOLD_WHITE;
					local_max = THOLD_BLACK;
					for (int k=i; k<i+BLK_SZ && k<width*height; k++)
					{
						int lred = unsignedByte2Int(bw_img[k*3+0]);
						int lgreen = unsignedByte2Int(bw_img[k*3+1]);
						int lblue = unsignedByte2Int(bw_img[k*3+2]);

						int lintensity = lred*RED_P + lgreen*GREEN_P + lblue*BLUE_P;
						lintensity /= 10;

						if ( lintensity > local_max ) local_max = lintensity;
						if ( lintensity < local_min ) local_min = lintensity;
					}
					local = BLK_SZ;
				}
				local --;

				// compare current pixel with local min and max
				if ( (local_max - intensity) < 0x15 )
				{
					bw_img[i] = -1;   // =0xff =white
				}
				else 
				{
					bw_img[i] = 0x00; // black
				}
			}
			// --------------------------------------------------------------

		}   

		// we give up 3Bpp, and replace them with 1Bpp (Bpp = Byte Per Pixel)
		return 1; // return the new bpp

	}

	/** 
	 * Apply OCR to a byte[] image
	 * @param bw_img the image as a vector (no compresion)
	 * @param width image width
	 * @param height image height
	 * @param bpp bytes per pixel
	 * @return the OCR result
	 */
	public String ImgOCRAndFilter(byte[] bw_img, int width, int height, int bpp)
	{
		String text = "";
		try {
			// the filter modifies the img-buffer itself
			//bpp = bwFilterImage(bw_img, width, height, bpp); 		


			setImageNative(bw_img, width, height, bpp);		// copy the image in the internal OCR buffers							

			setRectangleNative(0, 0, width, height);		// set rectangle to be OCRized							

			setPageSegModeNative(mConfig.m_iPSMMode);					// set the page segmentation mode											

			text = recognizeNative();						// OCR the buffered image
			//text = recognizeNative();						// OCR the buffered image

			//text = recognizeNative(bw_img, width, height, bpp); // may be used instead of the 4 steps above

			// do next - process the results				
		}
		catch (Exception ex)
		{
			text = ex.getMessage();
		}
		return text;
	}


	/**
	 * OCR an int[] image
	 * @param bw_img the image, no compression
	 * @param width image width
	 * @param height image height
	 * @param bHorizontalDisp if image is horizontal or vertical
	 * @return
	 */
	public String ImgOCRAndFilter(int[] bw_img, int width, int height, boolean bHorizontalDisp, boolean bLineMode)
	{
		String text_ret = "";
		m_bLineMode = bLineMode;
		try {
			setImageNative(bw_img, width, height, 
					mConfig.m_bUseBWFilter, bLineMode ? true : bHorizontalDisp); 				// copy the image in the internal OCR buffers							

			if (bHorizontalDisp || bLineMode)
				setRectangleNative(0, 0, width, height);					// set rectangle to be OCRized
			else 
				setRectangleNative(0, 0, height, width);

			setPageSegModeNative(mConfig.m_iPSMMode);									// set the page segmentation mode											

			String bulk_text = recognizeNative();							// OCR the buffered image
			text_ret = bulk_text;
			Log.v(TAG, "OCR brute text is ["+bulk_text+"]");


			int wconf[]	= wordConfidencesNative();							// get the confidence for each word
			Log.v(TAG, "no confidences="+wconf.length);

			String st[] = bulk_text.split("/");								// get the list of words decoded
			Log.v(TAG, "no words=" + st.length);

			boolean bLenMatch = true;
			int len = st.length-1;
			if (wconf.length != st.length-1)
			{
				Log.v(TAG, "errror: the word len do not match --------------------------");
				bLenMatch = false;
			}				

			// create the list of words and attributes	
			m_asWords = new Word[len];
			for (int i=0; i<len; i++)
				m_asWords[i] = new Word( st[i], bLenMatch ? wconf[i] : 0 ); 

			Log.v(TAG, "allocated memory, linemode=" + m_bLineMode);

			
			
			String debug_text = "";

			// in line-mode we keep only valid dictionary words and words with a large OCR trust
			if (m_bLineMode)
			{
				text_ret = "";
				for (int i=0; i<m_asWords.length; i++)
				{
					debug_text += "\t[" + m_asWords[i].m_sBody + "/" + 
					m_asWords[i].m_bIsValidWord +"/"+ 
					m_asWords[i].m_iConfidence +"]";

					// show the valid dictionary words
					if ( m_asWords[i].m_sBody.length() >= 3 &&
						 m_asWords[i].m_iConfidence > 80
						)		
					{
						text_ret += m_asWords[i].m_sBody + " ";
						continue;
					}
					
					// this is a void word (in line mode we do not display anything for it)
					if (  m_asWords[i].m_sBody.length() < 3 ||
						  !IsValidLineModeWord(m_asWords[i].m_sBody) // this checks if the word contains signs or numbers 
						)
					{
							continue;
					}

					// show the valid dictionary words
					if ( m_asWords[i].m_bIsValidWord ||
						 m_asWords[i].m_iConfidence > mConfig.m_iMinWordConfidence
						)		
					{
						text_ret += m_asWords[i].m_sBody + " ";
						continue;
					}

				}


				Log.v(TAG, "polished text is ["+ text_ret +"]");
				Log.v(TAG, "debug text is ["+ debug_text +"]");
			}
			
			
			
			// polish the results according to the configuration, if not in line-mode
			if (mConfig.m_iMinWordConfidence > 0 && !m_bLineMode)
			{
				text_ret = "";
				for (int i=0; i<m_asWords.length; i++)
				{
					debug_text += "\t[" + m_asWords[i].m_sBody + "/" + 
					m_asWords[i].m_bIsValidWord +"/"+ 
					m_asWords[i].m_iConfidence +"]";

					// this is a void word (in line mode we do not display anything for it)
					if (  m_asWords[i].m_sBody.compareTo(" ") == 0 ||
							m_asWords[i].m_sBody.contains("#") ||
							m_asWords[i].m_sBody.length() == 0)

					{
						Log.v(TAG, "1: [" + m_asWords[i].m_sBody + "]");
						text_ret += "... ";
						if (m_asWords[i].m_bNewLine)
							text_ret += "\n";
						m_asWords[i].m_iEnd = text_ret.length();
						continue;
					}

					// show the valid dictionary words
					if (m_asWords[i].m_bIsValidWord)		
					{
						Log.v(TAG, "2: ["+m_asWords[i].m_sBody+"]");
						text_ret += m_asWords[i].m_sBody + " ";
						if (m_asWords[i].m_bNewLine)
							text_ret += "\n";
						m_asWords[i].m_iEnd = text_ret.length();
						continue;
					}

					// invalid dictionary word, but preaty good confidence - show them
					if (m_asWords[i].m_iConfidence > mConfig.m_iMinWordConfidence)
					{   
						Log.v(TAG, "3: ["+m_asWords[i].m_sBody+"]");
						text_ret += m_asWords[i].m_sBody;
						text_ret += " ";
					}
					else
					{
						text_ret += "... ";
					}

					if (m_asWords[i].m_bNewLine)
						text_ret += "\n";
					m_asWords[i].m_iEnd = text_ret.length();
				}


				Log.v(TAG, "polished text is ["+ text_ret +"]");
				Log.v(TAG, "debug text is ["+ debug_text +"]");
			}

		}
		catch (Exception ex)
		{
			text_ret = ex.toString();
			Log.v(TAG, "Exception (ImgOCRAndFilter): " + ex.toString());
		}

		
		
		
		// create the spannable string, to be displayed
		m_ssOCRResult = new SpannableString(text_ret);

		if (!m_bLineMode)
		{
			int start = 0;
			int end = 0; 
			for (int i=0; i<m_asWords.length; i++)
			{
				end = m_asWords[i].m_iEnd;

				if ( m_asWords[i].m_bIsValidWord == false || 
						(m_asWords[i].m_iConfidence!=0 && m_asWords[i].m_iConfidence < mConfig.m_iMinWordConfidence)
				)
				{
					m_ssOCRResult.setSpan(new StyleSpan(Typeface.SERIF.MONOSPACE.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					Log.v(TAG, "ss: "+start+"-"+end);
				}
				start = end;
			}
		}

		return text_ret;
	}

	

	/**
	 * Get the index of one word, given the position in the decoded text. 
	 * @param iPosInString the position in the string
	 * @return returns -1 if iPosInString is longer than the string itself
	 */
	public static int GetWordIndex(int iPosInString)
	{
		int i=0;
		int prev = 0;
		for (; i<m_asWords.length; i++)
		{
			if (iPosInString >= prev && iPosInString < m_asWords[i].m_iEnd)
				break;
			prev = m_asWords[i].m_iEnd;
		}	

		if (i == m_asWords.length) return -1;
		return i;
	}



	/**
	 *  OCR a non-coded image from a file, 1 pixel = bpp bytes
	 * @param filename the image-file
	 * @param width image width
	 * @param height image height
	 * @param bpp bytes per pixel
	 * @return the OCR result
	 */
	public String FileOCR(String filename, int width, int height, int bpp)
	{
		String text = "";
		try {

			byte[] bw_img = null;

			// -------------- step 1
			bw_img = GetBytesFromFile(new File(filename), width, height, bpp); 	// 1
			if (bw_img == null)
			{
				return "could not load file\n";
			}

			text = ImgOCRAndFilter(bw_img, width, height, bpp); 		
			// do next - process the results				
		}
		catch (Exception ex)
		{
			text = ex.getMessage();
		}
		return text;
	}


	
	
	
	

	/**
	 * Verifies if the provided word is valid for line-mode, where we have more constrains than full-mode
	 * @param s - the word
	 * @return validity
	 */
	private boolean IsValidLineModeWord(String s)
	{
		// if only letters
		if (s.matches("[a-zA-Z]+")) 
			return true;
		if (s.contains("#"))
			return false;
		
		return IsValidComposedWord(s);
	}

	/**
	 * This is used in 'word' constructor, we do not want single-letters and signs to be valid dictionary words
	 * @param text the word to be searched
	 * @return validity of the word, according to the dictionary
	 */
	public static boolean MyIsValidDictionaryWord(String text)
	{
		// this is for numbers, signs and alphabet letters
		if (text.length() < 2) 
			return false; 
		
		return IsValidComposedWord(text);
	}

	/**
	 * Wrapper for JNI-IsValidWord - all chars converted to lower case, check for composed-words (ex: white-grey)
	 * @param word the word to be searched
	 * @return validity of the word, according to the dictionary
	 */
	public static boolean IsValidComposedWord(String word)
	{
		String lWord = word.toLowerCase();

		// this checks for composed words, ex: well-equiped, grey-white, Montenegro's 
		String ss = lWord.replaceAll("[^a-zA-Z]", " ");
		String [] vect = ss.split(" ");
		
		for (int i=0; i<vect.length; i++)
			if ( vect[i].length()>0 && !get().isValidWord(vect[i]) )		// even if one word is not valid, return
			{
				Log.v(TAG, "word: ["+vect[i] +"] is not valid");
				return false;
			}
			
		return true;
	}

	/*
	 * ----------------------------------------------------------------------------------------
	 * Java-lib variables
	 * ----------------------------------------------------------------------------------------
	 */
	/* The methods implemented by the 'ocr' native library, which is packaged
	 * with this application.
	 */

	// general initialization/cleanup/setup functions
	public native void    	classInitNative();				// init the lib (1st to be called at startup)
	public native void    	initializeNativeDataNative();	// init allocate lib buffers (2nd to be called)
	public native boolean 	openNative(String sLanguage);	// init the api with a language

	public native void    	cleanupNativeDataNative();		// delete the lib buffers
	public native void		clearResultsNative();			// api clear
	public native void		closeNative();					// api.end, called by the destructor automatically

	public native void		setVariableNative(				// set a lib variable
			String var, String value);    



	// language functions    
	public static native String[]  getLanguagesNative();	// get the language list
	public native int		getShardsNative(				// get the shard of the language
			String lang);
	public native boolean 	isValidWord(String word);		// is the word valid according to the installed language 



	// aux functions before OCR
	public native void		setImageNative(					// copy the image to the internal api buffers				
			byte[] image, int width, int height, int bpp);
	public native void		setImageNative(					// copy the image to the internal api buffers				
			int[] image, int width, int height, 
			boolean bBWFilter, boolean bHorizDisp);
	public native void		releaseImageNative();			// release the internal api buffers
	public native void		setRectangleNative(				// set the rectangle where OCR will focus 
			int left, int top, int width, int height);
	public native void		setPageSegModeNative(			// set the page segmentation mode
			int mode);


	// OCR
	public native String	recognizeNative(				// do OCR over the parameter image
			byte[] image, int width, int height, int bpp);
	public native String	recognizeNative();				// do OCR over the image in the api buffers (all ocr)
	public native String	recognizeNative(int nopass);	// do OCR over the image in the api buffers 
	// 		(options: 0=all, 1 or 2 passes)


	// aux functions, to be run after OCR    
	public native int		meanConfidenceNative();			// mean confidence (last OCR)
	public native int[]		wordConfidencesNative(); 		// confidence for each word (last OCR)
	public native String 	getBoxText();					// get the box for each letter


	// debug functions    
	public native void    	closeDebug();					// clean close the debug (if any)
	public native String 	libVer();						// get the lib version

	public int mNativeData; 								// storage space for the library's internal buffers


	/* this is used to load the 'ocr' library on application
	 * startup. The library has already been unpacked into
	 * /data/data/com.../lib/libocr.so at installation time by the package manager.
	 */
	static 
	{
		System.loadLibrary("ocr");
	}
}
