package experimental;
import java.util.List;

import junit.framework.Assert;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.h5.H5File;

import org.jfree.data.xy.XYZDataset;

import chordest.gui.JFreeChartUtils;
import chordest.gui.StructureMapGenerator;
import experimental.hdf5.Hdf5Getters;



public class HdfReadTest {

	private static final String FILENAME = "src/main/resources/TRAAAAW128F429D538.h5";

	public void getHdfSize() {
		int fileId = -1;
		try {
			fileId = H5.H5Fopen(FILENAME, HDFConstants.DFACC_READ, 0);
			System.out.println(H5.H5Fget_filesize(fileId));
		} catch (HDF5LibraryException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} finally {
			if (fileId != -1) {
				try {
					H5.H5Fclose(fileId);
				} catch (HDF5LibraryException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		H5File file = Hdf5Getters.hdf5_open_readonly(FILENAME);
		try {
			System.out.println(Hdf5Getters.get_track_7digitalid(file));
//			System.out.println(Arrays.toString(Hdf5Getters.get_sections_confidence(file)));
			double[] timbre = Hdf5Getters.get_segments_timbre(file);
//			double[] pitches = Hdf5Getters.get_segments_pitches(file);
			double[] times = Hdf5Getters.get_segments_start(file);
//			double[] loudness = Hdf5Getters.get_segments_loudness_max(file);
//			XYZDataset pitchesDataset = JFreeChartUtils.toXYZDataset(times, pitches);
//			JFreeChartUtils.visualize("Pitches", "Time", "Pitch class", pitchesDataset);
//			XYZDataset timbreDataset = JFreeChartUtils.toXYZDataset(times, timbre);
//			JFreeChartUtils.visualize("Timbre", "Time", "Timbre class", timbreDataset);
//			XYDataset loudnessDataset = JFreeChartUtils.toXYDataset(times, loudness);
//			JFreeChartUtils.visualize("Loudness", "Time", "dB", loudnessDataset);
			double[] distances = StructureMapGenerator.getStructureMap(timbre);
			XYZDataset distancesDataset = StructureMapGenerator.toXYZDataset(times, distances);
			JFreeChartUtils.visualize("Self-similarity", "Time", "Time", distancesDataset, 600, 600);
//			int cols = 12;
//			int rows = pitches.length / cols;
//			for (int row = 0; row < rows; row++) {
//				System.out.print(times[row] + ": [");
//				for (int col = 0; col < cols; col++) {
//					System.out.print(pitches[row * cols + col] + ", ");
//				}
//				System.out.println("]");
//			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		} finally {
			Hdf5Getters.hdf5_close(file);
		}
	}

	public void readHdf() {
		FileFormat fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
		if (fileFormat == null) {
            System.err.println("Cannot find HDF5 FileFormat.");
            return;
        }
		
		try {
			H5File testFile = (H5File) fileFormat.createInstance(FILENAME, FileFormat.READ);
			testFile.open();
			
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)testFile.getRootNode()).getUserObject();
			printGroup(root, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * Recursively print a group and its members.
     * @throws Exception
     */
    private static void printGroup(Group g, String indent) throws Exception {
        if (g == null) { return; }
        indent += "    ";

        List<?> members = g.getMemberList();
        for (Object o : members) {
            HObject obj = (HObject) o;
            System.out.println(indent + obj.getName());
            if (obj instanceof Group) {
                printGroup((Group)obj, indent);
            } else if (obj instanceof Dataset) {
            	printDataset((Dataset) obj, indent);
            }
        }
    }

	private static void printDataset(Dataset obj, String indent) {
		try {
			obj.read();
			long[] dimSizes = obj.getDims();
			indent += "    ";
			System.out.println(indent + "Datatype: " + obj.getDatatype().getDatatypeDescription());
			if (dimSizes.length > 1) {
				System.out.println(indent + "Width: " + obj.getWidth());
				System.out.println(indent + "Height: " + obj.getHeight());
			}
			for (int i = 0; i < dimSizes.length; i++) {
				System.out.println(indent + i + ": " + dimSizes[i]);
			}
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
