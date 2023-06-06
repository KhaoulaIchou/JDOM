package generateCpp;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class XmlToCpp {
    public static void main(String[] args) throws IOException {
        File xmlFile = new File("src/generateCpp/modelcpp.xml");
        Document document;
        try {
            document = new SAXBuilder().build(xmlFile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Element moduleElement = document.getRootElement();

        generateCPPFiles(moduleElement, null, null);
        Element linksElement = moduleElement.getChild("links");
        handleLinks(linksElement, moduleElement);
    }

    private static void searchNamespaceRecursively(String namespaceName, Element namespaceElement, String sourceClassName, String targetClassName, String sourceMultiplicity, String targetMultiplicity, String associationType, String namespacePath) throws IOException {
        String currentNamespaceName = namespaceElement.getChildText("Name");
        String updatedNamespacePath = namespacePath.isEmpty() ? currentNamespaceName : namespacePath + "::" + currentNamespaceName;
        //String namespacePathh = namespacePath.isEmpty() ? currentNamespaceName : namespacePath + "/" + currentNamespaceName;
        String namespacePathh = namespacePath.isEmpty() ? currentNamespaceName : namespacePath + "/" + currentNamespaceName;
        System.out.println("classsource : "+sourceClassName);
        System.out.println("targetclasse : "+targetClassName);
        if (updatedNamespacePath.equals(namespaceName)) {
            System.out.println("updatedNamespacePath  : "+updatedNamespacePath);
            System.out.println("namespacename  : "+namespaceName);
            Element classesElement = namespaceElement.getChild("Classes");
            if (classesElement != null) {
                for (Element classElement : classesElement.getChildren("Class")) {
                    if (classElement.getChildText("Name").equals(targetClassName)) {
                        if (associationType.equals("Simple")) {
                            handleMultiplicity(sourceMultiplicity, targetMultiplicity, sourceClassName, targetClassName, namespacePathh, namespacePathh);
                            //System.out.println("Source class name "+sourceClassName);
                        }
                    } else if (classElement.getChildText("Name").equals(sourceClassName)) {
                        String className = classElement.getChildText("Name");
                       // System.out.println("Classname "+className);
                        if (associationType.equals("Agregation") || associationType.equals("Composition")) {
                            String fileName = className + ".cpp";
                            File file = new File(namespacePathh + "/" + fileName);
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            StringBuilder previousContent = new StringBuilder();
                            StringBuilder constructorContent = new StringBuilder();
                            StringBuilder newContent = new StringBuilder();
                            String line;
                            boolean insideConstructor = false;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("public:")) {
                                    newContent.append(line).append("\n");
                                    newContent.append("\tstd::vector<" + className + "> " + className.toLowerCase() + "List;\n");
                                    while((line = reader.readLine()) != null && !line.contains("};")) {
                                        if (line.contains(className + "(") && !line.contains("~")) {
                                            constructorContent.append(line).append("\n");
                                            insideConstructor = true;
                                        }
                                        else if (insideConstructor && line.contains("{")) {
                                            constructorContent.append(line).append("\n");
                                            constructorContent.append("\t" + className.toLowerCase() + "List = std::vector<" + className + ">();\n");
                                        }
                                        else if (insideConstructor && line.contains("}")) {
                                            constructorContent.append(line).append("\n");
                                            insideConstructor = false;
                                        }
                                        else if (!insideConstructor){
                                            newContent.append(line).append("\n");
                                        }
                                    }
                                    newContent.append("\t~" + className + "() { }\n");  // destructor
                                    newContent.append("\tstd::vector<" + className + ">& get" + className + "List() {\n");
                                    newContent.append("\t\treturn " + className.toLowerCase() + "List;\n");
                                    newContent.append("\t}\n");
                                    newContent.append("\tvoid set" + className + "List(const std::vector<" + className + ">& list) {\n");
                                    newContent.append("\t\t" + className.toLowerCase() + "List = list;\n");
                                    newContent.append("\t}\n");
                                    newContent.append(constructorContent);
                                    newContent.append("};\n");
                                }
                                else {
                                    previousContent.append(line).append("\n");
                                }
                            }
                            newContent.insert(0, previousContent);
                            FileWriter writer = new FileWriter(file);
                            writer.write("#include <vector>\n\n");
                            writer.write(newContent.toString());
                            reader.close();
                            writer.close();
                        }



                        else if (associationType.equals("Simple")) {
                           // handleMultiplicity(sourceMultiplicity, targetMultiplicity, sourceClassName, targetClassName, namespacePathh, namespacePathh);
                        }
                    }
                }
            }

            Element subNamespacesElement = namespaceElement.getChild("Namespaces");
            if (subNamespacesElement != null) {
                List<Element> subNamespaceElements = subNamespacesElement.getChildren("Namespace");
                for (Element subNamespaceElement : subNamespaceElements) {
                    searchNamespaceRecursively(namespaceName, subNamespaceElement, sourceClassName, targetClassName, sourceMultiplicity, targetMultiplicity, associationType, updatedNamespacePath);
                }
            }
        } else {
            Element subNamespacesElement = namespaceElement.getChild("Namespaces");
            if (subNamespacesElement != null) {
                List<Element> subNamespaceElements = subNamespacesElement.getChildren("Namespace");
                for (Element subNamespaceElement : subNamespaceElements) {
                    searchNamespaceRecursively(namespaceName, subNamespaceElement, sourceClassName, targetClassName, sourceMultiplicity, targetMultiplicity, associationType, updatedNamespacePath);
                }
            }
        }
    }


    private static void handleMultiplicity(String multiplicitySource, String multiplicityCible, String sourceClassName, String targetClassName, String sourceNamespacePath, String targetNamespacePath) throws IOException {
        String fileNameSource = sourceClassName + ".cpp";
        File fileSource = new File(sourceNamespacePath + "/" + fileNameSource);
        StringBuilder contentSource = new StringBuilder();

        if (fileSource.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(fileSource));
            String line;
            while ((line = reader.readLine()) != null) {
                contentSource.append(line).append("\n");
                if (line.contains("private:")) {
                    String newContent = "";
                    switch (multiplicitySource) {
                        case "0..1":
                        case "1":
                            newContent = "\t" + targetClassName + " " + targetClassName.toLowerCase() + ";\n";
                            break;
                        case "*":
                        case "0..*":
                        case "1..*":
                            newContent = "\tstd::vector<" + targetClassName + "> " + targetClassName.toLowerCase() + "List;\n";
                            break;
                    }
                    if (!contentSource.toString().contains(newContent)) {
                        contentSource.append(newContent);
                    }
                }
            }
            reader.close();

            if (contentSource.toString().contains("vector") && !contentSource.toString().contains("#include <vector>")) {
                contentSource.insert(0, "#include <vector>\n\n");
            }
        }

        FileWriter writerSource = new FileWriter(fileSource);
        writerSource.write(contentSource.toString());
        writerSource.close();

        String fileNameTarget = targetClassName + ".cpp";
        File fileTarget = new File(targetNamespacePath + "/" + fileNameTarget);
        StringBuilder contentTarget = new StringBuilder();

        if (fileTarget.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(fileTarget));
            String line;
            while ((line = reader.readLine()) != null) {
                contentTarget.append(line).append("\n");
                if (line.contains("private:")) {
                    String newContent = "";
                    switch (multiplicityCible) {
                        case "0..1":
                        case "1":
                            newContent = "\t" + sourceClassName + " " + sourceClassName.toLowerCase() + ";\n";
                            break;
                        case "*":
                        case "0..*":
                        case "1..*":
                            newContent = "\tstd::vector<" + sourceClassName + "> " + sourceClassName.toLowerCase() + "List;\n";
                            break;
                    }
                    if (!contentTarget.toString().contains(newContent)) {
                        contentTarget.append(newContent);
                    }
                }
            }
            reader.close();

            if (contentTarget.toString().contains("vector") && !contentTarget.toString().contains("#include <vector>")) {
                contentTarget.insert(0, "#include <vector>\n\n");
            }
        }

        FileWriter writerTarget = new FileWriter(fileTarget);
        writerTarget.write(contentTarget.toString());
        writerTarget.close();
    }



    private static void handleLinks(Element linksElement, Element rootElement) throws IOException {
        List<Element> linkElements = linksElement.getChildren("Link");
        for (Element linkElement : linkElements) {
            String sourceClassName = linkElement.getChildText("Source").substring(linkElement.getChildText("Source").lastIndexOf(".") + 1);
            String targetClassName = linkElement.getChildText("Cible").substring(linkElement.getChildText("Cible").lastIndexOf(".") + 1);
            String sourceNamespace = linkElement.getChildText("Source").substring(0, linkElement.getChildText("Source").lastIndexOf("."));
            sourceNamespace = sourceNamespace.replace(".", "::");
            String sourceMultiplicity = linkElement.getChildText("MultiplicitySource");
            String targetMultiplicity = linkElement.getChildText("MultiplicityCible");
            String associationType = linkElement.getAttributeValue("AssociationType");

            Element namespacesElement = rootElement.getChild("Namespaces");
            if (namespacesElement != null) {
                List<Element> namespaceElements = namespacesElement.getChildren("Namespace");
                for (Element namespaceElement : namespaceElements) {
                    searchNamespaceRecursively(sourceNamespace, namespaceElement, sourceClassName, targetClassName, sourceMultiplicity, targetMultiplicity, associationType, "");
                }
            }
        }
    }

    private static void generateCPPFiles(Element element, String parentNamespace, String parentDirectory) throws IOException {
        String elementName = element.getName();

        if (elementName.equals("Module")) {
            Element namespacesElement = element.getChild("Namespaces");
            if (namespacesElement != null) {
                for (Element namespaceElement : namespacesElement.getChildren("Namespace")) {
                    generateCPPFiles(namespaceElement, null, null);
                }
            }
        } else if (elementName.equals("Namespace")) {
            String namespaceName = element.getChildText("Name");
            String currentNamespace = (parentNamespace != null) ? parentNamespace + "::" + namespaceName : namespaceName;
            String currentDirectory = (parentDirectory != null) ? parentDirectory + "/" + namespaceName : namespaceName;
            File directory = new File(currentDirectory);
            directory.mkdirs();
            Element classesElement = element.getChild("Classes");
            if (classesElement != null) {
                for (Element classElement : classesElement.getChildren("Class")) {
                    generateCPPFile(classElement, currentNamespace, currentDirectory);
                }
            }
            Element subNamespacesElement = element.getChild("Namespaces");
            if (subNamespacesElement != null) {
                for (Element subNamespaceElement : subNamespacesElement.getChildren("Namespace")) {
                    generateCPPFiles(subNamespaceElement, currentNamespace, currentDirectory);
                }
            }
        }
    }
    private static void generateCPPFile(Element classElement, String namespace, String directory) {
        String className = classElement.getChildText("Name");
        String visibility = classElement.getChildText("visibility");
        String abstractClass = classElement.getChildText("Abstract");
        String fileName = className + ".cpp";
        String filePath = directory + "/" + fileName;

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#include <iostream>\n\n");

            if (namespace != null) {
                writer.write("namespace " + namespace + " {\n\n");
            }

            writer.write("class " + className);
            Element parentsElement = classElement.getChild("Parents");
            if (parentsElement != null) {
                List<Element> parentElements = parentsElement.getChildren("Parent");
                if (!parentElements.isEmpty()) {
                    writer.write(" :");
                    for (int i = 0; i < parentElements.size(); i++) {
                        Element parentElement = parentElements.get(i);
                        String parentClassName = parentElement.getText().replace(".", "::");
                        writer.write(" " + "public " + parentClassName);
                        if (i < parentElements.size() - 1) {
                            writer.write(",");
                        }
                    }
                }
            }

            writer.write(" {\n");
            writer.write("public:\n");
            Element attributesElement = classElement.getChild("Attributes");
            if (attributesElement != null) {
                for (Element attributeElement : attributesElement.getChildren("Attribute")) {
                    String attributeName = attributeElement.getChildText("Name");
                    String attributeType = attributeElement.getChildText("Type");
                    String attributeVisibility = attributeElement.getAttributeValue("visibility");
                    if (attributeVisibility.equals("public")) {
                        writer.write("\t" + attributeType + " " + attributeName + ";\n");
                        String attributeNameCap = Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
                        writer.write("\t" + attributeType + " get" + attributeNameCap + "() { return " + attributeName + "; }\n");
                        writer.write("\tvoid set" + attributeNameCap + "(" + attributeType + " " + attributeName + ") { this->" + attributeName + " = " + attributeName + "; }\n");
                    }
                }
                writer.write("\n");
            }
            writer.write("\t" + className + "(");
            if (attributesElement != null) {
                List<Element> attributeElements = attributesElement.getChildren("Attribute");
                for (int i = 0; i < attributeElements.size(); i++) {
                    Element attributeElement = attributeElements.get(i);
                    String attributeName = attributeElement.getChildText("Name");
                    String attributeType = attributeElement.getChildText("Type");
                    writer.write(attributeType + " " + attributeName);
                    if (i < attributeElements.size() - 1) {
                        writer.write(", ");
                    }
                }
            }
            writer.write(") {\n");
            if (attributesElement != null) {
                for (Element attributeElement : attributesElement.getChildren("Attribute")) {
                    String attributeName = attributeElement.getChildText("Name");
                    writer.write("\t\tthis->" + attributeName + " = " + attributeName + ";\n");
                }
            }
            writer.write("\t}\n\n");
            writer.write("\t~" + className + "() {\n");
            writer.write("\t\t// Code du destructeur\n");
            writer.write("\t}\n\n");

            Element methodsElement = classElement.getChild("Methods");
            if (methodsElement != null) {
                for (Element methodElement : methodsElement.getChildren("method")) {
                    String methodName = methodElement.getChildText("Name");
                    String methodType = methodElement.getChildText("Type");
                    String methodVisibility = methodElement.getAttributeValue("visibility");
                    String isVirtual = methodElement.getChildText("Virtual");
                    if (methodVisibility != null && methodVisibility.equals("public")) {
                        writer.write("\t");
                        if (isVirtual != null && isVirtual.equals("true")) {
                            writer.write("virtual ");
                        }
                        writer.write(methodType + " " + methodName + "(");

                        Element parametersElement = methodElement.getChild("Parameters");
                        if (parametersElement != null) {
                            List<Element> parameterElements = parametersElement.getChildren("Parameter");
                            for (int i = 0; i < parameterElements.size(); i++) {
                                Element parameterElement = parameterElements.get(i);
                                String parameterName = parameterElement.getChildText("Name");
                                String parameterType = parameterElement.getChildText("Type");
                                writer.write(parameterType + " " + parameterName);
                                if (i < parameterElements.size() - 1) {
                                    writer.write(", ");
                                }
                            }
                        }

                        if (abstractClass != null && abstractClass.equals("true") && isVirtual != null && isVirtual.equals("true")) {
                            writer.write(") = 0;\n");
                        } else {
                            writer.write(") {\n\t\t// Implémentation de la méthode " + methodName + "\n");
                            writer.write("\t\treturn " + getDefaultValue(methodType) + ";\n");
                            writer.write("\t}\n\n");
                        }
                    }
                }
            }

            writer.write("protected:\n");

            if (attributesElement != null) {
                for (Element attributeElement : attributesElement.getChildren("Attribute")) {
                    String attributeName = attributeElement.getChildText("Name");
                    String attributeType = attributeElement.getChildText("Type");
                    String attributeVisibility = attributeElement.getAttributeValue("visibility");
                    if (attributeVisibility.equals("protected")) {
                        writer.write("\t" + attributeType + " " + attributeName + ";\n");
                    }
                }
                writer.write("\n");
            }
            if (methodsElement != null) {
                for (Element methodElement : methodsElement.getChildren("method")) {
                    String methodName = methodElement.getChildText("Name");
                    String methodType = methodElement.getChildText("Type");
                    String methodVisibility = methodElement.getAttributeValue("visibility");
                    String isVirtual = methodElement.getChildText("Virtual");
                    if (methodVisibility != null && methodVisibility.equals("protected")) {
                        writer.write("\t");
                        if (isVirtual != null && isVirtual.equals("true")) {
                            writer.write("virtual ");
                        }
                        writer.write(methodType + " " + methodName + "(");

                        Element parametersElement = methodElement.getChild("Parameters");
                        if (parametersElement != null) {
                            List<Element> parameterElements = parametersElement.getChildren("Parameter");
                            for (int i = 0; i < parameterElements.size(); i++) {
                                Element parameterElement = parameterElements.get(i);
                                String parameterName = parameterElement.getChildText("Name");
                                String parameterType = parameterElement.getChildText("Type");
                                writer.write(parameterType + " " + parameterName);
                                if (i < parameterElements.size() - 1) {
                                    writer.write(", ");
                                }
                            }
                        }

                        if (abstractClass != null && abstractClass.equals("true") && isVirtual != null && isVirtual.equals("true")) {
                            writer.write(") = 0;\n");
                        } else {
                            writer.write(") {\n\t\t// Implémentation de la méthode " + methodName + "\n");
                            writer.write("\t\treturn " + getDefaultValue(methodType) + ";\n");
                            writer.write("\t}\n\n");
                        }
                    }
                }
            }

            writer.write("private:\n");

            if (attributesElement != null) {
                for (Element attributeElement : attributesElement.getChildren("Attribute")) {
                    String attributeName = attributeElement.getChildText("Name");
                    String attributeType = attributeElement.getChildText("Type");
                    String attributeVisibility = attributeElement.getAttributeValue("visibility");
                    if (attributeVisibility.equals("private")) {
                        writer.write("\t" + attributeType + " " + attributeName + ";\n");
                    }
                }
                writer.write("\n");
            }
            if (methodsElement != null) {
                for (Element methodElement : methodsElement.getChildren("method")) {
                    String methodName = methodElement.getChildText("Name");
                    String methodType = methodElement.getChildText("Type");
                    String methodVisibility = methodElement.getAttributeValue("visibility");
                    String isVirtual = methodElement.getChildText("Virtual");
                    if (methodVisibility != null && methodVisibility.equals("private")) {
                        writer.write("\t");
                        if (isVirtual != null && isVirtual.equals("true")) {
                            writer.write("virtual ");
                        }
                        writer.write(methodType + " " + methodName + "(");

                        Element parametersElement = methodElement.getChild("Parameters");
                        if (parametersElement != null) {
                            List<Element> parameterElements = parametersElement.getChildren("Parameter");
                            for (int i = 0; i < parameterElements.size(); i++) {
                                Element parameterElement = parameterElements.get(i);
                                String parameterName = parameterElement.getChildText("Name");
                                String parameterType = parameterElement.getChildText("Type");
                                writer.write(parameterType + " " + parameterName);
                                if (i < parameterElements.size() - 1) {
                                    writer.write(", ");
                                }
                            }
                        }

                        if (abstractClass != null && abstractClass.equals("true") && isVirtual != null && isVirtual.equals("true")) {
                            writer.write(") = 0;\n");
                        } else {
                            writer.write(") {\n\t\t// Implémentation de la méthode " + methodName + "\n");
                            writer.write("\t\treturn " + getDefaultValue(methodType) + ";\n");
                            writer.write("\t}\n\n");
                        }
                    }
                }
            }

            writer.write("};\n\n");

            if (namespace != null) {
                writer.write("}\n");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static String getDefaultValue(String type) {
        if (type.equals("int") || type.equals("short") || type.equals("long") || type.equals("float") || type.equals("double")) {
            return "0";
        } else if (type.equals("char")) {
            return "'\0'";
        } else if (type.equals("bool")) {
            return "false";
        } else {
            return "nullptr";
        }
    }
}
