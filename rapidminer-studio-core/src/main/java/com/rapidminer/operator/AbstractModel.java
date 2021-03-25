/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
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
package com.rapidminer.operator;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.HeaderExampleSet;


/**
 * Abstract model is the superclass for all objects which change a data set. For example, a model
 * generated by a learner might add a predicted attribute. Other models can be created during
 * preprocessing, e.g. a transformation model containing the parameters for a z-transformation.
 * Models can be combined by using a CombinedModel. All models can be applied with a ModelApplier
 * operator.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractModel extends ResultObjectAdapter implements Model {

	private static final long serialVersionUID = 1640348739650532634L;

	/**
	 * This header example set contains all important nominal mappings of all training attributes.
	 * These mappings are necessary in order to ensure that the internally used double values
	 * encoding nominal values are equal for the training and application data sets.
	 */
	private HeaderExampleSet headerExampleSet;

	/**
	 * This Operator will be used to check whether the currently running Process was stopped. If it
	 * is <code>null</code> nothing will happen else checkForStop will be called.
	 */
	private Operator operator = null;
	
	/**
	 * This flag signalizes the apply method if progress in the {@link OperatorProgress} from {@link #getOperator()} should be shown.
	 */
	private boolean showProgress = false;

	/**
	 * Created a new model which was built on the given example set. Please note that the given
	 * example set is automatically transformed into a {@link HeaderExampleSet} which means that no
	 * reference to the data itself is kept but only to the header, i.e. to the attribute meta
	 * descriptions.
	 */
	protected AbstractModel(ExampleSet exampleSet) {
		if (exampleSet != null) {
			this.headerExampleSet = new HeaderExampleSet(exampleSet);
		}
	}

	/**
	 * Delivers the training header example set, i.e. the header of the example set (without data
	 * reference) which was used for creating this model. Might return null.
	 */
	@Override
	public HeaderExampleSet getTrainingHeader() {
		return this.headerExampleSet;
	}

	/**
	 * This default implementation returns false. Note that subclasses overriding this method should
	 * also override the method {@link #updateModel(ExampleSet)}.
	 */
	@Override
	public boolean isUpdatable() {
		return false;
	}

	/**
	 * This default implementation throws an {@link UserError}. Subclasses overriding this method to
	 * update the model according to the given example set should also override the method
	 * {@link #isUpdatable()} by delivering true.
	 */
	@Override
	public void updateModel(ExampleSet updateExampleSet) throws OperatorException {
		throw new UserError(null, 135, getClass().getName());
	}

	/**
	 * Throws a UserError since most models should not allow additional parameters during
	 * application. However, subclasses may overwrite this method.
	 */
	@Override
	public void setParameter(String key, Object value) throws OperatorException {
		throw new UnsupportedApplicationParameterError(null, getName(), key);
	}

	/**
	 * The default implementation returns the result of the super class. If the string ends with
	 * model, the substring &quot;model&quot; is removed.
	 */
	@Override
	public String getName() {
		String result = super.getName();
		if (result.toLowerCase().endsWith("model")) {
			result = result.substring(0, result.length() - "model".length());
		}
		return result;
	}

	public String getExtension() {
		return "mod";
	}

	public String getFileDescription() {
		return "model file";
	}

	@Override
	public boolean isInTargetEncoding() {
		return false;
	}

	/**
	 * If the Operator was set the Model will check for stop by calling checkForStop() at the
	 * Operator. If the Operator is set to null nothing will happen and the Model will no longer
	 * checkForStop.
	 * 
	 * @param operator If {@code true} this operator will check for stop by calling checkForStop()
	 */
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	
	/**
	 * If this flag is set to {@code true}, the apply method can show the progress in the {@link OperatorProgress} from {@link #getOperator()}
	 * 
	 * @param boolean When {@code true} progress will be shown when applying.
	 */
	public void setShowProgress(boolean showProgress) {
		this.showProgress = showProgress;
	}

	/**
	 * delivers the set Operator or null if no Operator was set.
	 */
	public Operator getOperator() {
		return operator;
	}
	
	/**
	 * {@code true} if progress should be shown while applying this model.
	 */
	public boolean getShowProgress() {
		return showProgress;
	}

	/**
	 * Checks whether the running Process of the given Operator was stopped.If no Operator is set,
	 * nothing will happen.
	 * 
	 * @throws ProcessStoppedException
	 *             if the running Process was stopped
	 */
	protected void checkForStop() throws ProcessStoppedException {
		Operator op = getOperator();
		if (op != null) {
			op.checkForStop();
		}
	}

}
