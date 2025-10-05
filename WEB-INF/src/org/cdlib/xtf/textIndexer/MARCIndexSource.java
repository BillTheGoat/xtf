package org.cdlib.xtf.textIndexer;

/*
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
 *
 * Acknowledgements:
 *
 * A significant amount of new and/or modified code in this module
 * was made possible by a grant from the Andrew W. Mellon Foundation,
 * as part of the Melvyl Recommender Project.
 */
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Vector;
import javax.xml.transform.Templates;

import org.apache.lucene.util.CountedInputStream;
import org.cdlib.xtf.util.Normalizer;
import org.cdlib.xtf.util.StructuredStore;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Supplies MARC data to an XTF index, breaking it up into individual MARCXML
 * records.
 *
 * @author Martin Haye
 */
public class MARCIndexSource extends IndexSource 
{
  /** Constructor -- initializes all the fields */
  public MARCIndexSource(File path, String key, Templates[] preFilters,
                         Templates displayStyle) 
  {
    this.path = path;
    this.key = key;
    this.preFilters = preFilters;
    this.displayStyle = displayStyle;

    // Find out the file's total size, for percent done calculations.
    fileSize = path.length();
  }

  /** Path to the file, or null if it's not a local file. */
  private File path;

  /** Key used to identify this file in the index */
  private String key;

  /** XSLT pre-filters used to massage the XML document (null for none) */
  private Templates[] preFilters;

  /** Stylesheet from which to gather XSLT key definitions to be computed
   *  and cached on disk. Typically, one would use the actual display
   *  stylesheet for this purpose, guaranteeing that all of its keys will be
   *  pre-cached.<br><br>
   *
   *  Background: stylesheet processing can be optimized by using XSLT 'keys',
   *  which are declared with an &lt;xsl:key&gt; tag. The first time a key
   *  is used in a given source document, it must be calculated and its values
   *  stored on disk. The text indexer can optionally pre-compute the keys so
   *  they need not be calculated later during the display process.
   */
  private Templates displayStyle;

  /** Size of the whole input file */
  private long fileSize = -1;

  /** Input stream for the raw data */
  private CountedInputStream rawStream = null;

  /** Record handling thread */
  private RecordHandler recordHandler;

  /** Are we there yet? */
  private boolean isDone = false;
  private int recordNum = 0;

  // inherit JavaDoc
  public File path() {
    return path;
  }

  // inherit JavaDoc
  public String key() {
    return key;
  }

  // inherit JavaDoc
  public Templates[] preFilters() {
    return preFilters;
  }

  // inherit JavaDoc
  public Templates displayStyle() {
    return displayStyle;
  }

  // inherit JavaDoc
  public long totalSize() {
    return fileSize;
  }

  // inherit JavaDoc
  public IndexRecord nextRecord()
    throws SAXException, IOException 
  {
    // If we're done, say so.
    if (isDone)
      return null;

    // Open the MARC file if we haven't already.
    openFile();

    // Get the next record from the handler thread.
    String parsedMarcXML = null;
    synchronized (recordHandler) 
    {
      while (true) 
      {
        if (recordHandler.isDone) {
          isDone = true;
          break;
        }
        if (recordHandler.parsedMarcXML != null) {
          parsedMarcXML = recordHandler.parsedMarcXML;
          ++recordNum;
          recordHandler.parsedMarcXML = null;
          recordHandler.notifyAll();
          break;
        }
        try {
          recordHandler.wait();
        }
        catch (InterruptedException e) {
          assert false : "how could this thread be interrupted??";
          isDone = true;
          break;
        }
      }
    } // sync

    // If we ran out of records, say so.
    if (isDone)
      return null;

    // Okay, make a record out of it.
    final Reader reader = new StringReader(parsedMarcXML);
    return new IndexRecord() 
    {
      public InputSource xmlSource()
        throws IOException 
      {
        return new InputSource(reader);
      }

      public int recordNum() {
        return recordNum;
      }

      public int percentDone() {
        return (int)((rawStream.nRead() + 1) * 100 / fileSize);
      }

      public StructuredStore lazyStore() {
        return null;
      }
    };
  } // nextRecord()

