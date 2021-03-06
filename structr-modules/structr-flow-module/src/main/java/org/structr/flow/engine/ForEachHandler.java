/*
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.structr.flow.api.*;
import org.structr.flow.impl.FlowAggregate;
import org.structr.flow.impl.FlowDecision;
import org.structr.flow.impl.FlowForEach;
import org.structr.flow.impl.FlowNode;

/**
 *
 */
public class ForEachHandler implements FlowHandler<FlowForEach> {

	@Override
	public FlowElement handle(final Context context, final FlowForEach flowElement) throws FlowException {

		final DataSource dataSource   = flowElement.getDataSource();

		if (dataSource != null) {

			final FlowEngine engine = new FlowEngine(context);
			final FlowNode loopBody = flowElement.getLoopBody();

			if (loopBody != null) {

				final Object data = dataSource.get(context);

				// Special handling for FlowAggregate to ensure it's properly reset for nested loops.
				FlowElement element = loopBody;

				final Context cleanedLoopContext = new Context(context);
				traverseAndEvaluate(element, (el) -> {
					if (el instanceof FlowAggregate) {
						cleanedLoopContext.setData(((FlowAggregate) el).getUuid(), null);
					}
				});

				Context loopContext = new Context(cleanedLoopContext);

				if (data instanceof Iterable) {

					for (final Object o : ((Iterable) data)) {

						// Provide current element data for loop context and write evaluation result into main context data for this loop element
						loopContext.setData(flowElement.getUuid(), o);
						engine.execute(loopContext, loopBody);
						loopContext = openNewContext(context, loopContext, flowElement);

						// Break when an intermediate result or error occurs
						if (context.hasResult() || context.hasError()) {
							break;
						}
					}

				} else {

					// Provide current element data for loop context and write evaluation result into main context data for this loop element
					loopContext.setData(flowElement.getUuid(), data);
					engine.execute(loopContext, loopBody);
				}

				for (Map.Entry<String,Object> entry : getAggregationData(loopContext, flowElement).entrySet()) {
					context.setData(entry.getKey(), entry.getValue());
				}
				context.setData(flowElement.getUuid(), data);

			}

		}

		return flowElement.next();
	}

	private Map<String,Object> getAggregationData(final Context context, final FlowElement flowElement) {
		Map<String,Object> aggregateData = new HashMap<>();

		FlowElement currentElement = ((FlowForEach)flowElement).getLoopBody();

		traverseAndEvaluate(currentElement, (el) -> {
			if (el instanceof FlowAggregate) {

				aggregateData.put(((FlowAggregate) el).getUuid(), context.getData(((FlowAggregate) el).getUuid()));
			}
		});

		return aggregateData;
	}

	private Context openNewContext(final Context context, Context loopContext, final FlowElement flowElement) {
		final Context newContext = new Context(context);

		for (Map.Entry<String,Object> entry : getAggregationData(loopContext, flowElement).entrySet()) {

			newContext.setData(entry.getKey(), entry.getValue());
		}

		return newContext;
	}

	private void traverseAndEvaluate(final FlowElement element, final Consumer<FlowElement> consumer) {

		if (element != null) {

			consumer.accept(element);

			if (element instanceof FlowDecision) {

				final FlowDecision decision = (FlowDecision)element;

				FlowElement decisionElement = decision.getProperty(FlowDecision.trueElement);
				if (decisionElement != null) {

					traverseAndEvaluate(decisionElement, consumer);
				}

				decisionElement = decision.getProperty(FlowDecision.falseElement);
				if (decisionElement != null) {

					traverseAndEvaluate(decisionElement, consumer);
				}

			} else {

				if (element.next() != null) {

					traverseAndEvaluate(element.next(), consumer);
				}
			}
		}
	}

}
