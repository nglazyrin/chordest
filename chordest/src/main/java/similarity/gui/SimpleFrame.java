package similarity.gui;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class SimpleFrame extends Component implements Runnable {

	private static final long serialVersionUID = 7510797003160836887L;

	protected JFrame frame;
	protected JScrollPane scrollPane;

	public SimpleFrame(Component component) {
		super();
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new GridLayout());
		scrollPane = new JScrollPane();
		scrollPane.setViewportView(component);
		frame.add(scrollPane);
	}

	public SimpleFrame(Component component, String title) {
		this(component);
		frame.setTitle(title);
	}

	public void run() {
		frame.pack();
		frame.setVisible(true);
	}

}