  private void openFile()
    throws IOException 
  {
    // Only open the file once.
    if (rawStream != null)
      return;

    // Open the input stream and reader.
    rawStream = new CountedInputStream(
      new BufferedInputStream(new FileInputStream(path)));

    // The output of the MARC converter will go to an XML handler of our
    // own design.
    //
    recordHandler = new RecordHandler();

    // Fire up the thread that will do the conversion.
    recordHandler.start();
  } // openFile()

  /**
   * Detect encoding from MARC leader byte 9
   */
  private String detectMarcEncoding(InputStream inputStream) throws IOException {
    inputStream.mark(24);
    byte[] leader = new byte[24];
    int bytesRead = inputStream.read(leader);
    inputStream.reset();
    
    if (bytesRead >= 24) {
      char encodingByte = (char) leader[9];
      switch (encodingByte) {
        case 'a':
          return "UTF-8";
        case ' ':
        case '#':
        default:
          return "ISO8859_1";  // MARC-8 or default
      }
    }
    return "ISO8859_1";  // Default fallback
  }

  /**
   * Handles running blocks of records through the stylesheet
   */
  private class RecordHandler extends Thread implements ContentHandler 
  {
    /** A single parsed MARCXML record */
    public String parsedMarcXML = null;

    /** Set to true when this thread has finished its business. */
    public boolean isDone = false;

    /** If an exception occured, it is recorded here */
    public Throwable error = null;

    /** Names of XML namespace prefixes */
    private Vector prefixNames = new Vector();

    /** URIs of XML namespace prefixes */
    private Vector prefixUris = new Vector();

    /** Mapping from URI to name */
    private HashMap prefixUriToName = new HashMap();

    /** Accumulates the current MARCXML record */
    private StringBuffer buffer = new StringBuffer();
    private int recordNum = 0;

    public void run() 
    {
      try 
      {
        while (true) 
        {
          long startPos = rawStream.nRead();
          try {
            convertRecords();
            long endPos = rawStream.nRead();
            if (endPos == startPos)
              break;
            else if (skipBadRecord())
              continue;
            else
              break;
          }
          catch (Throwable t) {
            long endPos = rawStream.nRead();
            if (endPos == startPos)
              throw t;
            else if (skipBadRecord())
              continue;
            else
              break;
          }
        }
      }
      catch (Throwable t) {
        error = t;
      }
      finally {
        isDone = true;
        synchronized (this) {
          notifyAll();
        }
      }
    }

    private void convertRecords()
      throws Exception 
    {
      // Detect encoding from MARC leader
      String encoding = detectMarcEncoding(rawStream);
      
      // Create a MarcReader with the detected encoding
      MarcReader reader = new MarcStreamReader(rawStream, encoding);
      
      // Process each record
      while (reader.hasNext()) {
        try {
          Record record = reader.next();
          
          // Convert to MARCXML using MarcXmlWriter
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          MarcXmlWriter writer = new MarcXmlWriter(baos, true);  // true = UTF-8
          writer.setIndent(true);
          writer.write(record);
          writer.close();
          
          // Get the XML as a string
          String marcXml = baos.toString("UTF-8");
          // Strip illegal XML characters (including ESC 0x1B from MARC-8)
          marcXml = stripIllegalXmlCharacters(marcXml);
          
          // Parse through our ContentHandler to apply normalization
          parseXmlString(marcXml);
          
        } catch (Exception e) {
          System.err.println("Error processing MARC record " + recordNum + ": " + e.getMessage());
          throw e;
        }
      }
    } // convertRecords

    /**
     * Parse MARCXML string through our ContentHandler for normalization
     */
    private void parseXmlString(String xml) throws Exception {
      javax.xml.parsers.SAXParserFactory factory = 
        javax.xml.parsers.SAXParserFactory.newInstance();
      factory.setNamespaceAware(true);
      javax.xml.parsers.SAXParser parser = factory.newSAXParser();
      
      org.xml.sax.XMLReader xmlReader = parser.getXMLReader();
      xmlReader.setContentHandler(this);
      
      InputSource source = new InputSource(new StringReader(xml));
      xmlReader.parse(source);
    }

