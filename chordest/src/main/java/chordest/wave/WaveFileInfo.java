package chordest.wave;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class WaveFileInfo {

	public final int samplingRate;
	public final int channels;
	public final double seconds;

	public final Exception exception;

	public WaveFileInfo(String fileName) {
		this(new File(fileName));
	}

	public WaveFileInfo(File file) {
		int srTemp = -1;
		int cTemp = -1;
		double sTemp = -1;
		Exception ex = null;
		try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
			AudioFormat format = stream.getFormat();
			int frames = (int) stream.getFrameLength();
			cTemp = format.getChannels();
			srTemp = (int) format.getSampleRate();
			sTemp = frames * 1.0 / srTemp;
		} catch (UnsupportedAudioFileException | IOException e) {
			ex = e;
		}
		this.samplingRate = srTemp;
		this.channels = cTemp;
		this.seconds = sTemp;
		this.exception = ex;
	}
}
