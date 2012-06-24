package similarity.transform;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import similarity.wave.Buffer;
import similarity.wave.ReadOnlyBuffer;

public abstract class AbstractTransform implements ITransform {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractTransform.class);
	
	private CountDownLatch countDownLatch;

	public AbstractTransform(CountDownLatch latch) {
		this.countDownLatch = latch;
	}

	@Override
	public Buffer call() {
		try {
			return transform();
		} catch (Exception e) {
			LOG.error("Error during transform", e);
			return ReadOnlyBuffer.newEmptyInstance();
		} finally {
			this.countDownLatch.countDown();
		}
	}

	abstract Buffer transform();

}