    private String stripIllegalXmlCharacters(String input) {
      if (input == null) return null;
      
      // Pattern matches &#[decimal]; or &#x[hex]; entity references
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#(x?)([0-9A-Fa-f]+);");
      java.util.regex.Matcher matcher = pattern.matcher(input);
      
      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
        String hexPrefix = matcher.group(1);
        String numStr = matcher.group(2);
        
        try {
          int charCode;
          if ("x".equals(hexPrefix)) {
            charCode = Integer.parseInt(numStr, 16);
          } else {
            charCode = Integer.parseInt(numStr, 10);
          }
          
          // Check if this is an illegal XML character
          // Legal: 0x9 (tab), 0xA (LF), 0xD (CR), 0x20-0xD7FF, 0xE000-0xFFFD
          if ((charCode == 0x9 || charCode == 0xA || charCode == 0xD) ||
              (charCode >= 0x20 && charCode <= 0xD7FF) ||
              (charCode >= 0xE000 && charCode <= 0xFFFD)) {
            // Legal character - keep the entity reference
            matcher.appendReplacement(sb, matcher.group(0));
          } else {
            // Illegal character - remove it (replace with empty string)
            matcher.appendReplacement(sb, "");
          }
        } catch (NumberFormatException e) {
          // If we can't parse it, keep it as-is
          matcher.appendReplacement(sb, matcher.group(0));
        }
      }
      matcher.appendTail(sb);
      
      return sb.toString();
    }

    private boolean skipBadRecord()
      throws IOException 
    {
      int nSkipped = 0;
      while (true) 
      {
        int ch = rawStream.read();
        if (ch < 0) {
          System.err.flush();
          System.out.println("Bad MARC data near end of file. Skipping.");
          return false;
        }
        // Look for record terminator (0x1D)
        if (ch == 0x1D)
          break;
        ++nSkipped;
      }
      if (nSkipped > 0) {
        System.err.flush();
        System.out.println(
          "Bad MARC data near record " + recordNum + ". Attempting to resume.");
      }
      return true;
    } // skipBadRecord()

    private void beginChunk()
      throws SAXException 
    {
      buffer.setLength(0);
      buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

      // Output the record wrapper, with any namespace prefix declarations.
      buffer.append("<record");
      for (int i = 0; i < prefixNames.size(); i++) 
      {
        String prefixName = (String)prefixNames.get(i);
        String prefixUri = (String)prefixUris.get(i);
        buffer.append(" xmlns");
        if (prefixName != null && prefixName.length() > 0) {
          buffer.append(':');
          buffer.append(prefixName);
        }
        buffer.append("=\"");
        buffer.append(prefixUri);
        buffer.append('\"');
      }
      buffer.append(">\n");
    }

