# warcrefs
Web archive deduplication tools for identifying duplicates and converting them to references in a web archive collection after crawl time. Warcrefs is implemented in JAVA.

The warcrefs tool takes as input a list of WARC files but also now has access to post-processed hash manifest lines for records in the files it is to operate on.
warcrefs iterates through each WARC file in the input and also concurrently through corresponding lines in the post-processed hash manifest.
Each record with a copy number greater than 1 in the corresponding manifest line is converted into a revisit record, where WARC-Refers-To-Target-URI and WARC-Refers-To-Date in the record headers are set to the URI and date, respectively, of the original resource, and payload headers are transferred as-is into the revisit record.
Otherwise, if the copy number is 1, or if no corresponding line is in the manifest, the record is not altered.

warcrefs uses the Java Web Archive Toolkit (JWAT) for WARC file IO.
warcrefs can be configured to rewrite files in-place or save to a new file.
warcrefs is to be run on all hosts in the data store.
The post-processed hash manifest is to be split across the hosts such that each host only has lines corresponding to records in WARC files on the host.
Further, as the absence of a manifest line for a record implies the record is not a duplicate, lines where the copy number is 1 are to be omitted to reduce the amount of manifest data warcrefs has to process.
