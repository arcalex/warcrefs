/**
 * @author Mohamed Elsayed
 */

package org.bibalex.warcrefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.jwat.common.HeaderLine;
import org.jwat.common.HttpHeader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderCompressed;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterCompressed;
import org.jwat.warc.WarcWriterUncompressed;
import org.jwat.warc.WarcWriterFactory;

public class Deduplicator 
{
    private List<String> warcFiles = new ArrayList();
    private List<String> warcRecords = new ArrayList();
    private RandomAccessFile digests;
    private int bufferSize;
    private String currentWarc;     // represents absolute path of the current warc which is under processing
    
    public Deduplicator() 
    {
        // Default length (8k) that will be copied (written) to dedup_warc
        bufferSize = 8129;
    }
    
    public Deduplicator( int size )
    {
        setBufferSize( size );
    }
    
    public void deduplicate( String digestsPath, String rootDirPath )
            throws FileNotFoundException, IOException
    {
        // Initialization phase
        digests = new RandomAccessFile( digestsPath, "r" );          
        File rootDir = new File( rootDirPath );                     
        
        // Processing phase
        findAllWarcsRecursively( rootDir );
        
        for ( String warc : warcFiles )
        {
            currentWarc = warc;
            writeWarcDedup( warc );
            printWarcRecords( warcRecords );
        }
        
        // Terminating phase
        printWarcs( warcFiles );
    }
    
    private void writeWarcDedup( String warcAbsolutePath ) throws IOException
    {
        String warcBasename = warcAbsolutePath.substring(
                warcAbsolutePath.lastIndexOf( "/" ) + 1 );
        
        String warcDirectory = warcAbsolutePath.substring( 
                0, warcAbsolutePath.lastIndexOf( "/" ) + 1 );
        
        findWarcRecordsFromDigests( warcBasename );
        
        if ( !warcRecords.isEmpty() )
        {
            String warcDedupAbsolutePath = warcDirectory + 
                    warcBasename.substring( 0, warcBasename.length() - 8 ) +
                    "_dedup.warc.gz";
            
            try
            {
                // Used in copyWarcBytes for copying bytes only
                FileInputStream warcInputStream = 
                        new FileInputStream( warcAbsolutePath );
                // Used in writeRevisitRecord for writing revisit record only
                /*FileInputStream warcRevisitInputStream = 
                        new FileInputStream( warcAbsolutePath );
                */
                
                FileOutputStream warcOutputStream = new FileOutputStream( 
                        warcDedupAbsolutePath );
                
                int preOffset = 0;
                int preLength = 0;
                int offset = 0;
                int length = 0;
                
                for( String record: warcRecords )
                {
                    String[] digestLine = record.split( " " );
                    int copyNumber = Integer.parseInt( digestLine[ 7 ] );
                    
                    if ( copyNumber > 1 )
                    {
                        offset = Integer.parseInt( digestLine[ 1 ] );
                        length = Integer.parseInt( digestLine[ 2 ] );
                        
                        /* Check whether there are bytes between current
                         *  response record and previous one. If so, we have to
                         *  copy them as they are to the new dedup_warc
                        */
                        int skippedBytesOffset = preLength + preOffset;
                        int skippedBytesLength = offset - skippedBytesOffset;
                        
                        if ( skippedBytesLength > 0 )
                        {
                            /* There are bytes in original warc should be
                             *  copied to the new one first, then add revisit
                             *  record by using JWAT or any other tools
                            */
                            copyWarcBytes( warcInputStream, warcOutputStream,
                                    skippedBytesLength );
                        }
                        
                        /* TODO: Add revisit record by calling
                         *  writeRevisitRecord method (should be implemented)
                        */
                        String refersToUri = digestLine[ 8 ];
                        String refersToDate = digestLine[ 9 ];
                        
                        // The following method is working gracefully if revisit
                        // records has been written into another file as shown
                        // below.
                        // TODO: should fix it by passing warcOutputStream instead of outputStream2
                        writeRevisitRecord( 
                                warcInputStream, warcOutputStream,
                                offset, refersToUri, refersToDate );
                        
                        /* Skip revisit record length to read the next record
                           correctly
                        */
                        warcInputStream.skip( length );
                        
                        preOffset = offset;
                        preLength = length;
                    }
                }
                
                /*
                 * Before closing the new warc, we have to check whether last
                 * record read from digests reaches to the end of original warc.
                 * 
                 * If not, we have to copy these bytes by calling copyWarcBytes.
                */
                long lengthRead = offset + length;
                long warcSize = new File(warcAbsolutePath).length();
                long lastRecordLength = warcSize - lengthRead;
                
                if (lastRecordLength > 0)
                    copyWarcBytes( warcInputStream, warcOutputStream,
                            ( int ) lastRecordLength );
                
                // Release resources for garbage collection
                warcInputStream.close();
                //warcRevisitInputStream.close();
                warcOutputStream.close();
            }
            catch ( Exception e )
            {
                System.err.println( e.getMessage() );
            }
        }
    }
    
