-----------
Description
-----------

This program tries to recognize chords in audio. It does not apply any learning
algorithms and therefore extractFeaturesAndTrain is missing. First the beat
detection and tuning frequency estimation are performed. Then constant-Q
transforms with very high time and frequency resolution are applied. Then the
spectrum is smoothed using median filter and the time resolution is reduced
to one frame per beat. Then CRP features are calculated from the spectrum. The
sequence of CRP vectors is then smoothed using self-similarity matrix. The
sequence of chords is built from the smoothed sequence of CRP vectors. Finally,
if there are chord sequences having same root note and different types (e.g. 
X:maj X:min X:aug X:maj), additional correction is performed to make all the
chords from this sequence have the same type (e.g. X:maj). 



------------
Installation
------------

Unpack the file 'chordest.zip' to a desired directory. It should contain
following items:
- config - directory, containing 'chordest.properties' and 'logback.xml' files
- chordest.jar - the main runnable file of chordest
- doChordID.bat - starter file for Windows
- doChordID.sh - starter file for Linux
- README.txt - this file
Starter flies contain only a 'java -jar chordest.jar' command and pass all
the command line parameters to the jar.



-------------
Configuration
-------------

All the configuration is done through the config/chordest.properties file.
Following settings are available:

- spectrum.octaves - The number of octaves spanned by constant-Q transforms.
The default value is 4.

- spectrum.notesPerOctave - The number of notes per one octave for constant-Q
transform. Should be multiple of 12, the default value is 60.

- spectrum.offsetFromF0InSemitones - Specifies the note for the first
constant-Q component. Here F0 is equal to 440 Hz. The default value is -33,
which corresponds to C3.

- spectrum.framesPerBeat - The number of analysis frames per one beat. On each
frame a constant-Q transform is performed. So the frames have substantial
overlap. The default value is 8.

- process.threadPoolSize - The number of threads used to perform constant-Q
transforms. The default value is 4.

- process.medianFilterWindow - Window size for the median filter used to smooth
the constant-Q spectrum before the further processing. The defalut value is 17.

- process.selfSimilarityTheta - The parameter that controls how many 'similar'
feature vectors should be used to smooth the chord sequence. The default value
is 0.10 - use most similar 10% of vectors.

-process.crpFirstNonZero - The number of first DCT coefficients that will be
set to zero when calculating CRP vectors. The default value is 10.



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
process.threadPoolSize key. The default value is 4. The worked threads are used
only when performing constant-Q transforms over the raw wave data. Further
(more lightweight) computations are done in a single thread.

Expected run time is 1-2 minutes per one full track. For the 2 collections
(12 albums of The Beatles and RWC Popular Music, 280 tracks in total) total run
time was about 3 hours under the following conditions:
PC, Intel Core i5 2.8 GHz, Windows 7 x64 Professional, JDK 1.6.0_29.

Peak memory footprint was about 1 GB. Memory usage depends on a track length,
so for some very long tracks in may be greater than this value. It goes down to
the level of about 300 MB when track processing is complete.

Java 1.6 or higher is required. All the dependencies are packed inside
chordest.jar.
