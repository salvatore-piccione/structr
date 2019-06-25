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
package org.structr.flow.api;

import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;

/**
 *
 */
public interface FlowHandler<T extends FlowElement> {

	/**
	 * Handles the given flow element and returns the next
	 * element in the execution flow.
	 *
	 * @param context
	 * @param flowElement
	 *
	 * @return the next element or null
	 */
	FlowElement handle(final Context context, final T flowElement) throws FlowException;
}