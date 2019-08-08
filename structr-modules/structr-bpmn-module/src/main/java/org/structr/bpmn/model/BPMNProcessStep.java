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
package org.structr.bpmn.model;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;

/**
 *
 */

public abstract class BPMNProcessStep<T> extends AbstractNode {

	public static final Property<BPMNProcessStep> nextStep     = new EndNode<>("nextStep", BPMNProcessStepNext.class);
	public static final Property<BPMNProcessStep> previousStep = new StartNode<>("previousStep", BPMNProcessStepNext.class);
	public static final Property<Boolean> isFinished           = new BooleanProperty("isFinished").indexed();
	public static final Property<Boolean> isManual             = new BooleanProperty("isManual").indexed();
	public static final Property<Boolean> isSuspended          = new BooleanProperty("isSuspended").indexed().hint("This flag can be used to manually suspend a process.");
	public static final Property<Date> dueDate                 = new ISO8601DateProperty("dueDate").indexed();

	public abstract T execute(final Map<String, Object> context) throws FrameworkException;
	public abstract String getStatusText();

	public boolean next(final T t) throws FrameworkException {

		final BPMNProcessStep nextStep = getNextStep(t);
		if (nextStep != null) {

			setProperty(BPMNProcessStep.nextStep, nextStep);

			return true;
		}

		return false;
	}

	public BPMNProcessStep getNextStep(final Object data) throws FrameworkException {

		final PropertyKey nextKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(getClass(), "next", false);
		if (nextKey != null) {

			final Class<BPMNProcessStep> nextType = nextKey.relatedType();
			if (nextType != null) {

				return StructrApp.getInstance(securityContext).create(nextType);
			}
		}

		return null;
	}

	public boolean isSuspended() {
		return getProperty(isSuspended);
	}

	public boolean isFinished() {
		return getProperty(isFinished);
	}

	public void finish() throws FrameworkException {
		setProperty(isFinished, true);
	}

	public void suspend() throws FrameworkException {
		setProperty(isSuspended, true);
	}

	public Object canBeExecuted(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {
		return true;
	}

	public boolean canBeExecuted() throws FrameworkException {

		final Object value = canBeExecuted(securityContext, new LinkedHashMap<>());
		if (value != null && value instanceof Boolean) {

			return (Boolean)value;
		}

		throw new FrameworkException(422, "Return value of canBeExecuted() method must be of type boolean");
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (this instanceof BPMNProcess) {

			// BPMNStart can be created by anyone, do nothing

		} else {

			if (Boolean.TRUE.equals(securityContext.getAttribute("BPMNService"))) {

				// BPMNService is allowed to create instances of intermediate steps, do nothing

			} else {

				throw new FrameworkException(422, "BPMN process must be started with a BPMNStart instance.");
			}
		}
	}

	public void initializeContext() {

		securityContext.getContextStore().setConstant("test", "Hallo");
	}
}