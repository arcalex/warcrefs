/**
 * @author Mohamed Elsayed
 */

package org.bibalex.warcrefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class Deduplicator 
{
    private List<String> warcFiles = new ArrayList<String>();
    private List<String> warcRecords = new ArrayList<String>();
    private RandomAccessFile digests;
    private int bufferSize = 8129;       // default length (8k) that will be copied (written) to dedup_warc 
    
    public Deduplicator()
    {
        
    }
    
    public Deduplicator(int size)
    {
        bufferSize = size;
    }
    
    public void deduplicate(String digestsPath, String rootDirPath) throws FileNotFoundException, IOException
    {
        // Initialization phase
        digests = new RandomAccessFile(digestsPath, "r");          
        File rootDir = new File (rootDirPath);                     
        
        // Processing phase
        getAllWarcsRecursively(rootDir);
        
        int count = 0;
        for (String warc : warcFiles)
        {
            // Please remove this condition after finishing writeWarcDedup implementation 
            if (count == 2)
                break;
            
            writeWarcDedup(warc);
            count ++;
        }
        
        // Terminating phase
        printWarcs(warcFiles);
        //printWarcRecords(warcRecords);

    }
    
    private void writeWarcDedup(String warcAbsolutePath) throws IOException
    {
        String warcBasename = warcAbsolutePath.substring(warcAbsolutePath.lastIndexOf("/") + 1);
        String warcDirectory = warcAbsolutePath.substring(0, warcAbsolutePath.lastIndexOf("/") + 1);
        
        getWarcRecordsFromDigests(warcBasename);
        
        if (warcRecords.size() != 0)
        {
            String warcDedupAbsolutePath = warcDirectory + warcBasename.substring(0, warcBasename.length() - 8) + "_dedup.warc.gz";
            
            File warcFile = null;
            FileInputStream warcInputStream = null;
            FileOutputStream warcOutputStream = null;
            
            try
            {
                warcInputStream = new FileInputStream(warcAbsolutePath);
                warcFile = new File(warcDedupAbsolutePath);
                warcOutputStream = new FileOutputStream(warcFile);

                int preOffset = 0;
                int preLength = 0;
                int offset = 0;
                int length = 0;
                
                for(String record: warcRecords)
                {
                    String[] digestLine = record.split(" ");
                    int copyNumber = Integer.parseInt(digestLine[7]);
                    
                    if (copyNumber > 1)
                    {
                        offset = Integer.parseInt(digestLine[1]);
                        length = Integer.parseInt(digestLine[2]);
                        
                        // Check whether there are bytes between current response record and previous one.
                        // If so, we have to copy them as they are to the new dedup_warc
                        int skippedBytesOffset = preLength + preOffset;
                        int skippedBytesLength = offset - skippedBytesOffset;
                        
                        if (skippedBytesLength > 0)
                        {
                            // There are bytes in original warc should be copied to the new one first,
                            // then add revisit record by using JWAT or any other tools
                            copyWarcBytes(warcInputStream, warcOutputStream, skippedBytesLength);
                        }
                        
                        // TODO: Add revisit record by calling addRevisitRecord method (should be implemented)
                        String refersToUri = digestLine[8];
                        String refersToDate = digestLine[9];
                        writeRevisitRecord(warcInputStream, warcOutputStream, length, refersToUri, refersToDate);
                        
                        preOffset = offset;
                        preLength = length;
                    }
                }
                
                // Before closing the new warc, we have to check whether last record read from digests reaches to the end of original warc
                // If not, we have to copy these bytes by calling copyWarcBytes
                long lengthRead = offset + length;
                long warcSize = new File(warcAbsolutePath).length();
                long lastRecordLength = warcSize - lengthRead;
                
                if (lastRecordLength > 0)
                    copyWarcBytes(warcInputStream, warcOutputStream, (int) lastRecordLength);
                
                warcInputStream.close();
                warcOutputStream.flush();
                warcOutputStream.close();
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
        }
    }
    
    private void writeRevisitRecord(FileInputStream fis, FileOutputStream fos, int length, String refersToUri, String refersToDate) throws IOException
    {
        // Line below is just to continue reading from the right offset by skipping record which has copyNumber = 2.
        fis.skip(length);
    }
    
    private void copyWarcBytes(FileInputStream fis, FileOutputStream fos, int length) throws IOException
    {
        while (length > bufferSize)
        {
            byte[] data = new byte [bufferSize];
            
            fis.read(data, 0, bufferSize);
            fos.write(data, 0, bufferSize);
            
            length = length - bufferSize;
        }
        
        byte[] data = new byte [length];    
        fis.read(data, 0, length);
        fos.write(data, 0, length);
        
    }
    
    private void getWarcRecordsFromDigests(String key) throws IOException
    {
        int blockSize = 8129;
        long fileSize = digests.length();
        long min = 0;
        long max = (long) fileSize / blockSize;
        long mid;
        String line;
    
        // find the right block
        while (max - min > 1) 
        {
            mid = min + (long)((max - min) / 2);
            digests.seek(mid * blockSize);
            
            if(mid > 0)
                line = digests.readLine(); // probably a partial line
            
            line = digests.readLine();
            
            if (key.compareTo(line) > 0)
                min = mid;
            else
                max = mid;
        }
        
        // find the right line
        min = min * blockSize;
        digests.seek(min);
        
        if(min > 0) 
            line = digests.readLine();
        
        warcRecords.clear();
        
        while (true) 
        {
            min = digests.getFilePointer();
            line = digests.readLine();
            
            if (line == null)
                break;
            
            String digestLine[] = line.split(" ");
            String warcName = digestLine[0];
            
            if (warcName.compareTo(key) == 0)
                warcRecords.add(line);
            else if (warcName.compareTo(key) > 0)
                break;
        }
    }
    
    // Search for warc files recursively
    private void getAllWarcsRecursively (File dir)
    {
        // Check whether it is a directory
        if(dir.isDirectory())
        {
            // Check whether I have permission to read this directory
            if(dir.canRead())
            {
                // Processing
                for (File temp : dir.listFiles()) 
                {
                    if(temp.isFile())
                    {
                        // Base case
                        if(temp.getName().endsWith(".warc.gz"))
                            warcFiles.add(temp.getAbsolutePath());
                    }
                    else if (temp.isDirectory())
                    {
                        // Recursive call
                        getAllWarcsRecursively(temp);
                    }
                }
            }
            else
                System.out.println(dir.getAbsolutePath() + "Permission Denied");
        }
        else
            System.out.println(dir.getAbsolutePath() + " is not a directory");
    }
    
    // Printing part (Used for testing purposes only)
    private void printWarcs (List<String> warcs)
    {
        int count = warcs.size();
        
        if(count != 0)
        {
            System.out.printf("\n# of warcs: %d\n", count);
            
            for(String warc: warcs)
                System.out.printf("%s\n", warc);
        }
        else
            System.out.println("\nNo warcs found!");
    }
    
    private void printWarcRecords (List<String> warcRecords)
    {
        int count = warcRecords.size();
        
        if(count != 0)
        {
            System.out.printf("\n# of warc records in digests: %d\n", count);
            
            for(String record: warcRecords)
                System.out.printf("%s\n", record);
        }
        else
            System.out.println("\nNo warc records found in digests!");
    }
    
    // Setters and getters
    public void setBufferSize (int size)
    {
        bufferSize = size;
    }
    
    public int getBufferSize ()
    {
        return bufferSize;
    }
}
