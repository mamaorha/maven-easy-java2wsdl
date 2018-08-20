package co.il.nmh.maven.easy.java2wsdl.merge;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import co.il.nmh.maven.easy.java2wsdl.data.WsdlInfo;

/**
 * @author Maor Hamami
 */

public class WsdlMerger
{
	protected Log log;
	protected String wsdlUrl;
	protected File wsdl;
	protected String service;
	protected List<WsdlInfo> wsdlsInfo;

	public WsdlMerger(Log log, String wsdlUrl, File wsdl, String service, List<WsdlInfo> wsdlsInfo)
	{
		this.log = log;
		this.wsdlUrl = wsdlUrl;
		this.wsdl = wsdl;
		this.service = service;
		this.wsdlsInfo = wsdlsInfo;
	}

	public void merge() throws MojoExecutionException
	{
		if (null == wsdlsInfo || wsdlsInfo.size() == 0)
		{
			log.info("nothing to merge");
			return;
		}

		log.info(String.format("merging %s wsdls into %s with url %s", wsdlsInfo.size(), wsdl.getName(), wsdlUrl));

		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			// new Document
			Document newDoc = builder.newDocument();

			for (WsdlInfo wsdlInfo : wsdlsInfo)
			{
				Document docToMerge = builder.parse(fixFile(wsdlInfo.getWsdl()));
				updateLocation(docToMerge, wsdlInfo.getService());
				merge2File(docToMerge, newDoc);
			}

			updateServiceName(newDoc);
			writeToFile(newDoc);

			finalFixes();
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("failed to merge wsdls - " + e.getMessage());
		}
	}

	private File fixFile(String wsdlPath) throws IOException
	{
		String content = FileUtils.readFileToString(new File(wsdlPath));
		content = content.replaceAll("wsdl:", "");

		int last = wsdlPath.lastIndexOf("/");

		String folder = wsdlPath.substring(0, last + 1) + "tmpWsdl/";
		String name = wsdlPath.substring(last);

		new File(folder).mkdir();

		File tempFile = new File(folder + name);
		FileUtils.writeStringToFile(tempFile, content, "UTF-8");

		return tempFile;
	}

	private void updateLocation(Document docToMerge, String service)
	{
		NodeList addressNodes = docToMerge.getElementsByTagName("soap:address");// location

		for (int i = 0; i < addressNodes.getLength(); i++)
		{
			Node addressNode = addressNodes.item(i);
			Node i_location = addressNode.getAttributes().getNamedItem("location");

			String newLocation = this.wsdlUrl + service;
			i_location.setTextContent(newLocation);
		}
	}

	private void merge2File(Document docToMerge, Document newDoc)
	{
		mergeDocByElement(docToMerge, newDoc, "message");
		mergeDocByElement(docToMerge, newDoc, "portType");
		mergeDocByElement(docToMerge, newDoc, "binding");
		mergeDocByFirstElement(docToMerge, newDoc, "service");
		mergeDocByFirstElement(docToMerge, newDoc, "schema");
	}

	private void mergeDocByElement(Document docToMerge, Document newDoc, String element)
	{
		Element toMergeRoot = docToMerge.getDocumentElement();
		NodeList elementNodesToMerge = toMergeRoot.getElementsByTagName(element);
		Element newDocRoot = newDoc.getDocumentElement();

		for (int i = 0; i < elementNodesToMerge.getLength(); i++)
		{
			Node cloned = newDoc.importNode(elementNodesToMerge.item(i), true);
			newDocRoot.appendChild(cloned);
		}
	}

	private void mergeDocByFirstElement(Document docToMerge, Document newDoc, String element)
	{
		Node serviceNode1 = newDoc.getElementsByTagName(element).item(0);
		Node serviceNode2 = docToMerge.getElementsByTagName(element).item(0);
		mergeNodes(serviceNode2, serviceNode1, newDoc);
	}

	private void mergeNodes(Node toMerge, Node merged, Document newDoc)
	{
		NodeList listNodesToMerge = toMerge.getChildNodes();

		for (int i = 0; i < listNodesToMerge.getLength(); i++)
		{
			Node cloned = newDoc.importNode(listNodesToMerge.item(i), true);
			merged.appendChild(cloned);
		}
	}

	private void updateServiceName(Document newDoc)
	{
		Node serviceNode1 = newDoc.getElementsByTagName("service").item(0);
		Node serviceName = serviceNode1.getAttributes().getNamedItem("name");

		serviceName.setTextContent(service);
	}

	private void writeToFile(Document newDoc) throws TransformerFactoryConfigurationError, TransformerException, IOException
	{
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		BufferedWriter wsdlFile = new BufferedWriter(new FileWriter(wsdl, false));
		StreamResult result = new StreamResult(wsdlFile);
		DOMSource source = new DOMSource(newDoc);
		transformer.transform(source, result);
	}

	private void finalFixes() throws IOException
	{
		List<String> existNodes = new ArrayList<String>();

		String content = FileUtils.readFileToString(wsdl);
		content = content.replaceAll(":wsdl", "");

		StringBuilder xml = new StringBuilder();

		int messageIndex = content.indexOf("<message");

		while (messageIndex > -1)
		{
			xml.append(content.substring(0, messageIndex));

			int endMessage = content.indexOf("</message>");

			String tempMessage = content.substring(messageIndex, endMessage + "</message>".length());

			int name = tempMessage.indexOf("name=");

			if (name > -1)
			{
				String currName = tempMessage.substring(name + "name=".length() + 1);
				currName = currName.substring(0, currName.indexOf("\""));

				if (!existNodes.contains(currName))
				{
					existNodes.add(currName);
					xml.append(tempMessage);
				}
			}

			content = content.substring(endMessage + "</message>".length());
			messageIndex = content.indexOf("<message");
		}

		xml.append(content);
		FileUtils.writeStringToFile(wsdl, xml.toString(), "UTF-8");
	}
}
