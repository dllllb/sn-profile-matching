Social network profile matching code sample
===========================================

This project is a companion of the Large-Scale Parallel Matching of Social Network Profiles paper.

It is based on work made in [Digital Society Laboratory](http://digsolab.com).

Instructions to build and execute sample code
---------------------------------------------

    mvn package

    java -cp target/sn-profile-matching.jar \
      -Dprofile.index.location=test-index \
      -Dprofile.source.file=data/index-data-sample.jsonl \
      com.digsolab.euler.intprof.matching.FbProfileFriendsJsonIndexBuilder

    java -cp target/sn-profile-matching.jar \
      -Dprofile.friends.index=test-index \
      -Dprofile.friends.list=data/match-data-sample.jsonl \
      com.digsolab.euler.intprof.matching.MatchProfileByFriendsTool

Executables
-----------

`com.digsolab.euler.intprof.matching.FbProfileFriendsJsonIndexBuilder` a tool to create Lucene index for user profiles,
which is required to perform matching.

**Parameters:**

- profile.index.location: a directory to write index files.
- profile.source.file: a JSONL file (a JSON per line text file) with user profiles.

Input file should contain user first / last name and user friends first last names in JSON format.
See example file: *data/index-data-sample.jsonl*.

`com.digsolab.euler.intprof.matching.MatchProfileByFriendsTool` profile matching tool.

**Parameters:**

- profile.friends.index: a directory with profiles index files.
- profile.friends.list: a JSONL file (a JSON per line text file) with user profiles.

Input file should contain user first / last name and user friends first last names in JSON format.
See example file: *data/match-data-sample.jsonl*.

Output data is printed to system out in tab-separated values (TSV) format
and contains the following fields:
profile ID, match candidate ID, profile name, match candidate name, match confidence.

**Sample output:**

    1	100000000004370	volodya shemenkov	vladimir shemenkov	2.0
    1	100000000004371	volodya shemenkov	vladimir shemenkov	0.0

`com.digsolab.euler.intprof.matching.MatchProfilesByFriendsJob` Hadoop job for large-scale matching.

Hadoop job execution
--------------------

    hadoop jar $ASSEMBLY_JAR_PATH \
      com.digsolab.euler.intprof.matching.MatchProfilesByFriendsJob \
      -Dmapreduce.user.classpath.first=true \
      -Dmapred.input.dir=$INPUT_PROFILES_JSONL_FILES_WILDCARD \
      -Dmapred.cache.archives=$ARCHIVED_INDEX_FILES \
      -Dmapred.output.dir=$OUTPUT_PATH

- ASSEMBLY_JAR_PATH: local filesystem path to assembly jar produced by `mvn package` command.
- INPUT_PROFILES_JSONL_FILES_WILDCARD: a HDFS path to input profiles JSONL files, may contain wildcard symbols.
- ARCHIVED_INDEX_FILES: a HDFS path to .tar.gz archive with Lucene profiles index to distribute across worker hosts.
- OUTPUT_PATH: a directory on HDFS to write output files.

Input files should contain user first / last name and user friends first last names in JSON format.
See example file: *data/match-data-sample.jsonl*

Hadoop job output files are in tab-separated values (TSV) format
and contain the following fields:
profile ID, match candidate ID, profile name, match candidate name, match confidence.