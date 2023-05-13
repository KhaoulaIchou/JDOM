package pack;

import java.io.File;
import java.io.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class XmlToJava {
    private File inputFile;
    private Document outputDoc;
    private Element currentPackageElem;
    private Element currentClassOrInterfaceElem;
    private List<Element> currentMembers;
    private int currentMemberIndex;

    public XmlToJava(File inputFile) {
        this.inputFile = inputFile;
        this.outputDoc = new Document();
        this.currentPackageElem = null;
        this.currentClassOrInterfaceElem = null;
        this.currentMembers = null;
        this.currentMemberIndex = -1;
    }

    public void transform() throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document inputDoc = builder.build(inputFile);
        Element inputRoot = inputDoc.getRootElement();
        String packageName = inputRoot.getAttributeValue("name");
        if (packageName == null)
        packageName = "defaultPackage";
        currentPackageElem = new Element("package");
        currentPackageElem.setAttribute("name", packageName);
        outputDoc.setRootElement(currentPackageElem);
        List<Element> inputChildren = inputRoot.getChildren();
        for (Element inputChild : inputChildren) {
            transformClassOrInterface(inputChild);
        }
        currentPackageElem = null;
        currentClassOrInterfaceElem = null;
        currentMembers = null;
        currentMemberIndex = -1;

        // If no classes or interfaces were transformed, add an empty comment to the package element
        if (currentPackageElem.getChildren().isEmpty()) {
            currentPackageElem.addContent(new Comment("No classes or interfaces found in the input file"));
        }
    }


    private void transformClassOrInterface(Element inputElem) {
        String type = inputElem.getAttributeValue("type");
        String name = inputElem.getAttributeValue("name");
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid name attribute");
        }
        if ("Class".equals(type)) {
            currentClassOrInterfaceElem = new Element("class");
        } else if ("Interface".equals(type)) {
            currentClassOrInterfaceElem = new Element("interface");
        } else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        currentClassOrInterfaceElem.setAttribute("name", name);
        currentMembers = new ArrayList<>();
        currentMemberIndex = -1;
        List<Element> inputChildren = inputElem.getChildren();
        for (Element inputChild : inputChildren) {
            if ("attribute".equals(inputChild.getName())) {
                transformAttribute(inputChild);
            } else if ("method".equals(inputChild.getName())) {
                transformMethod(inputChild);
            } else {
                throw new IllegalArgumentException("Invalid child element: " + inputChild.getName());
            }
        }
        currentClassOrInterfaceElem.addContent(currentMembers);
        currentPackageElem.addContent(currentClassOrInterfaceElem);
        currentClassOrInterfaceElem = null;
        currentMembers = null;
        currentMemberIndex = -1;
    }




    private void transformAttribute(Element inputElem) {
        String visibility = inputElem.getAttributeValue("visibility");
        String type = inputElem.getAttributeValue("type");
        String name = inputElem.getAttributeValue("name");
        Element outputElem = new Element("attribute");
        if(name != null) {
            outputElem.setAttribute("name", name);
        }
        if(type != null) {
            outputElem.setAttribute("type", getTypeName(type));
        }
        if(visibility != null) {
            outputElem.setAttribute("visibility", visibility);
        }
        currentMembers.add(outputElem);
    }



    private void transformMethod(Element inputElem) {
        String visibility = inputElem.getAttributeValue("visibility");
        String typeRetour = inputElem.getAttributeValue("returnType");
        String methodName = inputElem.getAttributeValue("name");
        List<Element> parametres = inputElem.getChildren("parameter");
        Element outputElem = new Element("method");
        if (methodName != null) {
            outputElem.setAttribute("name", methodName);
        }
        if (typeRetour != null) {
            outputElem.setAttribute("returnType", getTypeName(typeRetour));
        }
        if (visibility != null) {
            outputElem.setAttribute("visibility", visibility);
        }
        List<Element> outputParams = new ArrayList<>();
        for (Element parametre : parametres) {
            String paramType = parametre.getAttributeValue("type");
            String paramName = parametre.getAttributeValue("name");
            Element outputParam = new Element("parameter");
            if (paramName != null) {
                outputParam.setAttribute("name", paramName);
            }
            if (paramType != null) {
                outputParam.setAttribute("type", getTypeName(paramType));
            }
            outputParams.add(outputParam);
        }
        outputElem.addContent(outputParams);
        currentMembers.add(outputElem);
    }

    private String getTypeName(String inputType) {
        if ("int".equals(inputType)) {
            return "int";
        } else if ("float".equals(inputType)) {
            return "float";
        } else if ("double".equals(inputType)) {
            return "double";
        } else if ("String".equals(inputType)) {
            return "String";
        } else if ("boolean".equals(inputType)) {
            return "boolean";
        } else {
            // Assume it's a class name
            return "L" + inputType.replace(".", "/") + ";";
        }
    }

    public void writeOutputFile(File outputFile) throws IOException {
        FileWriter writer = new FileWriter(outputFile);
        writer.write("package " + currentPackageElem.getAttributeValue("name") + ";\n\n");
        for (Content content : currentPackageElem.getContent()) {
            if (content instanceof Element) {
                Element element = (Element) content;
                if ("class".equals(element.getName()) || "interface".equals(element.getName())) {
                    String type = element.getName();
                    String name = element.getAttributeValue("name");
                    writer.write("\n" + type + " " + name + " {\n");
                    for (Content member : element.getContent()) {
                        if (member instanceof Element) {
                            Element memberElement = (Element) member;
                            String visibility = memberElement.getAttributeValue("visibility");
                            String memberType = memberElement.getName();
                            String memberName = memberElement.getAttributeValue("name");
                            if ("attribute".equals(memberType)) {
                                String typeName = memberElement.getAttributeValue("type");
                                writer.write("\t" + visibility + " " + typeName + " " + memberName + ";\n");
                            } else if ("method".equals(memberType)) {
                                String returnType = memberElement.getAttributeValue("returnType");
                                writer.write("\t" + visibility + " " + returnType + " " + memberName + "() {\n");
                                writer.write("\t\t// TODO: implement method\n");
                                writer.write("\t}\n");
                            } else {
                                throw new IllegalArgumentException("Invalid member type: " + memberType);
                            }
                        }
                    }
                    writer.write("}\n");
                } else {
                    throw new IllegalArgumentException("Invalid element type: " + element.getName());
                }
            }
        }
        writer.close();
    }

}

