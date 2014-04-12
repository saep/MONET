package com.github.monet.common;

/**
 * Interface specifying methods that are needed when calculating the S-Metric.
 */
public interface ObjectivePoint {

	public enum ActiveObjectives {STANDARD, NORMALIZED, INVERTED};

	public boolean isMinimization();

	public double[] getObjectiveValues();
	public void setObjectiveValues(double[] objectiveValues);

	public double[] getNormalizedValues();
	public void setNormalizedValues(double[] normalizedValues);

	public double[] getInvertedValues();
	public void setInvertedValues(double[] invertedValues);

	public void setResult(double value);
	public double getResult();

	public void setActiveObjectives(ActiveObjectives activeObjectives);
	public ActiveObjectives getActiveObjectives();
}
