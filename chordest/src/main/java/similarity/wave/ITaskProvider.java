package similarity.wave;

public interface ITaskProvider extends Runnable {

	/**
	 * Request the next task (Buffer) to process
	 * @return Buffer or null if nothing to process yet
	 */
	public Buffer poll() throws InterruptedException;

}
