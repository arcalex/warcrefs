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
        int bufferSize = Integer.parseInt( args[ 0 ] ); // 8129
        String digestsPath = args[ 1 ];                 // path to digests file
        String rootDirPath = args[ 2 ];                 // warcs directory
        
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
