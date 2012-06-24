package similarity.chord.recognition;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import similarity.chord.Chord;
import similarity.chord.Note;

public class TemplateProducerTest {

	@Test
	public void testTemplateProducerSmoke() {
		TemplateProducer p = new TemplateProducer(Note.A);
		double[] template = p.getTemplateFor(Chord.major(Note.A));
		Assert.assertEquals(12, template.length);
		System.out.println(Arrays.toString(template));
		template = p.getTemplateFor(Chord.major(Note.D));
		System.out.println(Arrays.toString(template));
	}

}
