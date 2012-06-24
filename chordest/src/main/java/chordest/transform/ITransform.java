/**
 *
 */
package chordest.transform;

import java.util.concurrent.Callable;

import chordest.wave.Buffer;



/**
 * @author Nikolay
 *
 */
public interface ITransform extends Callable<Buffer> {

	public enum Direction { inverse, direct };
}
