package rs.sandbox.algebra;

import java.util.Random;
import org.apache.commons.math.stat.regression.*;

public class TestMatrixAlgebra {
	
	public static void testOLSMLR() {
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		double[] y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0};
		double[][] x = new double[6][];
		x[0] = new double[]{0,0,0,0};
		x[1] = new double[]{2.0, 0, 0, 0};
		x[2] = new double[]{0, 3.0, 0, 0};
		x[3] = new double[]{0, 0, 4.0, 0};
		x[4] = new double[]{0, 0, 5.0, 0};
		x[5] = new double[]{0, 0, 0, 0};
		regression.newSampleData(y, x);
		
		double[] beta = regression.estimateRegressionParameters();
		double[] residuals = regression.estimateResiduals();
		double regressandVariance = regression.estimateRegressandVariance();
		for(int i=0; i<beta.length; i++) {
			System.out.println(i + ": " + beta[i] + "," + residuals[i]);
		}
		
		System.out.println("Variance:" + regressandVariance);
	}
	
	public static void testMLSMLR() {
		GLSMultipleLinearRegression regression = new GLSMultipleLinearRegression();
		double[] y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0};
		double[][] x = new double[6][];
		x[0] = new double[]{0, 0, 0, 0, 0};
		x[1] = new double[]{2.0, 0, 0, 0, 0};
		x[2] = new double[]{0, 3.0, 0, 0, 0};
		x[3] = new double[]{0, 0, 4.0, 0, 0};
		x[4] = new double[]{0, 0, 0, 5.0, 0};
		x[5] = new double[]{0, 0, 0, 0, 6.0};          
		double[][] omega = new double[6][];
		omega[0] = new double[]{1.1, 0, 0, 0, 0, 0};
		omega[1] = new double[]{0, 2.2, 0, 0, 0, 0};
		omega[2] = new double[]{0, 0, 3.3, 0, 0, 0};
		omega[3] = new double[]{0, 0, 0, 4.4, 0, 0};
		omega[4] = new double[]{0, 0, 0, 0, 5.5, 0};
		omega[5] = new double[]{0, 0, 0, 0, 0, 6.6};
		regression.newSampleData(y, x, omega);
		
		double v = regression.estimateRegressandVariance();
		System.out.println("Variance: " + v);
		double[] residuals = regression.estimateResiduals();
		
		double[] beta = regression.estimateRegressionParameters();
		for(int i=0; i<beta.length; i++) {
			System.out.println(i+":"+beta[i] + "," + residuals[i]);
		}
	}
	
	public static void testLargeScaleMLR() {
		Random r = new Random();
		int SIZE = 60000;
		int TSIZE = 100;
		double[] y = new double[SIZE];
		double[][] x = new double[SIZE][TSIZE];
		
		for(int i=0; i<SIZE; i++) 
			for (int j=0; j<TSIZE; j++) {
				x[i][j] = r.nextDouble();
		}
		
		for (int i=0; i<SIZE; i++) 
			y[i] = r.nextDouble();
		
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(y, x);
		long start = System.currentTimeMillis();
		double[] beta = regression.estimateRegressionParameters();

		for(int i=0; i<beta.length; i++) {
			System.out.println(beta[i]);
		}
		long end = System.currentTimeMillis();
		long seconds = Math.round((end - start)/1000.0);
		System.out.println("Regression took " + seconds + "seconds.");
	}
	
	public static void main(String[] args) {		
//		TestMatrixAlgebra.testMLSMLR();
		TestMatrixAlgebra.testOLSMLR();
		
//		testLargeScaleMLR();
	}
}
