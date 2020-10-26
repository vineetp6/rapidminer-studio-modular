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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.PortPairExtender;


/**
 * Passes output <var>i</var> to input <var>i</var>. generated by a {@link PortPairExtender}.
 * 
 * @author Simon Fischer
 */
public class ManyToManyPassThroughRule implements MDTransformationRule {

	private InputPorts inputPorts;
	private OutputPorts outputPorts;

	public ManyToManyPassThroughRule(InputPorts inputPorts, OutputPorts outputPorts) {
		this.inputPorts = inputPorts;
		this.outputPorts = outputPorts;
	}

	@Override
	public void transformMD() {
		assert (inputPorts.getNumberOfPorts() == outputPorts.getNumberOfPorts());
		int numIn = inputPorts.getNumberOfPorts();
		int numOut = outputPorts.getNumberOfPorts();
		int num = numIn < numOut ? numIn : numOut;
		for (int i = 0; i < num; i++) {
			MetaData metaData = inputPorts.getPortByIndex(i).getMetaData();
			if (metaData != null) {
				metaData = metaData.clone();
				metaData.addToHistory(outputPorts.getPortByIndex(i));
				outputPorts.getPortByIndex(i).deliverMD(modifyMetaData(metaData));
			} else {
				outputPorts.getPortByIndex(i).deliverMD(null);
			}
		}
	}

	/**
	 * Modifies the received meta data before it is passed to the output. Can be used if the
	 * transformation depends on parameters etc. The default implementation just returns the
	 * original. Subclasses may safely modify the meta data, since a copy is used for this method.
	 */
	public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
		return unmodifiedMetaData;
	}
}
