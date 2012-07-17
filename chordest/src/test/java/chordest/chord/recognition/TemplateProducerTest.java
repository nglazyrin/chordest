package chordest.chord.recognition;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import chordest.chord.Chord;
import chordest.chord.Note;
import chordest.chord.recognition.TemplateProducer;


public class TemplateProducerTest {

	@Test
	public void testTemplateProducerSmoke() {
		TemplateProducer p = new TemplateProducer(Note.A, true);
		double[] template = p.getTemplateFor(Chord.major(Note.A));
		Assert.assertEquals(12, template.length);
		System.out.println(Arrays.toString(template));
		template = p.getTemplateFor(Chord.major(Note.D));
		System.out.println(Arrays.toString(template));
	}

}
