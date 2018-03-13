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
package org.structr.schema.parser;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.property.CollectionNotionProperty;
import org.structr.core.property.EntityNotionProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class NotionPropertyParser extends PropertySourceGenerator {

	private final Set<String> properties = new LinkedHashSet<>();
	private boolean isPropertySet  = false;
	private boolean isAutocreate   = false;
	private String parameters      = "";
	private String propertyType    = null;
	private String relatedType     = null;
	private String baseProperty    = null;
	private String multiplicity    = null;

	public NotionPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return propertyType;
	}

	@Override
	public String getValueType() {
		return relatedType;
	}

	@Override
	public String getUnqualifiedValueType() {
		return relatedType;
	}

	@Override
	public String getPropertyParameters() {
		return parameters;
	}

	@Override
	public Type getKey() {
		return Type.Notion;
	}

	@Override
	public void parseFormatString(final Schema entity, String expression) throws FrameworkException {

		if (StringUtils.isBlank(expression)) {

			//reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), expression, "invalid_property_definition", "Empty notion property expression."));
			throw new FrameworkException(422, "Empty notion property expression for property " + source.getPropertyName() + ".", new InvalidPropertySchemaToken(entity.getClassName(), source.getPropertyName(), expression, "invalid_property_definition", "Empty notion property expression for property " + source.getPropertyName() + "."));
		}

		final StringBuilder buf = new StringBuilder();
		final String[] parts    = expression.split("[, ]+");

		if (parts.length > 0) {

			boolean isBuiltinProperty = false;
			baseProperty              = parts[0];
			multiplicity              = entity.getMultiplicity(baseProperty);

			if (multiplicity != null) {

				// determine related type from relationship
				relatedType  = entity.getRelatedType(baseProperty);

				switch (multiplicity) {

					case "1X":
						// this line exists because when a NotionProperty is set up for a builtin propery
						// (like for example "owner", there must not be the string "Property" appended
						// to the property name, and the SchemaNode returns the above "extended" multiplicity
						// string when it has detected a fallback property name like "owner" from NodeInterface.
						isBuiltinProperty = true; // no break!
					case "1":
						propertyType = EntityNotionProperty.class.getSimpleName();
						break;

					case "*X":
						// this line exists because when a NotionProperty is set up for a builtin propery
						// (like for example "owner", there must not be the string "Property" appended
						// to the property name, and the SchemaNode returns the above "extended" multiplicity
						// string when it has detected a fallback property name like "owner" from NodeInterface.
						isBuiltinProperty = true; // no break!
					case "*":
						propertyType = CollectionNotionProperty.class.getSimpleName();
						break;

					default:
						break;
				}

				buf.append(", ");
				buf.append(entity.getClassName());
				buf.append(".");
				buf.append(baseProperty);

				// append "Property" only if it is NOT a builtin property!
				if (!isBuiltinProperty) {
					buf.append("Property");
				}

				buf.append(",");

				final boolean isBoolean = (parts.length == 3 && ("true".equals(parts[2].toLowerCase())));
				isAutocreate            = isBoolean;

				// use PropertyNotion when only a single element is given
				if (parts.length == 2 || isBoolean) {

					buf.append(" new PropertyNotion(");
					isPropertySet = false;

				} else {

					buf.append(" new PropertySetNotion(");
					isPropertySet = true;
				}

				for (int i=1; i<parts.length; i++) {

					String propertyName     = parts[i];
					String fullPropertyName = propertyName;

					if (!"true".equals(propertyName.toLowerCase()) && !propertyName.contains(".")) {

						buf.append(relatedType);
						buf.append(".");

						fullPropertyName = relatedType + "." + fullPropertyName;
					}

					fullPropertyName = extendPropertyName(fullPropertyName, null);

					properties.add(fullPropertyName);

					propertyName = extendPropertyName(propertyName, relatedType);

					buf.append(propertyName);

					if (i < parts.length-1) {
						buf.append(", ");
					}
				}

				buf.append(")");

			} else {

				throw new FrameworkException(422, "Invalid notion property expression for property " + source.getPropertyName() +  ".", new InvalidPropertySchemaToken(entity.getClassName(), source.getPropertyName(), expression, "invalid_property_definition", "Invalid notion property expression for property " + source.getPropertyName() + "."));
			}

			if (properties.isEmpty()) {
				throw new FrameworkException(422, "Invalid notion property expression for property " + source.getPropertyName() +  ".", new InvalidPropertySchemaToken(entity.getClassName(), source.getPropertyName(), expression, "invalid_property_definition", "Invalid notion property expression for property " + source.getPropertyName() + ", notion must define at least one property."));
			}
		}


		parameters = buf.toString();
	}

	private String extendPropertyName(final String propertyName, final String relatedType) throws FrameworkException {
		Class type;

		if (propertyName.contains(".")) {

			String[] typeAndKey = StringUtils.split(propertyName, ".");
			type = SchemaHelper.getEntityClassForRawType(typeAndKey[0]);

			if (type != null && SchemaHelper.isDynamic(type.getSimpleName(), typeAndKey[1])) {
				return propertyName + "Property";
			}

		} else {

			type = SchemaHelper.getEntityClassForRawType(relatedType);

			if (type != null && SchemaHelper.isDynamic(type.getSimpleName(), propertyName)) {
				return propertyName + "Property";
			}
		}

		if (propertyName.startsWith("_")) {
			return propertyName.substring(1) + "Property";
		}

		return propertyName;
	}

	public boolean isPropertySet() {
		return isPropertySet;
	}

	public Set<String> getProperties() {
		return properties;
	}

	public boolean isAutocreate() {
		return isAutocreate;
	}

	public String getBaseProperty() {
		return baseProperty;
	}

	public String getMultiplicity() {
		return multiplicity;
	}
}
