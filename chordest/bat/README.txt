08 September 2013
Nikolay Glazyrin
nglazyrin@gmail.com

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
X:maj X:min X:maj), additional correction is performed to make all the chords
from this sequence have the same type (e.g. X:maj).



------------
Installation
------------

Unpack the file 'chordest.zip' to a desired directory. It should contain
following items:
- config - directory, containing 'chordest.properties' and 'logback.xml' files
- vamp - directory, containing vamp simple host runnables for Windows/Linux
- chordest.jar - the main runnable file of chordest
- doChordID.bat - starter file for Windows
- doChordID.sh - starter file for Linux
- README.txt - this file
Starter flies contain only a 'java -jar chordest.jar' command and pass all
the command line parameters to the jar.



-------------
Configuration
-------------

All the configuration is done through the config/chordest.properties file. The
default values for settings are provided in comments in the file. The following
settings are available:

- spectrum.octaves - The number of octaves spanned by constant-Q transforms.

- spectrum.notesPerOctave - The number of notes per one octave for constant-Q
transform. Should be multiple of 12.

- spectrum.offsetFromF0InSemitones - Specifies the note for the first
constant-Q component. Here F0 is equal to 440 Hz.

- spectrum.framesPerBeat - The number of analysis frames per one beat. On each
frame a constant-Q transform is performed. So the frames have substantial
overlap.

- process.threadPoolSize - The number of threads used to perform constant-Q
transforms.

- process.medianFilterWindow - Window size for the median filter used to smooth
the constant-Q spectrum before the further processing.

- process.selfSimilarityTheta - The parameter that controls how many 'similar'
feature vectors should be used to smooth the chord sequence.

-process.crpFirstNonZero - The number of first DCT coefficients that will be
set to zero when calculating CRP vectors.

- pre.vampHostPath - The path to the vamp-simple-host executable file. It will
be directly passed to the system call.

- pre.estimateTuningFrequency - Whether to perform tuning frequency estimation
prior to spectrum calculation or not. 



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

In short:

- 1 GB of memory, Intel Core2Duo or higher (Core i5/i7 is preferable)
- JDK 1.7.0_02 or higher, X11 (for beatroot), vamp-simple-host.
- Estimated run time: 1-2 minutes per track, no training is required.

Details:

Expected run time is 1-2 minutes per one full track. For the 2 collections
(Isophonics and RWC Popular Music, 318 tracks in total) total run time was
between 5 and 6 hours under the following conditions:
PC, Intel Core i5 2.8 GHz, Windows 7 x64 Professional, JDK 1.7.0_05.

Peak memory footprint was about 1 GB. Memory usage depends on a track length,
so for some very long tracks in may be greater than this value. It goes down to
the level of about 300 MB when track processing is complete.

Java 1.7 or higher is required. All the dependencies are packed inside
chordest.jar.

NOTE: beatroot jar requires swing classes, so the X11 should be installed on
UNIX system (there should be no such issues under Windows). Unfortunately this
dependency cannot be excluded easily from beatroot without rewriting many
classes (which is problematic since I'm not the author of beatroot).
Chordest can also work with qm-vamp-plugins:qm-barbeattracker:beats plugin. For
this connection to work the "pre.vampHostPath" setting should point to the
runnable file for vamp-simple-host. It will be called as follows:

pre.vampHostPath qm-vamp-plugins:qm-barbeattracker:beats wavFilePath -o beatFilePath

where beatFilePath points to a file within the scratch directory.

Number of worker threads can be specified in chordest.properties under the
process.threadPoolSize key. The default value is 4. The worked threads are used
only when performing constant-Q transforms over the raw wave data. Further
(more lightweight) computations are done in a single thread (except for
DCT/IDCT for CRP features calculation, which is also done in 4 threads).
