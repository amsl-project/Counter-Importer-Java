package ubl.amsl.technology.counter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

public class ResourceResolver implements LSResourceResolver {

	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
			String baseURI) {
		try {
			File schemaFile = new File(FileLoader.getRootFolder() + "/schemas/counter/counterElements4_0.xsd");
			InputStream resourceAsStream = new FileInputStream(schemaFile);
			return new Input(publicId, systemId, resourceAsStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}