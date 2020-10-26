/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.clustering.clusterer;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * This operator represents an implementation of k-medoids. This operator will create a cluster
 * attribute if not present yet.
 *
 * @author Sebastian Land
 */
public class KMedoids extends RMAbstractClusterer {

	/** The parameter name for &quot;the maximal number of clusters&quot; */
	public static final String PARAMETER_K = "k";

	/**
	 * The parameter name for &quot;the maximal number of runs of the k method with random
	 * initialization that are performed&quot;
	 */
	public static final String PARAMETER_MAX_RUNS = "max_runs";

	/**
	 * The parameter name for &quot;the maximal number of iterations performed for one run of the k
	 * method&quot;
	 */
	public static final String PARAMETER_MAX_OPTIMIZATION_STEPS = "max_optimization_steps";

	public KMedoids(OperatorDescription description) {
		super(description);
	}

	@Override
	protected boolean checksForRegularAttributes() {
		return getCompatibilityLevel().isAbove(BEFORE_EMPTY_CHECKS);
	}

	@Override
	protected boolean affectedByEmptyCheck() {
		return true;
	}

	@Override
	protected ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException {
		int k = getParameterAsInt(PARAMETER_K);
		int maxOptimizationSteps = getParameterAsInt(PARAMETER_MAX_OPTIMIZATION_STEPS);
		int maxRuns = getParameterAsInt(PARAMETER_MAX_RUNS);
		boolean addAsLabel = addsLabelAttribute();
		boolean removeUnlabeled = getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED);
		DistanceMeasure measure = getInitializedMeasure(exampleSet);

		// init operator progress
		getProgress().setTotal(100);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// additional checks
		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);
		if (exampleSet.size() < k) {
			throw new UserError(this, 142, k);
		}

		// extracting attribute names
		Attributes attributes = exampleSet.getAttributes();
		ArrayList<String> attributeNames = new ArrayList<String>(attributes.size());
		for (Attribute attribute : attributes) {
			attributeNames.add(attribute.getName());
		}

		RandomGenerator generator = RandomGenerator.getRandomGenerator(this);
		double minimalIntraClusterDistance = Double.POSITIVE_INFINITY;
		CentroidClusterModel bestModel = null;
		int[] bestAssignments = null;
		double[] values = new double[attributes.size()];
		for (int iter = 0; iter < maxRuns; iter++) {
			CentroidClusterModel model = new CentroidClusterModel(exampleSet, k, attributeNames, measure, addAsLabel,
					removeUnlabeled);
			// init centroids
			int i = 0;
			for (Integer index : generator.nextIntSetWithRange(0, exampleSet.size(), k)) {
				double[] asDoubleArray = getAsDoubleArray(exampleSet.getExample(index), attributes, values);
				model.assignExample(i, asDoubleArray);
				i++;
			}
			model.finishAssign();

			// run optimization steps
			int[] centroidAssignments = new int[exampleSet.size()];
			boolean stable = false;
			for (int step = 0; step < maxOptimizationSteps && !stable; step++) {
				// assign examples to new centroids
				i = 0;
				for (Example example : exampleSet) {
					double[] exampleValues = getAsDoubleArray(example, attributes, values);
					double nearestDistance = measure.calculateDistance(model.getCentroidCoordinates(0), exampleValues);
					int nearestIndex = 0;
					for (int centroidIndex = 1; centroidIndex < k; centroidIndex++) {
						double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidIndex),
								exampleValues);
						if (distance < nearestDistance) {
							nearestDistance = distance;
							nearestIndex = centroidIndex;
						}
					}
					centroidAssignments[i] = nearestIndex;
					i++;
				}

				for (int clusterIndex = 0; clusterIndex < k; clusterIndex++) {
					double[] bestMedoidValues = new double[attributes.size()];
					double bestDistanceSum = Double.POSITIVE_INFINITY;
					for (Example medoid : exampleSet) {
						// calculate intra cluster distance if this example is used as medoid
						double distanceSum = 0;
						double[] medoidValues = getAsDoubleArray(medoid, attributes, values);
						int j = 0;
						for (Example example : exampleSet) {
							// add only if in current cluster
							if (centroidAssignments[j] == clusterIndex) {
								distanceSum += measure.calculateDistance(getAsDoubleArray(example, attributes, values),
										medoidValues);
							}
							j++;
						}
						if (distanceSum < bestDistanceSum) {
							bestDistanceSum = distanceSum;
							bestMedoidValues = medoidValues;
						}
					}
					// assigning into model as best point using average of one
					model.getCentroid(clusterIndex).assignExample(bestMedoidValues);
				}
				stable = model.finishAssign();
				getProgress()
						.setCompleted((int) (100.0 * iter / maxRuns + 100.0 / maxRuns * (step + 1) / maxOptimizationSteps));
			}

			// assessing quality of this model
			double distanceSum = 0;
			i = 0;
			for (Example example : exampleSet) {
				double distance = measure.calculateDistance(model.getCentroidCoordinates(centroidAssignments[i]),
						getAsDoubleArray(example, attributes, values));
				distanceSum += distance * distance;
				i++;
			}
			if (distanceSum < minimalIntraClusterDistance || Double.isInfinite(minimalIntraClusterDistance)) {
				bestModel = model;
				minimalIntraClusterDistance = distanceSum;
				bestAssignments = centroidAssignments;
			}
			getProgress().setCompleted((int) (100.0 * (iter + 1) / maxRuns));
		}
		bestModel.setClusterAssignments(bestAssignments, exampleSet);

		if (addsClusterAttribute()) {
			addClusterAssignments(exampleSet, bestAssignments);
		}

		getProgress().complete();

		return bestModel;
	}

	private double[] getAsDoubleArray(Example example, Attributes attributes, double[] values) {
		int i = 0;
		for (Attribute attribute : attributes) {
			values[i] = example.getValue(attribute);
			i++;
		}
		return values;
	}

	@Override
	public Class<? extends ClusterModel> getClusterModelClass() {
		return CentroidClusterModel.class;
	}

	@Override
	protected boolean usesDistanceMeasures() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeInt(PARAMETER_K, "The number of clusters which should be detected.", 2, Integer.MAX_VALUE,
				2, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_RUNS,
				"The maximal number of runs of k-Means with random initialization that are performed.", 1, Integer.MAX_VALUE,
				10, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_OPTIMIZATION_STEPS,
				"The maximal number of iterations performed for one run of k-Means.", 1, Integer.MAX_VALUE, 100, false));
		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		types.addAll(getMeasureParameterTypes());
		return types;
	}
}
