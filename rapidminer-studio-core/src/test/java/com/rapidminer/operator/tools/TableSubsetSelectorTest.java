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
package com.rapidminer.operator.tools;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.preprocessing.filter.columns.RegexColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.SingleColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.SubsetColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.ValueTypeColumnFilter;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributeSubset;
import com.rapidminer.test_utils.RapidAssert;


/**
 * Tests for {@link TableSubsetSelector}, in particular for the type pre-filtering.
 *
 * @author Gisa Meier
 * @since 9.9
 */
public class TableSubsetSelectorTest {

	@BeforeClass
	public static void setup() {
		RapidMiner.initAsserters();
	}

	private class DummyOperator extends Operator {
		private final InputPort tableInput = getInputPorts().createPort("example set input");
		private final TableSubsetSelector subsetSelector;

		public DummyOperator(BiFunction<Operator, InputPort, TableSubsetSelector> function) {
			super(new OperatorDescription("test", "test", DummyOperator.class, null, null, null));
			subsetSelector = function.apply(this, tableInput);
		}

		@Override
		public List<ParameterType> getParameterTypes() {
			List<ParameterType> types = super.getParameterTypes();
			types.addAll(subsetSelector.getParameterTypes());
			return types;
		}

	}

	private TableSubsetSelector getTableSubsetSelector(Operator op, InputPort inputPort) {
		return new TableSubsetSelector(op, inputPort);
	}

