/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt.mapper;

import java.util.function.Function;
import org.structr.api.graph.Node;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.wrapper.NodeWrapper;

/**
 *
 */
public class PathNodeMapper implements Function<PrefetchingNodeMapper, Node> {

	private BoltDatabaseService db = null;

	public PathNodeMapper(final BoltDatabaseService db) {
		this.db            = db;
	}

	@Override
	public Node apply(final PrefetchingNodeMapper t) {

		final NodeWrapper node = NodeWrapper.newInstance(db, t.getNode());

		t.prefetch(db, node);

		// user of this mapper is only interested in nodes
		return node;
	}
}