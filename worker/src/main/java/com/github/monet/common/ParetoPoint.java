package com.github.monet.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a point that can be used for calculating the S-Metric. 
 */
public class ParetoPoint implements ObjectivePoint {

	private double[] objectiveValues;
	private boolean minimize;
	private double[] normalizedValues;
	private double[] invertedValues;
	private double result;
	private ActiveObjectives activeObjectives;

	/**
	 * Constructor for a ParetoPoint. Minimization should be set to true for all
	 * MONET problems.
	 */
	public ParetoPoint(double[] objectiveValues, boolean minimize) {
		this.objectiveValues = objectiveValues;
		this.minimize = minimize;
		this.normalizedValues = null;
		this.invertedValues = null;
		this.activeObjectives = ObjectivePoint.ActiveObjectives.STANDARD;
	}
	
	/**
	 * Depending on the active objective values to use, the corresponding array
	 * is returned.
	 */
	@Override
	public double[] getObjectiveValues() {
		if (this.activeObjectives.equals(ObjectivePoint.ActiveObjectives.STANDARD))
			return this.objectiveValues;
		else if (this.activeObjectives.equals(ObjectivePoint.ActiveObjectives.NORMALIZED))
			return this.getNormalizedValues();
		else if (this.activeObjectives.equals(ObjectivePoint.ActiveObjectives.INVERTED))
			return this.getInvertedValues();
		return null;
	}
	@Override
	public ActiveObjectives getActiveObjectives() {
		return activeObjectives;
	}

	@Override
	public void setActiveObjectives(ActiveObjectives activeObjectives) {
		this.activeObjectives = activeObjectives;
	}
	@Override
	public void setObjectiveValues(double[] objectiveValues) {
		this.objectiveValues = objectiveValues;
	}

	@Override
	public double[] getNormalizedValues() {
		return this.normalizedValues;
	}
	@Override
	public void setNormalizedValues(double[] normalizedValues) {
		this.normalizedValues = normalizedValues;

	}

	@Override
	public double[] getInvertedValues() {
		return this.invertedValues;
	}
	@Override
	public void setInvertedValues(double[] invertedValues) {
		this.invertedValues = invertedValues;

	}

	@Override
	public void setResult(double value) {
		this.result = value;
	}
	@Override
	public double getResult() {
		return this.result;
	}
	
	@Override
	public boolean isMinimization() {
		return this.minimize;
	}

	/**
	 * Calculate the S-Metric for the given list of points.
	 * If minimization is true, the points will be inverted (maxValue[i] - pointValue[i])
	 * 
	 * @param points
	 * 		Points to calculate the S-Metric for. THE POINTS HAVE TO BE POSITIVE!
	 * @param minimization
	 * 		True for minimization problems (which is the case for all MONET-Problems)
	 * @return
	 * 		S-Metric value
	 */
	public static double calculateSMetric(List<double[]> points, double[] ref, boolean minimization) {
		
		// Get Dimension
		if (points.size() == 0) return 0;
		int k = points.get(0).length;
		
		// Create reference point if none is given
		if (ref == null) {
			ref = new double[k];
		}
		
		// Create ParetoPoint objects so we can work on the points
		List<ParetoPoint> resPoints = new ArrayList<ParetoPoint>(points.size());
		for (double[] point : points) {
			resPoints.add(new ParetoPoint(point, minimization));
		}
		
		// If we have a minimization problem we have to use inverted values
		// because getMaximizationSMetricByHSO expects a maximization problem!
		// NOTE: WE ALSO ADD 1 TO EACH INVERTED VALUE SO WE DON'T HAVE ANY POINTS THAT LIE ON ONE OF THE AXIS!
		if (minimization) {
			ParetoPoint.setNormalizedAndInvertedValues(resPoints, ParetoPoint.getMin(resPoints), ParetoPoint.getMax(resPoints), false, 0, 1);
			for (ParetoPoint p : resPoints)
				p.setActiveObjectives(ObjectivePoint.ActiveObjectives.INVERTED);
		}
		
		// Calculate the S-Metric value
		double sMetricValue = ParetoPoint.getMaximizationSMetricByHSO(resPoints, ref, k);
		return sMetricValue;
	}
	public static double calculateSMetric(List<double[]> points, boolean minimization) {
		return ParetoPoint.calculateSMetric(points, null, minimization);
	}
	
	
	/**
	 * Create a list of ParetoPoint objects from given double arrays
	 */
	public static List<ParetoPoint> createPoints(List<double[]> points, boolean minimization) {
		List<ParetoPoint> result = new ArrayList<ParetoPoint>(points.size());
		for (double[] point : points) {
			result.add(new ParetoPoint(point, minimization));
		}
		return result;
	}

	
	
