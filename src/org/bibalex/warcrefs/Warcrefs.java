package org.bibalex.warcrefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jwat.common.HeaderLine;
import org.jwat.common.Payload;
import org.jwat.common.UriProfile;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterCompressed;
import org.jwat.warc.WarcWriterFactory;
import org.jwat.warc.WarcWriterUncompressed;

public class Warcrefs {
  /*
   * This is the digest index, which looks like this:
   *
   * fn off len uri date digest digestExt copy referstoUri referstoDate
   */
  private LineNumberInputStream digests;

  /*
   * This is the input file (duplicated) and the output
   * file(deduplicated).
   */
  private FileInputStream dup;
  private FileOutputStream dedup;

  private int curentRecordOff;

  public static void main(String[] args) {

    /*
     * The rest of this class is more like pseudocode that will not
     * compile.
     */

    currentRecordOff = 0;

    while(getNextDigest()) {
      if(fn == fn && off == off) {
        if(copy == 1) {
          dup.read(b, 0, len);
          dedup.write(b, 0, len);
          currentRecordOff += len;
        } else {
          wr.getNextRecord();
          makeRevisitRecord();
          ww.writeRecord();
        }
    }

    System.exit(0);
  }

  private getNextDigest() {
  }
}
