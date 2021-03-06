/*
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.schema.openapi;

import java.util.List;
import java.util.Map;
import org.structr.schema.export.StructrTypeDefinition;

public class OpenAPIGetSingleOperation extends OpenAPIOperation {

	public OpenAPIGetSingleOperation(final StructrTypeDefinition type, final String view) {

		super(
			// summary
			"Fetches the contents of a single " + type.getName() + " object.",

			// description
			"Returns exactly one " + type.getName() + " object selected by its UUID.",

			// operationId
			"get" + type.getName() + "." + view,

			// tags
			type.getTagsForOpenAPI(),

			// parameters
			List.of(
				new OpenAPIPathParameter("uuid", "The UUID of the desired object", Map.of("type", "string"))
			),

			// requestBody
			null,

			// responses
			Map.of(
				"401", new OpenAPIReference("#/components/responses/forbidden"),
				"404", new OpenAPIReference("#/components/responses/notFound"),
				"200", new OpenAPIRequestResponse("Ok",
					new OpenAPIResultSchema(new OpenAPIReference("#/components/schemas/" + type.getName(), view), true),
					new OpenAPIObjectResultExample(type, view)
				)
			)
		);
	}
}
