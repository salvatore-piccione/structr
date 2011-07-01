package org.structr.renderer;

import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.Column;
import org.structr.core.entity.web.Domain;
import org.structr.core.entity.web.HomePage;
import org.structr.core.entity.web.WebNode;

/**
 *
 * @author Christian Morgner
 */
public class DomainRenderer implements NodeRenderer<Domain>
{
	@Override
	public void renderNode(StructrOutputStream out, Domain currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		if(currentNode.isVisible())
		{

			if(currentNode instanceof WebNode)
			{

				WebNode webNode = (WebNode)currentNode;
				HomePage homepage = webNode.getHomePage();

				if(homepage == null)
				{

					out.append("No home page found for ").append(currentNode.getName());

				} else
				{
					homepage.renderNode(out, homepage, editUrl, editNodeId);

				}

			}
		}
	}

	@Override
	public String getContentType(Domain currentNode)
	{
		return ("text/html");
	}
}
