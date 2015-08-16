/*
* Copyright 2015 Bibliotheca Alexandrina.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.bibalex.warcrefs;

import java.io.IOException;

/**
 *
 * @author Mohamed Elsayed
 */

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
