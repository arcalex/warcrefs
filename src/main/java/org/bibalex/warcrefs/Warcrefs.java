/*
* Copyright (C) 2015 Bibliotheca Alexandrina
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or (at
* your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
