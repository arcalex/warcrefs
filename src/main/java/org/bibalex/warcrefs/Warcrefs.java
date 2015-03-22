/**
 *
 * @author Mohamed Elsayed
 */

package org.bibalex.warcrefs;

import java.io.IOException;

public class Warcrefs 
{
    public static void main( String[] args ) throws IOException
    {
        int bufferSize = 8129;                               // args[0]
        String digestsPath = "/home/msm/warcrefs/digests";   // args[1]
        String rootDirPath = "/home/msm/warcrefs";           // args[2]
        
        try
        {
            Deduplicator deduplicator = new Deduplicator( bufferSize );
            
            // Command the deduplicator to deduplicate WARC files under the
            // directory at args[2] using the digests in file args[1].
            // TODO: should handle IOException and FileNotFound Exception
            deduplicator.deduplicate( digestsPath, rootDirPath );
        }
        catch( IllegalArgumentException e )
        {
            System.out.println( e.getMessage() );
        }
    }
}
