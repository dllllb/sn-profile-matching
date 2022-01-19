Large-Scale Parallel Matching of Social Network Profiles
===========================================

This project is a companion of the research paper on [Large-Scale Parallel Matching of Social Network Profiles](https://link.springer.com/chapter/10.1007/978-3-319-26123-2_27), [PDF](https://arxiv.org/abs/1911.06861).
A profile matching algorithm takes as input a user profile of one social network and returns, if existing, the profile of the same person in another social network. Such methods have immediate applications in Internet marketing, search, security, and a number of other domains, which is why this topic saw a recent surge in popularity.
In this paper, we present a user identity resolution approach that uses minimal supervision and achieves a precision of 0.98 at a recall of 0.54. Furthermore, the method is computationally efficient and easily paral- lelizable. We show that the method can be used to match Facebook, the most popular social network globally, with VKontakte, the most popular social network among Russian-speaking users.

If you use this code  please cite it as following:

```

@inproceedings{panchenko2015large,
  title={Large-Scale Parallel Matching of Social Network Profiles},
  author={Panchenko, Alexander and Babaev, Dmitry and Obiedkov, Sergei},
  booktitle={International Conference on Analysis of Images, Social Networks and Texts},
  pages={275--285},
  year={2015},
  organization={Springer}
}
```

Instructions to build and execute sample code
---------------------------------------------

```sh
mvn package

java -cp target/sn-profile-matching.jar \
  -Dprofile.index.location=test-index \
  -Dprofile.source.file=data/index-data-sample.jsonl \
  com.digsolab.papers.matching.FbProfileFriendsJsonIndexBuilder

java -cp target/sn-profile-matching.jar \
  -Dprofile.friends.index=test-index \
  -Dprofile.friends.list=data/match-data-sample.jsonl \
  com.digsolab.papers.matching.MatchProfileByFriendsTool
```

Executables
-----------

`com.digsolab.papers.matching.FbProfileFriendsJsonIndexBuilder` is a tool to create Lucene index for user profiles,
which is required to perform matching.

**Parameters:**

- profile.index.location: a directory to write index files.
- profile.source.file: a JSONL file (a JSON per line text file) with user profiles.

Input file should contain user first / last name and user friends first last names in JSON format.
See example file: *data/index-data-sample.jsonl*.

`com.digsolab.papers.matching.MatchProfileByFriendsTool` is a profile matching tool.

**Parameters:**

- profile.friends.index: a directory with profiles index files.
- profile.friends.list: a JSONL file (a JSON per line text file) with user profiles.

Input file should contain user first / last name and user friends first last names in JSON format.
See example file: *data/match-data-sample.jsonl*.

Output data is printed to system out in tab-separated values (TSV) format
and contains the following fields:
profile ID, match candidate ID, profile name, match candidate name, match confidence.

**Sample output:**

```
1	100000000004370	volodya shemenkov	vladimir shemenkov	2.0
1	100000000004371	volodya shemenkov	vladimir shemenkov	0.0
```

`com.digsolab.papers.matching.MatchProfilesByFriendsJob` is a Hadoop job for large-scale matching.

Hadoop job execution
--------------------

```sh
hadoop jar $ASSEMBLY_JAR_PATH \
  com.digsolab.papers.matching.MatchProfilesByFriendsJob \
  -Dmapreduce.user.classpath.first=true \
  -Dmapred.input.dir=$INPUT_PROFILES_JSONL_FILES_WILDCARD \
  -Dmapred.cache.archives=$ARCHIVED_INDEX_FILES \
  -Dmapred.output.dir=$OUTPUT_PATH
```

- ASSEMBLY_JAR_PATH: local filesystem path to assembly jar produced by `mvn package` command.
- INPUT_PROFILES_JSONL_FILES_WILDCARD: a HDFS path to input profiles JSONL files, may contain wildcard symbols.
- ARCHIVED_INDEX_FILES: a HDFS path to .tar.gz archive with Lucene profiles index to distribute across worker hosts.
- OUTPUT_PATH: a directory on HDFS to write output files.

Input files should contain user first / last name and user friends first last names in JSON format.
See example file: *data/match-data-sample.jsonl*

Hadoop job output files are in tab-separated values (TSV) format
and contain the following fields:
profile ID, match candidate ID, profile name, match candidate name, match confidence.

Additional data files
---------------------

*src/main/resources/fb-name-tf-top10k.tsv*: frequency of the most top 10 thousands first names
calculated on Russian subset of the Facebook profiles

src/main/resources/fb-2name-tf-top10k.tsv: frequency of the most top 10 thousands last names
calculated on Russian subset of the Facebook profiles

src/main/resources/name-aliases-known-vb-vk-pairs-vk90k.txt: alternative spellings of russian names
calculated from 90 thousands Vkontakte profiles with links to Facebook profiles

Sample data from real social networks
-------------------------------------

Samples of real data from FB and VK social networks are also provided in the data directory (stored in Git LFS):

- 4429 Facebook profiles with their friends names
- 21124 Vkontakte profiles with their friends names
- 577 linked profiles that exist in the previous two datasets

### How to execute profile matching on sample data:

```sh
mvn package

java -cp target/sn-profile-matching.jar \
      -Dprofile.index.location=test-index \
      -Dprofile.source.file=fb-friend-names-hse.txt \
      com.digsolab.papers.matching.FbProfileFriendsWallIndexBuilder

java -cp target/sn-profile-matching.jar \
      -Dprofile.friends.index=hse-index \
      -Dprofile.friends.list=vk-friend-names-hse.txt \
      com.digsolab.papers.matching.MatchProfileByFriendsTool
```
