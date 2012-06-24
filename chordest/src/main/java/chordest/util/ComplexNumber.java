package chordest.util;

import java.io.Serializable;


public class ComplexNumber implements Serializable {

	private static final long serialVersionUID = 1L;
	private double real;
	private double imaginary;

	public ComplexNumber(double re, double im) {
		super();
		this.real = re;
		this.imaginary = im;
	}

	public ComplexNumber(ComplexNumber cn)
	{
		this(cn.re(), cn.im());
	}

	public double re()
	{
		return this.real;
	}

	public double im()
	{
		return this.imaginary;
	}

	public String toString() {
		if (imaginary<0)
			return (Double.toString(real)+Double.toString(imaginary)+'i');
		return (Double.toString(real)+'+'+Double.toString(imaginary)+'i');
	}

	public ComplexNumber add(ComplexNumber operand) {
		return new ComplexNumber(real + operand.re(), imaginary + operand.im());
	}

	public ComplexNumber sub(ComplexNumber operand) {
		return new ComplexNumber(real - operand.re(), imaginary - operand.im());
	}

	public ComplexNumber mul(ComplexNumber operand) {
		double op_re = operand.re();
		double op_im = operand.im();
		return new ComplexNumber(
				real * op_re - imaginary * op_im, real * op_im + imaginary * op_re);
	}

	public ComplexNumber mul(double operand)
	{
		return new ComplexNumber(real * operand, imaginary * operand);
	}

	public ComplexNumber div(ComplexNumber operand)
			throws IllegalArgumentException {
		double op_re = (operand).re();
		double op_im = (operand).im();
		double divisor = op_re * op_re + op_im * op_im;
		if (divisor == 0) {
			throw new IllegalArgumentException("divisior = 0");
		}
		return new ComplexNumber(
				(real * op_re + imaginary * op_im) / divisor,
				(imaginary * op_re - real * op_im) / divisor);
	}

	public ComplexNumber conj()
	{
		return new ComplexNumber(this.real, -this.imaginary);
	}

	public ComplexNumber clone()
	{
		return new ComplexNumber(this.real, this.imaginary);
	}

	public double abs()
	{
		return Math.hypot(this.real, this.imaginary);
	}

/*	public ComplexNumber normalize()
	{
		double abs = Math.sqrt(this.re_value*this.re_value + this.im_value*this.im_value);
		return new ComplexNumber(this.re_value / abs, this.im_value / abs);
	}*/
}
