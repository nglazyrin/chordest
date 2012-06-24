/**
 *
 */
package similarity.transform;

import java.util.concurrent.Callable;

import similarity.wave.Buffer;


/**
 * @author Nikolay
 *
 */
public interface ITransform extends Callable<Buffer> {

	public enum Direction { inverse, direct };
}
