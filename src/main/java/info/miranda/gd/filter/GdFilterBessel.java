package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.*;
import static java.lang.Math.PI;

public class GdFilterBessel implements GdFilterInterface {

	private static double KernelBessel_J1(final double x) {
		double p, q;

		final double
				Pone[] =
				{
						0.581199354001606143928050809e+21,
						-0.6672106568924916298020941484e+20,
						0.2316433580634002297931815435e+19,
						-0.3588817569910106050743641413e+17,
						0.2908795263834775409737601689e+15,
						-0.1322983480332126453125473247e+13,
						0.3413234182301700539091292655e+10,
						-0.4695753530642995859767162166e+7,
						0.270112271089232341485679099e+4
				},
				Qone[] =
						{
								0.11623987080032122878585294e+22,
								0.1185770712190320999837113348e+20,
								0.6092061398917521746105196863e+17,
								0.2081661221307607351240184229e+15,
								0.5243710262167649715406728642e+12,
								0.1013863514358673989967045588e+10,
								0.1501793594998585505921097578e+7,
								0.1606931573481487801970916749e+4,
								0.1e+1
						};

		p = Pone[8];
		q = Qone[8];
		for (int i=7; i >= 0; i--)
		{
			p = p*x*x+Pone[i];
			q = q*x*x+Qone[i];
		}
		return (double)(p/q);
	}

	private static double KernelBessel_P1(final double x)
	{
		double p, q;

		final double
				Pone[] =
				{
						0.352246649133679798341724373e+5,
						0.62758845247161281269005675e+5,
						0.313539631109159574238669888e+5,
						0.49854832060594338434500455e+4,
						0.2111529182853962382105718e+3,
						0.12571716929145341558495e+1
				},
				Qone[] =
						{
								0.352246649133679798068390431e+5,
								0.626943469593560511888833731e+5,
								0.312404063819041039923015703e+5,
								0.4930396490181088979386097e+4,
								0.2030775189134759322293574e+3,
								0.1e+1
						};

		p = Pone[5];
		q = Qone[5];
		for (int i=4; i >= 0; i--)
		{
			p = p*(8.0/x)*(8.0/x)+Pone[i];
			q = q*(8.0/x)*(8.0/x)+Qone[i];
		}
		return (double)(p/q);
	}

	private static double KernelBessel_Q1(final double x)
	{
		double p, q;

		final double
				Pone[] =
				{
						0.3511751914303552822533318e+3,
						0.7210391804904475039280863e+3,
						0.4259873011654442389886993e+3,
						0.831898957673850827325226e+2,
						0.45681716295512267064405e+1,
						0.3532840052740123642735e-1
				},
				Qone[] =
						{
								0.74917374171809127714519505e+4,
								0.154141773392650970499848051e+5,
								0.91522317015169922705904727e+4,
								0.18111867005523513506724158e+4,
								0.1038187585462133728776636e+3,
								0.1e+1
						};

		p = Pone[5];
		q = Qone[5];
		for (int i=4; i >= 0; i--)
		{
			p = p*(8.0/x)*(8.0/x)+Pone[i];
			q = q*(8.0/x)*(8.0/x)+Qone[i];
		}
		return (double)(p/q);
	}

	private static double KernelBessel_Order1(double x)
	{
		double p, q;

		if (x == 0.0)
			return (0.0f);
		p = x;
		if (x < 0.0)
			x=(-x);
		if (x < 8.0)
			return (p*KernelBessel_J1(x));
		q = (double)sqrt(2.0f/(PI*x))*(double)(KernelBessel_P1(x)*(1.0f/sqrt(2.0f)*(sin(x)-cos(x)))-8.0f/x*KernelBessel_Q1(x)*
				(-1.0f/sqrt(2.0f)*(sin(x)+cos(x))));
		if (p < 0.0f)
			q = (-q);
		return (q);
	}

	public double filter(final double x) {
		if (x == 0.0f)
			return (double)(PI/4.0f);
		return (KernelBessel_Order1((double)PI*x)/(2.0f*x));
	}

}