	//##########################################################################
	// STATIC METHODS TAKEN FROM THE INDIVIDUAL CLASS
	//##########################################################################
	
	/**
	 * Checks if first array dominates the second array. Only considers the
	 * first k objectives / values. Both arrays must be of the same size.
	 * 
	 * @param first
	 *            first array
	 * @param second
	 *            second array (same size as first!)
	 * @param k
	 *            objectives to compare
	 * 
	 * @return true if first array dominates second array.
	 */
	public static boolean dominates(double[] first, double[] second, int k, boolean minimization) {
		assert ((first.length == second.length) && (first.length >= k) && (second.length >= k));
		
		boolean oneBetter = false;
		// Check domination
		// competitor has a better value at any pos => cannot be dominated
		// the individual has to be better(!) in one objective
		if (minimization) {
			for (int i = 0; i < k; i++) {
				if (first[i] > second[i]) return false;
				if (first[i] < second[i]) oneBetter = true;
			}
		} else {
			for (int i = 0; i < k; i++) {
				if (first[i] < second[i]) return false;
				if (first[i] > second[i]) oneBetter = true;
			}
		}
		return oneBetter;
	}
	
	
	/**
	 * Get non-dominated solutions (fast book-keeping strategy, see NSGA2 Paper)
	 * Note: No "Individual competitor : nonDominated" loop because the lists
	 * change!
	 * 
	 * Modification for k-non-domination: only consider first k objective
	 * values.
	 * 
	 * The given population list will not be modified.
	 * 
	 * @param nonDominated
	 * 			  initial list of nonDominated solutions.
	 * 			  IF THIS LIST IS GIVEN, IT WILL BE MODIFIED! THE MODIFIED LIST IS THEN RETURNED.
	 * 			  If the parameter is null, a new list is created and returned
	 * @param population
	 *            list of individuals which is checked for non-dominated
	 *            solutions. The list will not be modified by the algorithm.
	 * 
	 * @return a new list containing non-dominated individuals found in the
	 *          given population
	 */
	public static <P extends ObjectivePoint> List<P> getNondominatedSolutions(List<P> nonDominated, List<P> population, int k, boolean minimization) {
		assert(population != null && population.size() != 0);
		
		// Init nondominated set
		if (nonDominated == null) nonDominated = new ArrayList<P>();
		if (population.size() == 0) return nonDominated;
		if (nonDominated.size() == 0) nonDominated.add(population.get(0));
		
		// Fill nondominated set
		for (P individual : population) {
			if (nonDominated.contains(individual)) continue;
			nonDominated.add(individual);
			// For current individual selected from population: 
			// check if any individual in the nonDominated set is now dominated
			for (int j = 0; j < nonDominated.size(); j++) {
				P competitor = nonDominated.get(j);
				if (competitor.equals(individual)) {
					continue;
				} else if (ParetoPoint.dominates(individual.getObjectiveValues(), competitor.getObjectiveValues(), k, minimization)) {
					nonDominated.remove(competitor);
					j--;
				} else if (ParetoPoint.dominates(competitor.getObjectiveValues(), individual.getObjectiveValues(), k, minimization)) {
					nonDominated.remove(individual);
					// j--; Individual is always added at the end => no j--!
				}
			}
		}
		
		return nonDominated;
	}
	
	
	/**
	 * Checks if the given set contains a dominated solution
	 */
	public static <P extends ObjectivePoint> boolean containsDominatedSolution(List<P> population, int k, boolean minimization) {
		for (ObjectivePoint p1 : population)
			for (ObjectivePoint p2 : population)
				if (       ParetoPoint.dominates(p1.getObjectiveValues(), p2.getObjectiveValues(), k, minimization) 
						|| ParetoPoint.dominates(p2.getObjectiveValues(), p1.getObjectiveValues(), k, minimization)
					)
					return true;
		return false;
	}
	
	
	/**
	 * Returns a new list of dominated individuals
	 * 
	 * @param population
	 *            list of individuals which is checked for being dominated
	 *            solutions. The list will not be modified by the algorithm.
	 * 
	 * @return a new list containing dominated individuals found in the given
	 *          population
	 */
	public static <P extends ObjectivePoint> List<P> getDominatedSolutions(List<P> population, int k, boolean minimization) {
		List<P> result = new ArrayList<P>();
		for (P ind : population) {
			// Check if ind is dominated by any individual and add it if this is the case
			for (P comp : population) {
				if (ParetoPoint.dominates(comp.getObjectiveValues(), ind.getObjectiveValues(), k, minimization)) {
					result.add(ind);
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Gets worst individual according to the k-th objective value.
	 * 
	 * @param inds
	 *            list of individuals
	 * @param k
	 *            objective value to use for comparison
	 * 
	 * @return worst individual according to k-th objective value
	 */
	public static <P extends ObjectivePoint> P getWorst(List<P> inds, int k, boolean minimization) {
		if (inds.size() == 0) return null;
		P worst = inds.get(0);
		
		for (P ind : inds)
			if (minimization) {
				if (ind.getObjectiveValues()[k] > worst.getObjectiveValues()[k])
					worst = ind;
			} else {
				if (ind.getObjectiveValues()[k] < worst.getObjectiveValues()[k])
					worst = ind;
			}
					
		return worst;
	}
	
	
	/**
	 * Gets the minimum value of every dimension
	 * 
	 * @param inds
	 *            individuals to process
	 * @return array containing the minimum values of each dimension
	 */
	public static <P extends ObjectivePoint> double[] getMin(List<P> inds) {
		if (inds.size() == 0) return null;
		int numValues = inds.get(0).getObjectiveValues().length;
		double[] min = new double[numValues];
		
		for (int i = 0; i < inds.size(); i++) {
			double[] values = inds.get(i).getObjectiveValues();
			for (int j = 0; j < values.length; j++) {
				if (min[j] > values[j] || i == 0) {
					min[j] = values[j];
				}
			}
		}
		
		return min;
	}
	
	
	/**
	 * Gets the maximum value of every dimension
	 * 
	 * @param inds
	 *            individuals to process
	 * @return array containing the maximum values of each dimension
	 */
	public static <P extends ObjectivePoint> double[] getMax(List<P> inds) {
		if (inds.size() == 0) return null;
		int numValues = inds.get(0).getObjectiveValues().length;
		double[] max = new double[numValues];
		
		for (int i = 0; i < inds.size(); i++) {
			double[] values = inds.get(i).getObjectiveValues();
			for (int j = 0; j < values.length; j++) {
				if (max[j] < values[j] || i == 0) {
					max[j] = values[j];
				}
			}
		}
		
		return max;
	}
	
	
	/**
	 * Get minimum value of given dimension found in objective values of
	 * individuals
	 * 
	 * @param inds
	 *            individuals to process
	 * @param k
	 *            dimension to process
	 * @return minimum value of k-th dimension
	 */
	public static <P extends ObjectivePoint> double getMinValue(List<P> inds, int k) {
		double min = -1;
		for (int i = 0; i < inds.size(); i++) {
			double[] values = inds.get(i).getObjectiveValues();
			if (values[k] < min || i == 0) {
				min = values[k];
			}
		}
		return min;
	}


	/**
	 * Get maximum value of given dimension found in objective values of
	 * individuals
	 * 
	 * @param inds
	 *            individuals to process
	 * @param k
	 *            dimension to process
	 * @return minimum value of k-th dimension
	 */
	public static <P extends ObjectivePoint> double getMaxValue(List<P> inds, int k) {
		double max = -1;
		for (int i = 0; i < inds.size(); i++) {
			double[] values = inds.get(i).getObjectiveValues();
			if (values[k] > max || i == 0) {
				max = values[k];
			}
		}
		return max;
	}
	
	
	/**
	 * Set normalized and inverted values of given Points.
	 * 
	 * @param inds
	 *            list of individuals to process
	 * @param min
	 *            minimum values in each dimension
	 * @param max
	 *            maximum values in each dimension
	 * @param addToNormalized
	 *            constant value added to all normalized values
	 * @param addToInverted
	 *            constant value added to all inverted values
	 * @param normalizeInverted
	 *            normalize inverted values?
	 */
	public static <P extends ObjectivePoint> void setNormalizedAndInvertedValues(List<P> inds, double[] min, double[] max, boolean normalizeInverted, double addToNormalized, double addToInverted) {
		//System.out.println(Arrays.toString(max));
		//System.out.println(Arrays.toString(min));
		
		int numValues = min.length;
		for (P ind : inds) {
			double[] normalized = new double[numValues];
			double[] inverted   = new double[numValues];
			double[] values     = ind.getObjectiveValues();
			for (int i = 0; i < numValues; i++) {
				normalized[i] = ((values[i] - min[i]) / (max[i] - min[i])) + addToNormalized;
				inverted[i]   = (normalizeInverted) ? (1 - normalized[i]) + addToInverted : (max[i] - values[i]) + addToInverted;
			}
			ind.setNormalizedValues(normalized);
			ind.setInvertedValues(inverted);
		}
	}
	public static <P extends ObjectivePoint> void setNormalizedAndInvertedValues(List<P> inds) {
		ParetoPoint.setNormalizedAndInvertedValues(inds, ParetoPoint.getMin(inds), ParetoPoint.getMax(inds), false, 0, 0);
	}
	public static <P extends ObjectivePoint> void setNormalizedAndNormalizedInvertedValues(List<P> inds) {
		ParetoPoint.setNormalizedAndInvertedValues(inds, ParetoPoint.getMin(inds), ParetoPoint.getMax(inds), true, 0, 0);
	}
	
	
	/**
	 * Get a new list of individuals that have a larger value than the given
	 * threshold in the k-th dimension
	 * 
	 * @param inds
	 *            Individuals to process
	 * @param dim
	 *            dimension to process
	 * @param threshold
	 *            only higher values will be selected
	 * @return new list of selected individuals
	 */
	public static <P extends ObjectivePoint> List<P> filterByMaxObjectiveThreshold(List<P> inds, int dim, double threshold) {
		List<P> filtered = new ArrayList<P>();
		for (int i = 0; i < inds.size(); i++) {
			P ind = inds.get(i);
			if (ind.getObjectiveValues()[dim] > threshold) {
				filtered.add(ind);
			}
		}
		return filtered;
	}
	
	
	
	//##########################################################################
	// LOGGING
	//##########################################################################
	
	/**
	 * Export objective values of the individuals to CSV
	 */
	public static <P extends ObjectivePoint> void exportObjectiveValuesToCSV(List<P> individuals, String filePath, boolean append) {
		try {
			FileWriter writer = new FileWriter(filePath, append);
			for (P ind : individuals) {
				double[] objValues = ind.getObjectiveValues();
				if (objValues != null) {
					writer.append( ParetoPoint.join(objValues, ", ") + "\n" );
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static <P extends ObjectivePoint> void exportObjectiveValuesToCSV(P individual, String filePath, boolean append) {
		try {
			FileWriter writer = new FileWriter(filePath, append);
			double[] objValues = individual.getObjectiveValues();
			if (objValues != null) {
				writer.append( ParetoPoint.join(objValues, ", ") + "\n" );
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static double calcSMetricfromCSV(String filepath,
			boolean minimization) {
		BufferedReader br = null;
		List<double[]> front = new ArrayList<>();
		String line = "";
		String cvsSplitBy = ",";
		try {
			br = new BufferedReader(new FileReader(filepath));
			while ((line = br.readLine()) != null) {
				String[] pointStr = line.split(cvsSplitBy);
				double[] point = new double[pointStr.length];
				for (int i = 0; i < pointStr.length; i++) {
					point[i] = Double.parseDouble(pointStr[i].trim());
				}
				front.add(point);
				// ParetoPoint nextPoint = new ParetoPoint(point,minimization);
				// front.add(nextPoint);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return ParetoPoint.calculateSMetric(front, minimization);
	}
	
	
	/**
	 * Simple join function on double arrays
	 */
	public static String join(double[] arr, String sep) {
		String result = "";
		for (int i = 0; i < arr.length; i++)
			result += (i < arr.length-1) ? (arr[i]+sep) : (arr[i]);
		return result;
	}
	
	
	
	// ########################################################################
	// STATIC S-METRIC METHODS
	// ########################################################################
	
	
	/**
	 * Calculates the dominated hypervolume of the given List of Individuals.
	 * !!! OBJECTIVE VALUES HAVE TO BE POSITIVE !!! 
	 * !!! A _MAXIMIZATION_ PROBLEM IS ASSERTED !!!
	 * 
	 * @param inds
	 *            List of individuals with set objective values
	 * @param ref
	 * 			  THIS PARAMETER IS IGNORED IN THE CURRENT IMPLEMENTATION.
	 *            Reference point (e.g. the nadir-point) (this is currently not
	 *            needed because inverted normalized values are used so that
	 *            0,...,0 can be used as reference point.
	 * 
	 * @return s-metric value
	 */
	public static <P extends ObjectivePoint> double getMaximizationSMetricByHSO(List<P> inds, double[] ref, int k) {
		/*
		 * Note: The example given in the comments refers to example in the
		 * paper "A Faster Algorithm for Calculating Hypervolume", 2006, by
		 * Lyndon While, Phil Hingston, Luigi Barone and Simon Huband (Figure
		 * 5). This version of the algorithm follows E. Zitzler and L. Thiele
		 * "Multiobjective Evolutionary Algorithms: A Comparative Case Study and
		 * the Strength Pareto Approach" and a similar implementation found in
		 * the jMetal Framework for Evolutionary Algorithms.
		 * Another useful reference is (know02, pdf page 115).
		 */
		
		double volume = 0; // total hypervolume
		double curDistance = ref[k-1]; // Current distance of the slice from 0 in Dimension k
		
		/*
		 * Each iteration represents a single slice in the given dimension k
		 */
		while (inds.size() > 0) {
			
			/*
			 * Calculate hypervolume for k-1 objective non-dominated Individuals (lastVol). Dominated individuals do not add anything to the hypervolume!
			 * If k==2 the recursive function calculates the hypervolume for k==1, which is just the maximum of the nondominated values. Thus the recursion ends.
			 * Example (k=3, calculate hypervolume lastVol for k-1=2):
			 * 		Iteration 1: Point c,d not dominated w.r.t. y/z
			 * 		Iteration 2: new non-dominated points (d removed, c now non-dominated)
			 * 		Iteration 3: new non-dominated points (c/d removed, a/b now non-dominated - note that they were dominated before!)
			 */
			List<P> nonDominated = ParetoPoint.getNondominatedSolutions(null, inds, k-1, false);
			double lastVol = 0;
			if (k <= 2) {
				// Max value in k-1=1 Dimension (Index 0). assert(lastVol > 0);
				lastVol = getMaxValue(inds, 0) - ref[0];
			} else {
				lastVol = getMaximizationSMetricByHSO(nonDominated, ref, k-1);
			}
			
			/*
			 * Calculate the thickness of the current slice in the k-th dimension  and multiply it with the k-1 hypervolume we just calculated.
			 * the "thickness" of the slice ranges from the end of the last slice to the minimum value of a point.
			 * Note that the newDistance is always larger than curDistance, since only small-value-points are deleted.
			 * Example:
			 * 		Iteration 1: Minimum value in Dimension x (corresponds to coordinate of d) [Slice 4]
			 * 		Iteration 2: Coordinate of c (d has already been removed!) [Slice 3]
			 * 		Iteration 3: Coordinate of b (c/d have already been removed!) [Slice 2]
			 */
			double newDistance = getMinValue(inds, k-1);
			volume += lastVol * Math.abs(newDistance - curDistance);
			curDistance = newDistance;
			
			/*
			 * Remove individuals that have a value <= "distance" in the current dimension k
			 * (k-1 Parameter because we want to filter in k-th dimension which corresponds to index k)
			 * Example:
			 * 		Iteration 1: Remove d
			 * 		Iteration 2: Remove c
			 * 		Iteration 3: Remove b
			 * 
			 * Note: filterByMaxObjectiveThreshold returns a new list. The original list is unchanged 
			 * (and still used by the upper level of recursion!).
			 */
			inds = filterByMaxObjectiveThreshold(inds, k-1, newDistance);
			
			/*
			 * Debugging output
			 */
			/*
			System.out.println(inds.size() + " " + newDistance);
			for (Individual ind : inds) ind.printShort();
			//*/
		}
		
		return volume;
	}
//	public static <P extends ObjectivePoint> double getMaximizationSMetricByHSO(List<P> inds) {
//		if (inds.size() == 0) return 0;
//		return ParetoPoint.getMaximizationSMetricByHSO(inds, null, inds.get(0).getObjectiveValues().length);
//	}
	
	
	/**
	 * Calculates the S-Metric contribution of each individual in the given
	 * list. The results are stored in the individuals "result" attribute.
	 * 
	 * @param inds
	 *            list to calculate the contribution values for
	 * @param ref
	 *            reference point for the HSO alg.
	 * @param k
	 *            objective values to process (first k values are used)
	 * @param returnMaxContributor
	 *            return Individual that contributes the most to the S-Metric
	 *            value of the population. If this is set to false, then the
	 *            minimum contributor is returned instead.
	 */
	public static <P extends ObjectivePoint> P calcSMetricContrib(List<P> inds, double[] ref, int k, boolean returnMaxContibutor) {
		// Nothing to do?
		if (inds.size() == 0) {
			return null;
		}
		// Reference must not be null
		if (ref == null) {
			ref = new double[k];
		}
		// Calculate contribution
		P result = inds.get(0);
		double totalVolume = getMaximizationSMetricByHSO(inds, ref, k);
		for (int i = 0; i < inds.size(); i++) {
			// Always take first element (will select new individual each time)
			P ind = inds.remove(0);
			// Calculate contribution of ind to the totalVolume
			double subsetVolume  = getMaximizationSMetricByHSO(inds, ref, k);
			double contribVolume = totalVolume - subsetVolume;
			ind.setResult(contribVolume);
			// Add Individual back (append!)
			inds.add(ind);
			// Check what to return later
			if ( (result.getResult() < ind.getResult() && returnMaxContibutor) || (result.getResult() > ind.getResult() && !returnMaxContibutor) ) {
				result = ind;
			}
		}
		return result;
	}

}