	@Test
	public void testAll() throws UserError {
		Table table = createTable();
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		final Table subset = dummyOperator.subsetSelector.getSubset(table);

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_REAL,
						ValueTypeColumnFilter.TYPE_INTEGER, ValueTypeColumnFilter.TYPE_BINOMINAL,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL, ValueTypeColumnFilter.TYPE_DATE_TIME,
						ValueTypeColumnFilter.TYPE_TIME));
		final Table subset2 = dummyOperator2.subsetSelector.getSubset(table);

		RapidAssert.assertEquals(new IOTable(subset), new IOTable(subset2));
	}

	@Test
	public void testAllMD() throws UserError {
		TableMetaData tableMD = new TableMetaData(new IOTable(createTable()), true);
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		final TableMetaData subset = dummyOperator.subsetSelector.getMetaDataSubset(tableMD);

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_REAL,
						ValueTypeColumnFilter.TYPE_INTEGER, ValueTypeColumnFilter.TYPE_BINOMINAL,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL, ValueTypeColumnFilter.TYPE_DATE_TIME,
						ValueTypeColumnFilter.TYPE_TIME));
		final TableMetaData subset2 = dummyOperator2.subsetSelector.getMetaDataSubset(tableMD);

		Assert.assertEquals(subset.labels(), subset2.labels());
	}

	@Test
	public void testNumeric() throws UserError {
		Table table = createTable();
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "a subset");
		dummyOperator.setParameter(SubsetColumnFilter.PARAMETER_SELECT_SUBSET, ValueTypeColumnFilter.TYPE_REAL +
				ParameterTypeAttributeSubset.ATTRIBUTE_SEPARATOR_CHARACTER + ValueTypeColumnFilter.TYPE_INTEGER +
				"Missing");
		final Table subset =
				dummyOperator.subsetSelector.getSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_REAL,
						ValueTypeColumnFilter.TYPE_INTEGER,
						ValueTypeColumnFilter.TYPE_REAL + "Missing", ValueTypeColumnFilter.TYPE_INTEGER + "Missing")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_REAL,
						ValueTypeColumnFilter.TYPE_INTEGER));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "a subset");
		dummyOperator2.setParameter(SubsetColumnFilter.PARAMETER_SELECT_SUBSET, ValueTypeColumnFilter.TYPE_REAL +
				ParameterTypeAttributeSubset.ATTRIBUTE_SEPARATOR_CHARACTER + ValueTypeColumnFilter.TYPE_INTEGER +
				"Missing");
		final Table subset2 = dummyOperator2.subsetSelector.getSubset(table);

		RapidAssert.assertEquals(new IOTable(subset), new IOTable(subset2));
	}

	@Test
	public void testNumericMD() throws UserError {
		TableMetaData table = new TableMetaData(new IOTable(createTable()), true);
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "a subset");
		dummyOperator.setParameter(SubsetColumnFilter.PARAMETER_SELECT_SUBSET, ValueTypeColumnFilter.TYPE_REAL +
				ParameterTypeAttributeSubset.ATTRIBUTE_SEPARATOR_CHARACTER + ValueTypeColumnFilter.TYPE_INTEGER +
				"Missing");
		final TableMetaData subset =
				dummyOperator.subsetSelector.getMetaDataSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_REAL,
						ValueTypeColumnFilter.TYPE_INTEGER,
						ValueTypeColumnFilter.TYPE_REAL + "Missing", ValueTypeColumnFilter.TYPE_INTEGER + "Missing")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_REAL,
						ValueTypeColumnFilter.TYPE_INTEGER));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "a subset");
		dummyOperator2.setParameter(SubsetColumnFilter.PARAMETER_SELECT_SUBSET, ValueTypeColumnFilter.TYPE_REAL +
				ParameterTypeAttributeSubset.ATTRIBUTE_SEPARATOR_CHARACTER + ValueTypeColumnFilter.TYPE_INTEGER +
				"Missing");
		final TableMetaData subset2 = dummyOperator2.subsetSelector.getMetaDataSubset(table);

		Assert.assertEquals(subset.labels(), subset2.labels());
	}

	@Test
	public void testNominal() throws UserError {
		Table table = createTable();
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "one attribute");
		dummyOperator.setParameter(SingleColumnFilter.PARAMETER_ATTRIBUTE,
				ValueTypeColumnFilter.TYPE_BINOMINAL + "Missing");
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION, "exclude attributes");
		final Table subset =
				dummyOperator.subsetSelector.getSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_BINOMINAL, ValueTypeColumnFilter.TYPE_NON_BINOMINAL,
						ValueTypeColumnFilter.TYPE_BINOMINAL + "Missing",
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL + "Missing", "Special")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_BINOMINAL,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "one attribute");
		dummyOperator2.setParameter(SingleColumnFilter.PARAMETER_ATTRIBUTE,
				ValueTypeColumnFilter.TYPE_BINOMINAL + "Missing");
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION, "exclude attributes");
		final Table subset2 = dummyOperator2.subsetSelector.getSubset(table);

		RapidAssert.assertEquals(new IOTable(subset), new IOTable(subset2));
	}

	@Test
	public void testNominalMD() throws UserError {
		TableMetaData table = new TableMetaData(new IOTable(createTable()), true);
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "one attribute");
		dummyOperator.setParameter(SingleColumnFilter.PARAMETER_ATTRIBUTE,
				ValueTypeColumnFilter.TYPE_BINOMINAL + "Missing");
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION, "exclude attributes");
		final TableMetaData subset =
				dummyOperator.subsetSelector.getMetaDataSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_BINOMINAL, ValueTypeColumnFilter.TYPE_NON_BINOMINAL,
						ValueTypeColumnFilter.TYPE_BINOMINAL + "Missing",
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL + "Missing", "Special")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_BINOMINAL,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "one attribute");
		dummyOperator2.setParameter(SingleColumnFilter.PARAMETER_ATTRIBUTE,
				ValueTypeColumnFilter.TYPE_BINOMINAL + "Missing");
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION, "exclude attributes");
		final TableMetaData subset2 = dummyOperator2.subsetSelector.getMetaDataSubset(table);

		Assert.assertEquals(subset.labels(), subset2.labels());
	}

	@Test
	public void testNonBinominal() throws UserError {
		Table table = createTable();
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "no missing values");
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final Table subset =
				dummyOperator.subsetSelector.getSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_NON_BINOMINAL,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL + "Missing", "Special")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "no missing values");
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final Table subset2 = dummyOperator2.subsetSelector.getSubset(table);

		RapidAssert.assertEquals(new IOTable(subset), new IOTable(subset2));
	}

	@Test
	public void testNonBinominalMD() throws UserError {
		TableMetaData table = new TableMetaData(new IOTable(createTable()), true);
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "no missing values");
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final TableMetaData subset =
				dummyOperator.subsetSelector.getMetaDataSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_NON_BINOMINAL,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL + "Missing", "Special")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp,
						ValueTypeColumnFilter.TYPE_NON_BINOMINAL));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "no missing values");
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final TableMetaData subset2 = dummyOperator2.subsetSelector.getMetaDataSubset(table);

		Assert.assertEquals(subset.labels(), subset2.labels());
	}

	@Test
	public void testInteger() throws UserError {
		Table table = createTable();
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "regular expression");
		dummyOperator.setParameter(RegexColumnFilter.PARAMETER_REGULAR_EXPRESSION, "Mis");
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final Table subset =
				dummyOperator.subsetSelector.getSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_INTEGER,
						ValueTypeColumnFilter.TYPE_INTEGER + "Missing")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_INTEGER));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "regular expression");
		dummyOperator2.setParameter(RegexColumnFilter.PARAMETER_REGULAR_EXPRESSION, "Mis");
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final Table subset2 = dummyOperator2.subsetSelector.getSubset(table);

		RapidAssert.assertEquals(new IOTable(subset), new IOTable(subset2));
	}

	@Test
	public void testIntegerMD() throws UserError {
		TableMetaData table = new TableMetaData(new IOTable(createTable()), true);
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "regular expression");
		dummyOperator.setParameter(RegexColumnFilter.PARAMETER_REGULAR_EXPRESSION, "Mis");
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final TableMetaData subset =
				dummyOperator.subsetSelector.getMetaDataSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_INTEGER,
						ValueTypeColumnFilter.TYPE_INTEGER + "Missing")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_INTEGER));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "regular expression");
		dummyOperator2.setParameter(RegexColumnFilter.PARAMETER_REGULAR_EXPRESSION, "Mis");
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_SPECIAL_ATTRIBUTES, "true");
		final TableMetaData subset2 = dummyOperator2.subsetSelector.getMetaDataSubset(table);

		Assert.assertEquals(subset.labels(), subset2.labels());
	}

	@Test
	public void testDatetime() throws UserError {
		Table table = createTable();
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "type(s) of values");
		dummyOperator.setParameter(ValueTypeColumnFilter.PARAMETER_VALUE_TYPES, ValueTypeColumnFilter.TYPE_DATE_TIME);
		final Table subset =
				dummyOperator.subsetSelector.getSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_TIME,
						ValueTypeColumnFilter.TYPE_DATE_TIME,
						ValueTypeColumnFilter.TYPE_TIME + "Missing",
						ValueTypeColumnFilter.TYPE_DATE_TIME + "Missing")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_TIME, ValueTypeColumnFilter.TYPE_DATE_TIME));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "type(s) of values");
		dummyOperator2.setParameter(ValueTypeColumnFilter.PARAMETER_VALUE_TYPES, ValueTypeColumnFilter.TYPE_DATE_TIME);
		final Table subset2 = dummyOperator2.subsetSelector.getSubset(table);

		RapidAssert.assertEquals(new IOTable(subset), new IOTable(subset2));
	}

	@Test
	public void testDatetimeMD() throws UserError {
		TableMetaData table = new TableMetaData(new IOTable(createTable()), true);
		final DummyOperator dummyOperator = new DummyOperator(this::getTableSubsetSelector);
		dummyOperator.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "type(s) of values");
		dummyOperator.setParameter(ValueTypeColumnFilter.PARAMETER_VALUE_TYPES, ValueTypeColumnFilter.TYPE_DATE_TIME);
		final TableMetaData subset =
				dummyOperator.subsetSelector.getMetaDataSubset(table.columns(Arrays.asList(ValueTypeColumnFilter.TYPE_TIME,
						ValueTypeColumnFilter.TYPE_DATE_TIME,
						ValueTypeColumnFilter.TYPE_TIME + "Missing",
						ValueTypeColumnFilter.TYPE_DATE_TIME + "Missing")));

		final DummyOperator dummyOperator2 =
				new DummyOperator((op, inp) -> new TableSubsetSelector(op, inp, ValueTypeColumnFilter.TYPE_TIME, ValueTypeColumnFilter.TYPE_DATE_TIME));
		dummyOperator2.setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME, "type(s) of values");
		dummyOperator2.setParameter(ValueTypeColumnFilter.PARAMETER_VALUE_TYPES, ValueTypeColumnFilter.TYPE_DATE_TIME);
		final TableMetaData subset2 = dummyOperator2.subsetSelector.getMetaDataSubset(table);

		Assert.assertEquals(subset.labels(), subset2.labels());
	}

	private static Table createTable() {
		TableBuilder builder = Builders.newTableBuilder(10);
		// every value type
		builder.addReal(ValueTypeColumnFilter.TYPE_REAL, i -> i);
		builder.addInt53Bit(ValueTypeColumnFilter.TYPE_INTEGER, i -> i);
		builder.addTime(ValueTypeColumnFilter.TYPE_TIME, i -> LocalTime.now());
		builder.addDateTime(ValueTypeColumnFilter.TYPE_DATE_TIME, i -> Instant.now());
		builder.addBoolean(ValueTypeColumnFilter.TYPE_BINOMINAL, i -> i % 2 == 0 ? "true" : "false", "true");
		builder.addNominal(ValueTypeColumnFilter.TYPE_NON_BINOMINAL, String::valueOf);
		// every value type filled with missing values
		builder.addReal(ValueTypeColumnFilter.TYPE_REAL + "Missing", i -> Double.NaN);
		builder.addInt53Bit(ValueTypeColumnFilter.TYPE_INTEGER + "Missing", i -> Double.NaN);
		builder.addTime(ValueTypeColumnFilter.TYPE_TIME + "Missing", i -> null);
		builder.addDateTime(ValueTypeColumnFilter.TYPE_DATE_TIME + "Missing", i -> null);
		builder.addBoolean(ValueTypeColumnFilter.TYPE_BINOMINAL + "Missing", i -> null, null);
		builder.addNominal(ValueTypeColumnFilter.TYPE_NON_BINOMINAL + "Missing", i -> null);
		builder.addNominal("Special", i -> i % 2 == 0 ? "yes" : "no");
		builder.addMetaData("Special", ColumnRole.LABEL);
		return builder.build(Belt.defaultContext());
	}

}