    private void endChunk()
      throws SAXException 
    {
      buffer.append("</record>\n");

      // Pass the newly parsed record to the main thread.
      synchronized (this) 
      {
        // Put up the new record, and notify the main thread.
        assert parsedMarcXML == null; // invalid state?
        parsedMarcXML = buffer.toString();
        ++recordNum;
        notifyAll();

        // Now wait for it to be consumed. Previously this wait was before
        // putting up the new record, and that caused the last record of
        // the file to be missed because by the time the main thread got
        // around to looking, our isDone flag would already be set.
        //
        while (parsedMarcXML != null) 
        {
          try {
            wait();
          }
          catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }
    } // endChunk()

    public void startDocument()
      throws SAXException 
    {
      prefixNames.clear();
      prefixUris.clear();
      prefixUriToName.clear();
    }

    public void endDocument()
      throws SAXException 
    {
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes atts)
      throws SAXException 
    {
      if (localName.equals("record")) {
        beginChunk();
        return;
      }

      // First, the "<"
      buffer.append("<");

      // Then the element name.
      if (qName != null)
        buffer.append(qName);
      else if (uri != null) {
        String prefix = (String)prefixUriToName.get(uri);
        assert prefix != null : "invalid URI??";
        buffer.append(prefix);
        buffer.append(':');
        buffer.append(localName);
      }
      else
        buffer.append(localName);

      // Then each attribute.
      for (int i = 0; i < atts.getLength(); i++) 
      {
        buffer.append(' ');

        if (atts.getQName(i) != null)
          buffer.append(atts.getQName(i));
        else if (atts.getURI(i) != null) {
          String prefix = (String)prefixUriToName.get(atts.getURI(i));
          assert prefix != null : "invalid URI??";
          buffer.append(prefix);
          buffer.append(':');
          buffer.append(atts.getLocalName(i));
        }
        else
          buffer.append(atts.getLocalName(i));

        buffer.append("=\"");
        buffer.append(atts.getValue(i));
        buffer.append("\"");
      }

      // Close the declaration.
      buffer.append(">");
    }

    public void endElement(String uri, String localName, String qName)
      throws SAXException 
    {
      if (localName.equals("record")) {
        endChunk();
        return;
      }

      // First, the "</"
      buffer.append("</");

      // Then the element name.
      if (qName != null)
        buffer.append(qName);
      else if (uri != null) {
        String prefix = (String)prefixUriToName.get(uri);
        assert prefix != null : "invalid URI??";
        buffer.append(prefix);
        buffer.append(':');
        buffer.append(localName);
      }
      else
        buffer.append(localName);

      // Then the end.
      buffer.append(">");
    }

    public void characters(char[] ch, int start, int length)
      throws SAXException 
    {
      // Scan for suspicious characters that might need Unicode 
      // normalization.
      //
      boolean needNormalize = false;
      int needEscape = 0;
      for (int i = start; i < start + length; i++) 
      {
        if ((ch[i] & ~0x7f) != 0)
          needNormalize = true;

        if (ch[i] == '&' || ch[i] == '<')
          ++needEscape;
        else if (ch[i] < '\u0020' &&
                 (ch[i] != '\t' && ch[i] != '\n' && ch[i] != '\r')) 
        {
          ++needEscape;
        }
        else if (ch[i] >= '\uD800' && ch[i] <= '\uDFFF')
          ++needEscape;
        else if (ch[i] >= '\uFFFE' && ch[i] <= '\uFFFF')
          ++needEscape;
      }

      if (needNormalize) 
      {
        String s = new String(ch, start, length);
        String s2 = Normalizer.normalize(s);
        if (!s.equals(s2)) 
        {
          ch = s2.toCharArray();
          start = 0;
          length = ch.length;
        }
      }

      if (needEscape > 0) 
      {
        int maxSpace = length + (needEscape * 5);
        char[] newCh = new char[maxSpace];
        int dp = 0;
        for (int sp = start; sp < (start + length); sp++) 
        {
          if (ch[sp] == '&') {
            newCh[dp++] = '&';
            newCh[dp++] = 'a';
            newCh[dp++] = 'm';
            newCh[dp++] = 'p';
            newCh[dp++] = ';';
          }
          else if (ch[sp] == '<') {
            newCh[dp++] = '&';
            newCh[dp++] = 'l';
            newCh[dp++] = 't';
            newCh[dp++] = ';';
          }
          else if (ch[sp] < '\u0020' &&
                   (ch[sp] != '\t' && ch[sp] != '\n' && ch[sp] != '\r')) 
          {
            ; // delete invalid character
          }
          else if (ch[sp] >= '\uD800' && ch[sp] <= '\uDFFF')
            ; // delete invalid character
          else if (ch[sp] >= '\uFFFE' && ch[sp] <= '\uFFFF')
            ; // delete invalid character
          else
            newCh[dp++] = ch[sp];
        }
        ch = newCh;
        start = 0;
        length = dp;
      }

      buffer.append(ch, start, length);
    }

    public void startPrefixMapping(String prefix, String uri)
      throws SAXException 
    {
      prefixNames.add(prefix);
      prefixUris.add(uri);
      prefixUriToName.put(uri, prefix);
    }

    public void endPrefixMapping(String prefix)
      throws SAXException 
    {
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException 
    {
    }

    public void processingInstruction(String target, String data)
      throws SAXException 
    {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void skippedEntity(String name)
      throws SAXException 
    {
    }
  } // class RecordHandler
} // class MARCIndexSource