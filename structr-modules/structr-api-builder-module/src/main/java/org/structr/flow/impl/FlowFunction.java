/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.flow.impl;

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.transform.APIBuilderModule;

public class FlowFunction extends Function<Object, Object> {

	public static final String USAGE    = "Usage: ${flow(name)}";
	public static final String USAGE_JS = "Usage: ${{ Structr.flow(name) }}";

	private APIBuilderModule parent     = null;

	public FlowFunction(final APIBuilderModule parent) {
		this.parent = parent;
	}

	@Override
	public String getName() {
		return "flow-engine";
	}

	@Override
	public String getRequiredModule() {
		return parent.getName();
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				final String name                                     = (String)sources[0];
				final FlowContainer container = StructrApp.getInstance(ctx.getSecurityContext()).nodeQuery(FlowContainer.class).and(FlowContainer.effectiveName, name).getFirst();
				Map<String, Object> parameters                        = null;

				if (sources.length > 1 && sources[1] instanceof Map) {
					parameters = (Map)sources[1];
				}

				if (container != null) {

					final FlowNode node = container.getProperty(FlowContainer.startNode);
					if (node != null) {

						final Context context = new Context(caller instanceof GraphObject ? (GraphObject)caller : null);

						// Inject given parameter object into context
						if (parameters != null) {

							for (Map.Entry<String, Object> entry : parameters.entrySet()) {
								context.setParameter(entry.getKey(), entry.getValue());
							}

						} else {

							// If parameters are given in key,value format e.g. from StructrScript
							if (sources.length >= 3 && sources.length % 2 != 0) {

								for (int c = 1; c < sources.length; c += 2) {
									context.setParameter(sources[c].toString(), sources[c + 1]);
								}
							}
						}

						final FlowEngine engine = new FlowEngine(context);
						final FlowResult result = engine.execute(node);

						return result.getResult();

					} else {

						logger.warn("FlowContainer {} does not have a start node set", container.getUuid());
					}

				} else {

					logger.error("FlowContainer {} does not exist", name);
					throw new FrameworkException(422, "FlowContainer " + name + " does not exist");
				}
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return (inJavaScriptContext ? USAGE_JS : USAGE);
	}

	@Override
	public String shortDescription() {
		return "Returns the evaluation result of the Flow with the given name.";
	}
}
