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

import chordest.chord.ChordExtractor;
import chordest.configuration.Configuration;
import chordest.io.lab.LabFileWriter;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.util.PathConstants;

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
						ChordExtractor ce = new ChordExtractor(c, new WaveFileSpectrumDataProvider(wavFileName, c));
						
						String labFileName = wavFileName.substring(0, wavFileName.lastIndexOf(".")) + PathConstants.EXT_LAB;
						LabFileWriter labWriter = new LabFileWriter(ce);
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
