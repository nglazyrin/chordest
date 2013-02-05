package chordest.chord;

import junit.framework.Assert;

import org.junit.Test;

import chordest.model.Note;

public class NoteTest {

	@Test
	public void test_33() {
		Assert.assertEquals(Note.C, Note.byNumber(-33));
	}

	@Test
	public void testOffset() {
		Assert.assertEquals(-1, Note.C.offsetFrom(Note.CD));
		Assert.assertEquals(1, Note.C.offsetFrom(Note.B));
		Assert.assertEquals(2, Note.D.offsetFrom(Note.C));
	}

	@Test
	public void testWithOffset() {
		Assert.assertEquals(Note.C, Note.CD.withOffset(-1));
		Assert.assertEquals(Note.B, Note.CD.withOffset(-2));
		Assert.assertEquals(Note.AD, Note.CD.withOffset(-3));
		Assert.assertEquals(Note.A, Note.CD.withOffset(-4));
		Assert.assertEquals(Note.C, Note.CD.withOffset(-121));
		Assert.assertEquals(Note.C, Note.CD.withOffset(11));
		Assert.assertEquals(Note.C, Note.B.withOffset(1));
		Assert.assertEquals(Note.D, Note.C.withOffset(2));
		Assert.assertEquals(Note.D, Note.C.withOffset(122));
	}

}
