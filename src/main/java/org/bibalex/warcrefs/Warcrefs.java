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

  private int curentRecordOff;

  public static void main(String[] args) {
    try {
      // The BufferedReader provides a readLine() method.
      BufferedReader digestsReader = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));

      /*
       * Each line will be split into fields, where 'h' is the digest
       * (hash), and 'hx' is the digest extension (and other shorthands
       * are obvious):
       *
       * fn off len uri date h hx copy refsuri refsdate
       *  0   1   2   3    4 5  6    7       8        9
       */
      String[] digestLine;
    
      // While there are more files in directories.
      while (true) {
        digestLine = digestsReader.readLine().split(" ");


        //IF OFF AND LEN EQUAL CURRENT FN AND LEN

        //IF COPY 1

        //  fis.read
        //    (b, 0, len);
        //  fos.write
        //    (b, 0, len);

        //  currentRecordOff += len;

        //IF COPY 2

        //  READ NEXT RECOD USING JWAT AND WRITE REVISIT RECORD.


        // TODO: Implement this loop and remove the break.
        break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
