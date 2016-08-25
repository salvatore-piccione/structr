/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.Predicate;
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;

/**
 *
 *
 */
public class OneStartpoint<S extends NodeInterface> extends AbstractEndpoint implements Source<Relationship, S> {

	private Relation<S, ?, OneStartpoint<S>, ?> relation = null;

	public OneStartpoint(final Relation<S, ?, OneStartpoint<S>, ?> relation) {
		this.relation = relation;
	}

	@Override
	public S get(final SecurityContext securityContext, final NodeInterface node, final Predicate<GraphObject> predicate) {

		/*
		final Iterable<Node> nodes        = node.getNode().getRelatedNodes(Direction.INCOMING, relation, relation.getSourceType().getSimpleName());
		final Iterator<Node> iterator     = nodes.iterator();

		if (iterator.hasNext()) {
			return nodeFactory.instantiate(iterator.next());
		}
		*/

		final NodeFactory<S> nodeFactory = new NodeFactory<>(securityContext);
		final Relationship rel           = getRawSource(securityContext, node.getNode(), predicate);

		if (rel != null) {
			return nodeFactory.instantiate(rel.getStartNode(), rel);
		}

		return null;
	}

	@Override
	public Object set(final SecurityContext securityContext, final NodeInterface targetNode, final S sourceNode) throws FrameworkException {

		// let relation check multiplicity
		relation.ensureCardinality(securityContext, sourceNode, targetNode);

		if (sourceNode != null && targetNode != null) {

			final String storageKey = sourceNode.getName() + relation.name() + targetNode.getName();

			return StructrApp.getInstance(securityContext).create(sourceNode, targetNode, relation.getClass(), getNotionProperties(securityContext, relation.getClass(), storageKey));
		}

		return null;
	}

	@Override
	public Relationship getRawSource(final SecurityContext securityContext, final Node dbNode, final Predicate<GraphObject> predicate) {
		return getSingle(securityContext, dbNode, relation, Direction.INCOMING, relation.getSourceType());
	}

	@Override
	public boolean hasElements(SecurityContext securityContext, Node dbNode, final Predicate<GraphObject> predicate) {
		return getRawSource(securityContext, dbNode, predicate) != null;
	}
}
