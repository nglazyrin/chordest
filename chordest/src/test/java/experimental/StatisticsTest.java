package experimental;
import java.io.File;

import ncsa.hdf.object.h5.H5File;
import similarity.hdf5.Hdf5Getters;


public class StatisticsTest {

	public static final String HDF_ROOT_DIR = "D:\\Study\\sci-work\\MillionSongDataset\\millionsongsubset_full\\MillionSongSubset";

	public static final String HDF_DATA_DIR = HDF_ROOT_DIR + "\\data";

	private static void processDirectory(File[] files, IFileProcessor processor) {
	    for (File file : files) {
	        if (file.isDirectory()) {
	        	processDirectory(file.listFiles(), processor);
	        } else {
	            processor.process(file);
	        }
	    }
	}

	public static void main(String[] args) {
		File hdfDataDir = new File(HDF_DATA_DIR);
		IFileProcessor processor = new IFileProcessor() {
			public void process(File file) {
				if (file == null) { return; }
				if (! file.getName().endsWith(".h5")) { return; }
				H5File h5 = Hdf5Getters.hdf5_open_readonly(file.getAbsolutePath());
				try {
					double danceability = Hdf5Getters.get_danceability(h5);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					Hdf5Getters.hdf5_close(h5);
				}
			}
		};
	}

}
