package pack;

import org.jdom2.JDOMException;

import java.io.File;
import java.io.IOException;

public interface JDOMInterface {
    public void XmlToJava(File file) throws IOException, JDOMException;
}
