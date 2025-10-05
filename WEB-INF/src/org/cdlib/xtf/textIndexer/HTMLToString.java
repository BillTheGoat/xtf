package org.cdlib.xtf.textIndexer;

/**
 * Copyright (c) 2004, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.w3c.tidy.Tidy;
import org.mozilla.universalchardet.UniversalDetector;
import org.cdlib.xtf.util.*;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

/** This class provides a single static {@link HTMLToString#convert(InputStream) convert() }
 *  method that converts an HTML file into an XML string that can be
 *  pre-filtered and added to a Lucene database by the
 *  {@link XMLTextProcessor } class. <br><br>
 *
 *  Internally, the HTML to XML file conversion is performed by the jTidy
 *  library, which is a variant of the HTMLTidy converter.
 */
public class HTMLToString 
{
  /** Create the HTMLTidy object that will do the work. */
  static Tidy tidy = new Tidy();

  //////////////////////////////////////////////////////////////////////////////

  /** Convert an HTML file into an HTMLTidy style XML string.
   *
   *  @param htmlInputStream  Stream of HTML text to convert to an XML string.
   *
   *  @return
   *      If successful, a string containing the XML equivalent of the source
   *      HTML file. If an error occurred, this method returns <code>null</code>.
   *
   */
  static public String convert(InputStream htmlInputStream) 
  {
    // Tell Tidy to suppress warning and other output messages.
    if (Trace.getOutputLevel() == Trace.debug) {
      tidy.setErrout(new PrintWriter(new TraceWriter(Trace.debug)));
      tidy.setQuiet(false);
      tidy.setShowWarnings(true);
    }
    else {
      tidy.setQuiet(true);
      tidy.setShowWarnings(false);
    }

    // Tell Tidy to make XML as it outputs.
    tidy.setXmlOut(true);
    tidy.setOutputEncoding("UTF-8");
    tidy.setForceOutput(true); // return something whenever possible
    tidy.setDocType("omit"); // Omit DOCTYPE to avoid parsing issues
    
    try {
        // Detect input encoding using juniversalchardet, fallback to UTF-8
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = htmlInputStream.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        byte[] data = buffer.toByteArray();
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(data, 0, data.length);
        detector.dataEnd();
        String detected = detector.getDetectedCharset();
        detector.reset();
        String encoding = (detected != null) ? detected : "UTF-8";
        Trace.debug("Detected HTML encoding: " + encoding);
        tidy.setInputEncoding(encoding);

        // --- Convert the HTML to XML ---
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream tidyInput = new ByteArrayInputStream(data)) {
            tidy.parse(tidyInput, out);
        }

        // --- Convert output to string ---
        String retStr = out.toString("UTF-8");
        // remove illegal control characters
        retStr = retStr.replaceAll("[\\x00-\\x08\\x0B-\\x0C\\x0E-\\x1F]", "");
        // Replace windows abused control codes with proper unicode
        retStr = replaceHtmlCodes(retStr);

        return retStr;
    } //try

    // If anything went wrong, say what it was.
    catch (Throwable t) {
      Trace.error("*** HTMLToXML.convert() Exception: " + t.getClass());
      Trace.error("                     With message: " + t.getMessage());
    }

    // If we got to this point, something went wrong. So return a null
    // string back to the caller.
    //
    return null;
  } // static public String convert()


  //////////////////////////////////////////////////////////////////////////////

  /** Convert any non-XML ampersand codes within a string to their unicode
   *  equivalents.
   *
   *  @param in  The string within which to convert codes.
   */
public static String replaceHtmlCodes(String in) 
{
  // Scan through the string, looking for numeric HTML entities in the 128-159 range
  StringBuffer out = new StringBuffer(in.length());
  char[] inChars = in.toCharArray();
  int i = 0;
  
  while (i < inChars.length) 
  {
    // Look for an ampersand
    if (inChars[i] != '&') { 
      out.append(inChars[i++]);
      continue;
    }
    
    // Find the end of the entity
    int start = i + 1;
    int end = start;
    while (end < inChars.length && 
           (inChars[end] == '#' || Character.isLetterOrDigit(inChars[end])))
    {
      end++;
    }
    
    // Must end with semicolon
    if (end == inChars.length || inChars[end] != ';') {
      out.append(inChars[i++]);
      continue;
    }
    
    // Check if it's a numeric entity (&#xxx;)
    if (inChars[start] == '#') 
    {
      try {
        int codeNum = Integer.parseInt(in.substring(start + 1, end));
        
        // Only replace Windows-1252 codes (128-159)
        if (codeNum >= 128 && codeNum <= 159)
        {
          int outNum = 0;
          switch (codeNum)
          {
            case 128:  outNum = 0x20ac;  break;
            case 129:  outNum = 0x0081;  break;
            case 130:  outNum = 0x201A;  break;
            case 131:  outNum = 0x0192;  break;
            case 132:  outNum = 0x201E;  break;
            case 133:  outNum = 0x2026;  break;
            case 134:  outNum = 0x2020;  break;
            case 135:  outNum = 0x2021;  break;
            case 136:  outNum = 0x02C6;  break;
            case 137:  outNum = 0x2030;  break;
            case 138:  outNum = 0x0160;  break;
            case 139:  outNum = 0x2039;  break;
            case 140:  outNum = 0x0152;  break;
            case 141:  outNum = 0x008D;  break;
            case 142:  outNum = 0x017D;  break;
            case 143:  outNum = 0x008F;  break;
            case 144:  outNum = 0x0090;  break;
            case 145:  outNum = 0x2018;  break;
            case 146:  outNum = 0x2019;  break;
            case 147:  outNum = 0x201C;  break;
            case 148:  outNum = 0x201D;  break;
            case 149:  outNum = 0x2022;  break;
            case 150:  outNum = 0x2013;  break;
            case 151:  outNum = 0x2014;  break;
            case 152:  outNum = 0x02DC;  break;
            case 153:  outNum = 0x2122;  break;
            case 154:  outNum = 0x0161;  break;
            case 155:  outNum = 0x203A;  break;
            case 156:  outNum = 0x0153;  break;
            case 157:  outNum = 0x009D;  break;
            case 158:  outNum = 0x017E;  break;
            case 159:  outNum = 0x0178;  break;
          }
          out.append("&#");
          out.append(Integer.toString(outNum));
          out.append(";");
          i = end + 1;
          continue;
        }
      }
      catch (NumberFormatException e) { 
        // Not a valid number, pass through as-is
      }
    }
    
    // Pass through unchanged (numeric entity outside 128-159 range, 
    // named entity, or malformed entity)
    out.append(inChars[i++]);
  }
  
  return out.toString();
} //replaceHtmlCodes()

} // class HTMLToString