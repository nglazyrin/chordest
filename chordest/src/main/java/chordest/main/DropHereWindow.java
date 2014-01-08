package chordest.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.iharder.filedrop.FileDrop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordRecognizer;
import chordest.chord.ChromaExtractor;
import chordest.chord.comparison.Triads;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.io.lab.LabFileWriter;
import chordest.model.Chord;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.util.PathConstants;

/**
 * Shows a frame where you can drop a wave file, wait some seconds/minutes
 * and get its sequence of chords as a text file in the same folder.
 * @author Nikolay
 *
 */
public class DropHereWindow extends Component implements Runnable {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(DropHereWindow.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DropHereWindow();
	}

	private Thread mainWindowThread;

	private JFrame frame = new JFrame();
	private JPanel centerPanel = new JPanel();
	private JProgressBar progressBar = new JProgressBar();

	public DropHereWindow() {
		createFrame();
	}

	private void createFrame() {
		frame.setTitle("Chordest - drop wave flie here");
		
		centerPanel.setBackground(Color.WHITE);
		centerPanel.setToolTipText("Drop .wav file here");
		centerPanel.setPreferredSize(new Dimension(500, 500));
		new FileDrop(centerPanel, new FileDrop.Listener() {
			public void filesDropped(File[] files) {
				if (files.length > 0) {
					File file = files[0];
					if (file.getName().endsWith(".wav")) {
						processingStarted();
						
						String wavFileName = file.getAbsolutePath();
						LOG.info(wavFileName);
						Configuration c = new Configuration();
						progressBar.setValue(10);
						String beatFileName = wavFileName.substring(0, wavFileName.lastIndexOf(".")) + PathConstants.EXT_BEAT;
						ChromaExtractor ce = new ChromaExtractor(c.process, c.template,
								new WaveFileSpectrumDataProvider(wavFileName, beatFileName, c));
						ITemplateProducer producer = new TemplateProducer(ce.getStartNote(), c.template);
						ChordRecognizer cr = new ChordRecognizer(ce.getChroma(), ce.getNoChordness(), producer);
						Chord[] chords = cr.recognize(new Triads().getOutputTypes());
						
						String labFileName = wavFileName.substring(0, wavFileName.lastIndexOf(".")) + PathConstants.EXT_LAB;
						LabFileWriter labWriter = new LabFileWriter(chords, ce.getOriginalBeatTimes());
						try {
							labWriter.writeTo(new File(labFileName));
						} catch (IOException e) {
							LOG.error("Error when writing lab file", e);
						}
						
						processingFinished();
					}
				}
			}
		});

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(centerPanel, BorderLayout.CENTER);
		frame.add(progressBar, BorderLayout.SOUTH);

		mainWindowThread = new Thread(this);
		mainWindowThread.start();
	}

	@Override
	public void run() {
		frame.pack();
		frame.setVisible(true);
	}

	private void processingStarted() {
		centerPanel.setBackground(Color.BLUE);
	}

	private void processingFinished() {
		centerPanel.setBackground(Color.WHITE);
		progressBar.setValue(0);
	}

}
