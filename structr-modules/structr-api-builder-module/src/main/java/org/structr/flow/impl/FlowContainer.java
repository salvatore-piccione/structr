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

import java.awt.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.impl.rels.DOMNodeFLOWFlowContainer;
import org.structr.flow.impl.rels.FlowContainerBaseNode;
import org.structr.flow.impl.rels.FlowContainerConfigurationFlow;
import org.structr.flow.impl.rels.FlowContainerFlowNode;
import org.structr.flow.impl.rels.FlowContainerPackageFlow;
import org.structr.module.api.DeployableEntity;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 */
public class FlowContainer extends AbstractNode implements DeployableEntity {

	public static final Property<FlowContainerPackage> flowPackage                        = new StartNode<>("flowPackage", FlowContainerPackageFlow.class);
	public static final Property<Iterable<FlowBaseNode>> flowNodes                        = new EndNodes<>("flowNodes", FlowContainerBaseNode.class);
	public static final Property<Iterable<FlowContainerConfiguration>> flowConfigurations = new StartNodes<>("flowConfigurations", FlowContainerConfigurationFlow.class);
	public static final Property<FlowNode> startNode                                      = new EndNode<>("startNode", FlowContainerFlowNode.class).indexed();
	public static final Property<String> name                                             = new StringProperty("name").notNull().indexed();
	public static final Property<Object> effectiveName                                    = new FunctionProperty<>("effectiveName").indexed().unique().notNull().readFunction("if(empty(this.flowPackage), this.name, concat(this.flowPackage.effectiveName, \".\", this.name))").writeFunction("{\r\n\tlet self = Structr.get(\'this\');\r\n\tlet path = Structr.get(\'value\');\r\n\r\n\tfunction getOrCreatePackage(name, path) {\r\n\t\tlet effectiveName = Structr.empty(path) ? name : Structr.concat(path,\".\",name);\r\n\r\n\t\tlet package = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", effectiveName));\r\n\r\n\t\tif (Structr.empty(path)) {\r\n\t\t\t\r\n\t\t\tif (Structr.empty(package)) {\r\n\t\t\t\tpackage = Structr.create(\"FlowContainerPackage\", \"name\", name);\r\n\t\t\t}\r\n\t\t} else {\r\n\t\t\tlet parent = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", path));\r\n\r\n\t\t\tif (Structr.empty(package)) {\r\n\t\t\t\tpackage = Structr.create(\"FlowContainerPackage\", \"name\", name, \"parent\", parent);\r\n\t\t\t}\r\n\t\t}\r\n\r\n\t\treturn package;\r\n\t}\r\n\r\n\tif (!Structr.empty(path)) {\r\n\r\n\t\tif (path.length > 0) {\r\n\r\n\t\t\tlet flowName = null;\r\n\r\n\t\t\tif (path.indexOf(\".\") !== -1) {\r\n\r\n\t\t\t\tlet elements = path.split(\".\");\r\n\r\n\t\t\t\tif (elements.length > 1) {\r\n\r\n\t\t\t\t\tflowName = elements.pop();\r\n\t\t\t\t\tlet currentPath = \"\";\r\n\t\t\t\t\tlet parentPackage = null;\r\n\r\n\t\t\t\t\tfor (let el of elements) {\r\n\t\t\t\t\t\tlet package = getOrCreatePackage(el, currentPath);\r\n\t\t\t\t\t\tparentPackage = package;\r\n\t\t\t\t\t\tcurrentPath = package.effectiveName;\r\n\t\t\t\t\t}\r\n\r\n\t\t\t\t\tself.flowPackage = parentPackage;\r\n\t\t\t\t} else {\r\n\r\n\t\t\t\t\tflowName = elements[0];\r\n\t\t\t\t}\r\n\r\n\t\t\t\tself.name = flowName;\r\n\t\t\t} else {\r\n\r\n\t\t\t\tself.name = path;\r\n\t\t\t}\r\n\r\n\t\t}\r\n\r\n\t}\r\n\r\n}").typeHint("String");
	public static final Property<Boolean> scheduledForIndexing                            = new BooleanProperty("scheduledForIndexing").defaultValue(false);
	public static final Property<Iterable<DOMNode>> repeaterNodes                         = new StartNodes<>("repeaterNodes", DOMNodeFLOWFlowContainer.class);


	public static final View defaultView = new View(FlowContainer.class, PropertyView.Public, name, flowNodes, startNode, flowPackage, effectiveName, scheduledForIndexing, repeaterNodes);
	public static final View uiView      = new View(FlowContainer.class, PropertyView.Ui,     name, flowNodes, startNode, flowPackage, effectiveName, scheduledForIndexing, repeaterNodes);

	private static final Logger logger = LoggerFactory.getLogger(FlowContainer.class);

	@Export
	public Map<String, Object> evaluate(final Map<String, Object> parameters) {

		final FlowEngine engine       = new FlowEngine();
		final Context context         = new Context();
		context.setParameters(parameters);
		final FlowNode entry          = getProperty(startNode);
		final FlowResult result       = engine.execute(context, entry);
		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("error",  result.getError());
		map.put("result", result.getResult());

		return map;
	}

	/*
	@Export
	public void duplicate(final Map<String, Object> parameters) {

		App app = StructrApp.getInstance(securityContext);

		try (Tx tx = app.tx()) {

			PropertyMap props = new PropertyMap();
			props.put(flowPackage, getProperty(flowPackage));
			props.put(name, getProperty(name) + "_copy");

			FlowContainer container = app.create(FlowContainer.class, props);





		} catch (FrameworkException ex) {

			logger.warn("Error while trying to duplicate flow.", ex);
		}



		for (FlowBaseNode node : getProperty(flowNodes)) {

		}

	}
	*/

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("name", this.getName());

		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		super.onCreation(securityContext, errorBuffer);

		this.setProperty(visibleToAuthenticatedUsers, true);
		this.setProperty(visibleToPublicUsers, true);
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		super.onModification(securityContext, errorBuffer, modificationQueue);
		setProperty(scheduledForIndexing, false);
	}

	@Override
	public void onNodeDeletion() {
		deleteChildren();
	}

	private void deleteChildren() {

		final Iterable<FlowBaseNode> nodes                 = getProperty(flowNodes);
		final Iterable<FlowContainerConfiguration> configs = getProperty(flowConfigurations);
		final App app                                      = StructrApp.getInstance();

		try (Tx tx = app.tx()) {
			for (FlowBaseNode node: nodes) {
				app.delete(node);
			}

			for (FlowContainerConfiguration conf: configs) {
				app.delete(conf);
			}

			tx.success();
		} catch (FrameworkException ex) {
			logger.warn("Could not handle onDelete for FlowContainer: " + ex.getMessage());
		}

	}

}
