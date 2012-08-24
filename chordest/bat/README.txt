-----------
Description
-----------

This program tries to recognize chords in audio. It does not apply any learning
algorithms and therefore extractFeaturesAndTrain is missing.


-------------------
Command line format
-------------------

As it is defined in http://www.music-ir.org/mirex/wiki/2012:Audio_Chord_Estimation:

doChordID.sh "/path/to/testFileList.txt" "/path/to/scratch/dir" "/path/to/results/dir"

testFileList should contain a list of wave file names, one file name per line.
Scratch dir will contain full program log.
Results dir will contain resulting .txt files with chord annotations.


--------------------------------
Hardware & software requirements
--------------------------------

Number of worker threads can be specified in chordest.properties under the
process.threadPoolSize key. The default value is 4. Plus 1 main thread, so
5 threads are used in total.

Expected run time is 1-2 minutes per one full track. On 2 collections
(12 albums of The Beatles and RWC Popular Music, 280 tracks in total) the run
time was about 3.5 hours.
Peak memory footprint was about 900 MB.

Java 1.6 or higher is required. All the dependencies are packed inside
chordest.jar.