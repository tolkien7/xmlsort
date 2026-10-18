import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLSort {

	private XMLSort() {
	}

	public static Document sortXml(Document oldDocument) throws TransformerException {
		DocumentBuilder builder;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new TransformerException("It is not possible to create input xml document", ex);
		}
		Document newDocument = builder.newDocument();
		newDocument.appendChild(sortNode(oldDocument.getDocumentElement(), newDocument));
		return newDocument;
	}

	private static Node sortNode(Node oldNode, Document newDocument) {
		String name = oldNode.getNodeName();
		Node newNode = newDocument.createElement(name);
		sortAttributes(oldNode, newNode);
		sortNodes(oldNode, newNode, newDocument);
		return newNode;
	}

	private static void sortAttributes(Node oldNode, Node newNode) {
		List<Node> newAttributes = new LinkedList<>();
		NamedNodeMap oldAttributes = oldNode.getAttributes();
		for (int i = 0; i < oldAttributes.getLength(); i++) {
			newAttributes.add(oldAttributes.item(i));
		}

		if (newNode instanceof Element) {
			sortAttributesCollection(newAttributes).stream()
					.forEach(attr -> ((Element) newNode).setAttribute(attr.getNodeName(), attr.getNodeValue()));
		}
	}

	private static List<Node> sortAttributesCollection(List<Node> attributes) {
		Collections.sort(attributes, (a1, a2) -> a1.getNodeName().compareTo(a2.getNodeName()));
		return attributes;
	}

	private static void sortNodes(Node oldNode, Node newNode, Document newDocument) {
		List<Node> newChildren = new LinkedList<>();
		NodeList oldChildren = oldNode.getChildNodes();
		for (int i = 0; i < oldChildren.getLength(); i++) {
			Node child = oldChildren.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				newChildren.add(sortNode(child, newDocument));
			} else if (child.getNodeType() == Node.TEXT_NODE) {
				processTextNode(newNode, newDocument, child);
			}
		}

		sortNodesCollection(newChildren).forEach(newNode::appendChild);
	}

	private static void processTextNode(Node newNode, Document newDocument, Node child) {
		String textContent = child.getTextContent();
		if (textContent != null && !"".equals(textContent.trim())) {
			Node newTextNode = newDocument.createTextNode(textContent);
			newNode.appendChild(newTextNode);
		}
	}

	private static List<Node> sortNodesCollection(List<Node> childNodes) {
		Collections.sort(childNodes, (n1, n2) -> getNodeString(n1).compareTo(getNodeString(n2)));
		return childNodes;
	}

	private static String getNodeString(Node node) {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		sb.append(node.getNodeName());

		NamedNodeMap oldAttributes = node.getAttributes();
		for (int i = 0; i < oldAttributes.getLength(); i++) {
			sb.append(" ").append(getAttributeString(oldAttributes.item(i)));
		}

		String content = node.getTextContent();
		if (content == null || "".equals(content)) {
            sb.append("/>");
		} else {
			sb.append(">");
			sb.append(content);
			sb.append("</");
			sb.append(node.getNodeName());
			sb.append(">");
		}

		return sb.toString();
	}

	private static String getAttributeString(Node attribute) {
		return attribute.getNodeName() + "=\"" + attribute.getNodeValue() + "\"";
	}

	public static String sortXmlString(String oldXmlString) throws TransformerException {
		Document oldDocument;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(oldXmlString));
			oldDocument = builder.parse(is);
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new TransformerException("Not possible to parse input xml", ex);
		}

		Document newDocument = sortXml(oldDocument);

		try (StringWriter writer = new StringWriter()) {
			Transformer tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			tf.transform(new DOMSource(sortXml(newDocument)), new StreamResult(writer));
			return writer.toString().replace("\r", "").trim();
		} catch (IOException ex) {
			throw new TransformerException("Not possible to sort xml", ex);
		}
	}

	public static void sortXmlFile(String inputFilename, String outputFilename) throws TransformerException {
		String inputXmlString;
		try {
			inputXmlString = Files.readAllLines(Paths.get(inputFilename)).stream().collect(Collectors.joining("\n"));
		} catch (IOException ex) {
			throw new TransformerException("Not possible to read from input file", ex);
		}

		String outputXmlString = sortXmlString(inputXmlString);
		try {
			Files.write(Paths.get(outputFilename), Arrays.asList(outputXmlString.split("\n")));
		} catch (IOException ex) {
			throw new TransformerException("Not possible to write to output file", ex);
		}
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("No input and/or output file");
			System.exit(1);
		}

		try {
			sortXmlFile(args[0], args[1]);
		} catch (TransformerException ex) {		
			System.err.println("It is not possible to process xml file");
			System.exit(2);
		}
	}
}