    private void writeRevisitRecord( FileInputStream fis, FileOutputStream fos,
            int offset, String refersToUri, String refersToDate )
            throws IOException
    {    
        WarcReader wrc = new WarcReaderCompressed();
        
        // TODO: should not create a new FileInputStream object when invoking writeRevisitRecord method
        fis = new FileInputStream( currentWarc );
        fis.skip( offset );
        WarcRecord record = wrc.getNextRecordFrom( fis, offset );
        
        /* Create a new warc record and write it into output warc file.
           Don't modify existing warc record. This will not work!
        */
        
        // http header
        HttpHeader httpHeader = record.getHttpHeader();
        String httpHeaderStr = String.format( "%s %d %s\n",
                httpHeader.httpVersion, httpHeader.statusCode,
                httpHeader.reasonPhrase);

        for ( HeaderLine hl : httpHeader.getHeaderList() )
            httpHeaderStr += String.format( "%s: %s\n", hl.name, hl.value );
        
        // warc header
        WarcWriter ww = WarcWriterFactory.getWriter( fos, true );
        WarcRecord warcHeader = WarcRecord.createRecord( ww );
        
        warcHeader.header.warcTypeStr = "revisit";
        warcHeader.header.warcTargetUriStr = record.header.warcTargetUriStr;
        warcHeader.header.warcDate = record.header.warcDate;
        warcHeader.header.warcPayloadDigest = record.header.warcPayloadDigest;
        warcHeader.header.warcIpAddress = record.header.warcIpAddress;
        warcHeader.header.warcProfileStr
                = "http://netpreserve.org/warc/1.0/revisit/identical-payload-digest";
        warcHeader.header.warcRefersToTargetUriStr = refersToUri;
        warcHeader.header.warcRefersToDateStr = refersToDate;
        warcHeader.header.warcRecordIdUri = record.header.warcRecordIdUri;
        warcHeader.header.contentType = record.header.contentType;
        warcHeader.header.contentLength = ( long ) httpHeaderStr.length();
        
        // Write warc header and http header
        ww.writeHeader( warcHeader );
        ww.writePayload( httpHeaderStr.getBytes() );
        
        ww.closeRecord();
    }
    
    private void copyWarcBytes( FileInputStream fis, FileOutputStream fos,
            int length) throws IOException
    {
        while ( length > bufferSize )
        {
            byte[] data = new byte [ bufferSize ];
            
            fis.read( data, 0, bufferSize );
            fos.write( data, 0, bufferSize );
            
            length = length - bufferSize;
        }
        
        byte[] data = new byte [ length ];    
        int len = fis.read( data, 0, length );
        fos.write( data, 0, len );
    }
    
    // Using binary search to search for a specific warc record in digests file
    private void findWarcRecordsFromDigests( String key ) throws IOException
    {
        int blockSize = 8129;
        long fileSize = digests.length();
        long min = 0;
        long max = ( long ) fileSize / blockSize;
        long mid;
        String line;
    
        // find the right block
        while ( max - min > 1 ) 
        {
            mid = min + ( long ) ( ( max - min ) / 2 );
            digests.seek( mid * blockSize );
            
            if( mid > 0 )
                line = digests.readLine();      // probably a partial line
            
            line = digests.readLine();
            
            if ( key.compareTo( line ) > 0 )
                min = mid;
            else
                max = mid;
        }
        
        // find the right line
        min = min * blockSize;
        digests.seek( min );
        
        if( min > 0 ) 
            line = digests.readLine();
        
        warcRecords.clear();
        
        while ( true ) 
        {
            min = digests.getFilePointer();
            line = digests.readLine();
            
            if ( line == null )
                break;
            
            String digestLine[] = line.split( " " );
            String warcName = digestLine[ 0 ];
            
            if ( warcName.compareTo( key ) == 0 )
                warcRecords.add( line );
            else
                break;
        }
    }
    
    // Search for warc files recursively
    private void findAllWarcsRecursively( File dir )
    {
        // Check whether it is a directory and not hidden
        if ( dir.isDirectory() && !dir.isHidden() )
        {
            // Check whether I have permission to read this directory
            if( dir.canRead() )
            {
                for ( File temp : dir.listFiles() ) 
                {
                    if( temp.isFile() )
                    {
                        if( temp.getName().endsWith( ".warc.gz" ) )
                            warcFiles.add( temp.getAbsolutePath() );
                    }
                    else if ( temp.isDirectory() )
                        findAllWarcsRecursively( temp );
                }
            }
            else
                System.out.println(dir.getAbsolutePath() + "Permission Denied");
        }
        else
            System.out.println(dir.getAbsolutePath() + " is not a directory");
    }
    
    // Printing part (Used for testing purposes only)
    private void printWarcs( List<String> warcs )
    {
        if( warcs.size() != 0 )
        {
            System.out.printf( "\n# of warcs: %d\n", warcs.size() );
            
            for( String warc: warcs )
                System.out.printf( "%s\n", warc );
        }
        else
            System.out.println( "\nNo warcs found!" );
    }
    
    private void printWarcRecords( List<String> warcRecords )
    {
        if( warcRecords.size() != 0 )
        {
            System.out.printf( "\n# of warc records in digests: %d\n",
                    warcRecords.size() );
            
            for( String record: warcRecords )
                System.out.printf( "%s\n", record );
        }
        else
            System.out.println( "\nNo warc records found in digests!" );
    }
    
    // Setters and getters
    public void setBufferSize( int size )
    {
        if ( size > 0 )
            bufferSize = size;
        else
            throw new IllegalArgumentException( "Buffer size should be > 0" );
    }
    
    public int getBufferSize()
    {
        return bufferSize;
    }
}
