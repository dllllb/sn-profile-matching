Social network profile matching code sample
===========================================

This project is a companion of the Large-Scale Parallel Matching of Social Network Profiles paper.

It is based on work made in Digital Society Laboratory (digsolab.com).

Build and execute instructions
------------------------------

    mvn package

    java -cp target/sn-profile-matching.jar \
      -Dprofile.index.location=test-index \
      -Dprofile.source.file=data/index-data-sample.jsonl \
      com.digsolab.euler.intprof.matching.FbProfileFriendsJsonIndexBuilder

    java -cp target/sn-profile-matching.jar \
      -Dprofile.friends.index=test-index \
      -Dprofile.friends.list=data/match-data-sample.jsonl \
      com.digsolab.euler.intprof.matching.MatchProfileByFriendsTool
