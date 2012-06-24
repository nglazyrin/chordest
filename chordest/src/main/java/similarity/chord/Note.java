package similarity.chord;

public enum Note {

	A,
	AD,
	B,
	C,
	CD,
	D,
	DD,
	E,
	F,
	FD,
	G,
	GD;

	public final static String DIEZ = "#";
	public final static String[] NOTE_LABELS_SHORT_EN = new String[] {
		"A", "A" + DIEZ, "B", "C", "C" + DIEZ, "D",
		"D" + DIEZ, "E", "F", "F" + DIEZ, "G", "G" + DIEZ
	};
	public final static String[] NOTE_LABELS_SHORT_RU = new String[] {
		"ля", "ля" + DIEZ, "си", "до", "до" + DIEZ, "ре",
		"ре" + DIEZ, "ми", "фа", "фа" + DIEZ, "соль", "соль" + DIEZ
	};
	public final static String LABEL_A = NOTE_LABELS_SHORT_EN[0] + " / " + NOTE_LABELS_SHORT_RU[0];
	public final static String LABEL_Ad = NOTE_LABELS_SHORT_EN[1] + " / " + NOTE_LABELS_SHORT_RU[1];
	public final static String LABEL_B = NOTE_LABELS_SHORT_EN[2] + " / " + NOTE_LABELS_SHORT_RU[2];
	public final static String LABEL_C = NOTE_LABELS_SHORT_EN[3] + " / " + NOTE_LABELS_SHORT_RU[3];
	public final static String LABEL_Cd = NOTE_LABELS_SHORT_EN[4] + " / " + NOTE_LABELS_SHORT_RU[4];
	public final static String LABEL_D = NOTE_LABELS_SHORT_EN[5] + " / " + NOTE_LABELS_SHORT_RU[5];
	public final static String LABEL_Dd = NOTE_LABELS_SHORT_EN[6] + " / " + NOTE_LABELS_SHORT_RU[6];
	public final static String LABEL_E = NOTE_LABELS_SHORT_EN[7] + " / " + NOTE_LABELS_SHORT_RU[7];
	public final static String LABEL_F = NOTE_LABELS_SHORT_EN[8] + " / " + NOTE_LABELS_SHORT_RU[8];
	public final static String LABEL_Fd = NOTE_LABELS_SHORT_EN[9] + " / " + NOTE_LABELS_SHORT_RU[9];
	public final static String LABEL_G = NOTE_LABELS_SHORT_EN[10] + " / " + NOTE_LABELS_SHORT_RU[10];
	public final static String LABEL_Gd = NOTE_LABELS_SHORT_EN[11] + " / " + NOTE_LABELS_SHORT_RU[11];
	public final static String[] NOTE_LABELS = new String[] {
		LABEL_A, LABEL_Ad, LABEL_B, LABEL_C, LABEL_Cd, LABEL_D,
		LABEL_Dd, LABEL_E, LABEL_F, LABEL_Fd, LABEL_G, LABEL_Gd
		};

	public static Note byNumber(int number) {
		int ordinal = number;
		if (ordinal < 0) {
			ordinal = 12 - ((-ordinal) % 12);
		}
		ordinal = ordinal % 12;
		return Note.values()[ordinal];
	}

	public Note next() {
		return withOffset(1);
	}

	public Note previous() {
		return withOffset(-1);
	}

	public Note withOffset(int offset) {
		if (offset < 0) {
			offset = 12 - ((-offset) % 12);
		}
		int newOrdinal = (this.ordinal() + offset) % 12;
		return Note.values()[newOrdinal];
	}

	public String getName() { return NOTE_LABELS[this.ordinal()]; }

	public String getShortName() { return NOTE_LABELS_SHORT_EN[this.ordinal()]; }

	public String toString() {
		return this.getName();
	}

	public boolean hasDiez() {
		switch (this.ordinal()) {
			case 1:
			case 4:
			case 6:
			case 9:
			case 11:
				return true;
			default:
				return false;
		}
	}

	public int offsetFrom(Note other) {
		if (other == null) {
			throw new NullPointerException("other is null");
		}
		int offset = this.ordinal() - other.ordinal();
		if (offset > 6) { offset = offset - 12; }
		else if (offset <= -6) { offset = offset + 12; }
		return offset;
	}

}
