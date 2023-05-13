package pack;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JDOMClass implements JDOMInterface{
    private void treatPackage(Element element, String parentPackageName) throws IOException {
             if (element.getAttributeValue("type").equals("package")) {
                 String packageName = element.getName();
                 String fullPackageName = null;
                 String importName = null;
                 //if (element.getParentElement().getName().equals("module"))
                 fullPackageName = parentPackageName.isEmpty() ? packageName : parentPackageName + "." + packageName;

                 System.out.println("package " + fullPackageName + ";");
                 StringBuilder fullNameBuilder = new StringBuilder();
                 File packagefilsDir = new File("src/" + fullPackageName.replace(".", "/"));
                 boolean isCreated = packagefilsDir.mkdir();

                 if (isCreated) {
                     System.out.println("Le package a été créé avec succès.");
                 } else {
                     System.out.println("Impossible de créer le package.");
                 }
                 List<Element> children = element.getChildren();
                 for (Element child : children) {
                     if (child.getAttributeValue("type").equals("package")) {
                         treatPackage(child, fullPackageName);
                     } else if (child.getAttributeValue("type").equals("Class") || child.getAttributeValue("type").equals("Interface")) {
                         String visibility = null;
                         if (child.getAttributeValue("visibility").equals("public") || child.getAttributeValue("visibility").equals("package")) {
                             if(child.getAttributeValue("visibility").equals("public"))
                                 visibility = "public";
                             else if (child.getAttributeValue("visibility").equals("package"))
                                 visibility = "";

                             String className = child.getName();
                             String classFileName = className + ".java";
                             File classFile = new File(packagefilsDir.getAbsolutePath() + "/" + classFileName);
                             FileWriter packagewriter = new FileWriter(classFile);
                             Element parentPackage = child.getChild("parent");
                             Element implementPackage = child.getChild("implements");
                             List<Element> members = child.getChildren();
                             if (parentPackage != null) {
                                 String text = parentPackage.getText().replaceAll(".*\\.", "");
                                 char[] charArray = text.toCharArray();
                                 int index = charArray.length - 1;
                                 while (index >= 0 && !Character.isUpperCase(charArray[index])) {
                                     index--;
                                 }
                                 String parentClassName = text.substring(index);
                                 packagewriter.write(String.format("package %s;\n", fullPackageName));
                                 packagewriter.write(String.format("import %s;\n", parentPackage.getText()));
                                 packagewriter.write(String.format("%s class %s extends %s {\n", visibility, className, parentClassName));
                                 treatMembers(members, packagewriter);
                             } else if (implementPackage != null) {
                                 String text = implementPackage.getText().replaceAll(".*\\.", "");
                                 char[] charArray = text.toCharArray();
                                 int index = charArray.length - 1;
                                 while (index >= 0 && !Character.isUpperCase(charArray[index])) {
                                     index--;
                                 }
                                 String implementClassName = text.substring(index);
                                 packagewriter.write(String.format("package %s;\n", fullPackageName));
                                 String interfaceName = implementPackage.getText();
                                 packagewriter.write(String.format("import %s;\n", implementPackage.getText()));
                                 packagewriter.write(String.format("%s class %s implements %s {\n", visibility, className, implementClassName));
                                 treatMembers(members, packagewriter);
                             } else {
                                 if (child.getAttributeValue("visibility").equals("public"))
                                     packagewriter.write(String.format("%s %s %s {\n", visibility, child.getAttributeValue("type").toLowerCase(), className));
                             }


                             packagewriter.write("}\n");
                             packagewriter.close();
                         }
                     }
                 }
             }
    }
    public void treatMembers(List<Element> members, FileWriter writer) throws IOException {
        for (Element member : members) {
            //String memberName = member.getName();
            String memberVisibility = member.getAttributeValue("visibility");
            //String membertypeRetour = member.getAttributeValue("type");
            String memberType = member.getAttributeValue("typemembre");
            System.out.println(memberType + " " + memberVisibility);
            if (memberType!= null && memberType!= null && memberType.equals("method") && !member.getName().equals("parent") && !member.getName().equals("implements")) {
                String returnType = member.getAttributeValue("typeRetour") != null ? member.getAttributeValue("typeRetour") : "void";
                String methodName = member.getName();

                writer.write(String.format("\t%s %s %s(", memberVisibility, returnType, methodName));
                List<Element> parameters = member.getChildren("parameter");
                for (int i = 0; i < parameters.size(); i++) {
                    Element parameter = parameters.get(i);
                    String parameterType = parameter.getAttributeValue("type");
                    String parameterName = parameter.getAttributeValue("nom");
                    writer.write(String.format("%s %s", parameterType, parameterName));
                    if (i < parameters.size() - 1) {
                        writer.write(", ");
                    }
                }
                if(member.getParentElement().getAttributeValue("type").equals("Class")){
                    System.out.println("classname "+member.getParentElement().getName()+" type "+member.getParentElement().getAttributeValue("type"));
                    if (member.getAttribute("typeRetour") != null) {
                        writer.write("){\n");
                        String typeRetour = member.getAttributeValue("typeRetour");
                        Object valeurParDefaut = null;

                        if (typeRetour.equals("int") || typeRetour.equals("float")) {
                            valeurParDefaut = 0;
                        } else if (typeRetour.equals("double")) {
                            valeurParDefaut = 0.0;
                        } else if (typeRetour.equals("boolean")) {
                            valeurParDefaut = false;
                        } else if (typeRetour.equals("String")) {
                            valeurParDefaut = null;
                        } else {
                            valeurParDefaut = null;
                        }
                        writer.write("return "+valeurParDefaut+";\n");
                        writer.write("}\n");
                    }else{
                        writer.write("){\n");
                        writer.write("}\n");
                    }
                }else
                    writer.write(");\n");
            } else if (memberType!=null && memberType.equals("attribute") && !member.getName().equals("parent") && !member.getName().equals("implements")){

                String value = member.getText();
                Attribute typeAttr = member.getAttribute("type");
                String attributeType = typeAttr != null ? typeAttr.getValue() : "String";
                String attributeName = member.getName();
                if (attributeType.equals("String"))
                    writer.write(String.format("\t%s %s %s = \"%s\";\n", memberVisibility, attributeType, attributeName, value));
                else
                    writer.write(String.format("\t%s %s %s = %s;\n", memberVisibility, attributeType, attributeName, value));

                writer.write(String.format("\n\tpublic %s get%s() {\n", attributeType, attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1)));
                writer.write(String.format("\treturn %s;\n", attributeName));
                writer.write("}\n");

                writer.write(String.format("\n\tpublic void set%s(%s %s) {\n", attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1), attributeType, attributeName));
                writer.write(String.format("\tthis.%s = %s;\n", attributeName, attributeName));
                writer.write("}\n");
            }
            if (member.getName().equals("constructeur")){
                if(member.getAttributeValue("type").equals("default")){
                    if(member.getChildren().size() > 0){
                        writer.write(String.format("\tpublic %s (){\n", member.getParentElement().getName()));
                        for (Element element : member.getChildren()) {
                            String value = element.getText();
                            Attribute typeAttr = element.getAttribute("type");
                            String attributeType = typeAttr != null ? typeAttr.getValue() : "String";
                            if (attributeType.equals("String"))
                                writer.write(String.format("\t\tthis.%s = \"%s\";\n", element.getAttributeValue("initialize"), element.getText()));
                            else
                                writer.write(String.format("\t\tthis.%s = %s;\n", element.getAttributeValue("initialize"), element.getText()));
                        }
                        writer.write("\t}\n");
                    }else{
                        writer.write(String.format("\tpublic %s (){\n}\n", member.getParentElement().getName()));
                    }
                }else if (member.getAttributeValue("type").equals("both")) {
                    writer.write(String.format("\tpublic %s (){\n}\n", member.getParentElement().getName()));
                    writer.write(String.format("\tpublic %s (", member.getParentElement().getName()));
                    boolean firstAttribute = true;
                    for (Element element : member.getChildren()) {
                        if (!firstAttribute) {
                            writer.write(", ");
                        }
                        Attribute typeAttr = element.getAttribute("type");
                        String attributeType = typeAttr != null ? typeAttr.getValue() : "String";
                        writer.write(String.format("%s %s", attributeType, element.getName()));
                        if (element.getText() != null && !element.getText().isEmpty()) {
                            writer.write(String.format(" = %s", element.getText()));
                        }
                        firstAttribute = false;
                    }
                    writer.write("){\n");
                    for (Element element : member.getChildren()) {
                        writer.write(String.format("\t\tthis.%s = %s;\n", element.getAttributeValue("initialize"), element.getName()));
                    }
                    writer.write("\t}\n");
                } else if (member.getAttributeValue("type").equals("withparameters")) {
                    writer.write(String.format("\tpublic %s (", member.getParentElement().getName()));
                    boolean firstAttribute = true;
                    for (Element element : member.getChildren()) {
                        if (!firstAttribute) {
                            writer.write(", ");
                        }
                        Attribute typeAttr = element.getAttribute("type");
                        String attributeType = typeAttr != null ? typeAttr.getValue() : "String";
                        writer.write(String.format("%s %s", attributeType, element.getName()));
                        if (element.getText() != null && !element.getText().isEmpty()) {
                            writer.write(String.format(" = %s", element.getText()));
                        }
                        firstAttribute = false;
                    }
                    writer.write("){\n");
                    for (Element element : member.getChildren()) {
                        writer.write(String.format("\t\tthis.%s = %s;\n", element.getAttributeValue("initialize"), element.getName()));
                    }
                    writer.write("\t}\n");
                }
            }
        }

    }
    @Override
    public void XmlToJava(File file) throws IOException, JDOMException {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(file);
        Map<String, List<Element>> nonPublicElements = new HashMap<>();
        Element rootElement = document.getRootElement();
        List<Element> childElementss = rootElement.getChildren();
        Element packageElement =childElementss.get(0);
        for (Element childElementt : childElementss){
            treatPackage(childElementt, "");
            StringBuilder othersb = new StringBuilder();
            List<Element> childElements = childElementt.getChildren();
            String packageName = childElementt.getName();
            for (Element childElement : childElements) {
                String visibility = childElement.getAttributeValue("visibility");
                String fileName = null;
                File outputFile = null;
                FileWriter writer = null;
                String className = null;
                String classOrInterface = null;
                if ("public".equals(visibility)) {
                    String type = childElement.getAttributeValue("type");
                    className = childElement.getName();
                    //fileName = className + (type.equals("Class") ? ".java" : "_interface.java");
                    fileName = className + ".java";
                    // Création du fichier Java pour l'élément
                    outputFile = new File("src/" + packageName + "/" + fileName);
                    writer = new FileWriter(outputFile);
                    classOrInterface = type.equals("Class") ? "class" : "interface";
                    String visibilityKeyword = "public";
                    writer.write("package " + packageName.toLowerCase() + ";\n");
                    List<Element> members = childElement.getChildren();
                    Element parentElement = (childElement.getChild("parent") != null)
                            ? childElement.getChild("parent")
                            : (childElement.getChild("implements") != null)
                            ? childElement.getChild("implements")
                            : null;
                    if (parentElement == null){
                        writer.write(String.format("%s %s %s {\n", visibilityKeyword, classOrInterface, className));
                        treatMembers(members, writer);
                    }
                    else{
                        String text = parentElement.getText().replaceAll(".*\\.", "");
                        char[] charArray = text.toCharArray();
                        int index = charArray.length - 1;
                        while (index >= 0 && !Character.isUpperCase(charArray[index])) {
                            index--;
                        }
                        String parentClassName = text.substring(index);
                        if(parentElement.getName().equals("parent"))
                           writer.write(String.format("%s class %s extends %s {\n", visibilityKeyword, className, parentClassName));
                        else if(parentElement.getName().equals("implements")){
                            writer.write(String.format("%s class %s implements %s {\n", visibilityKeyword, className, parentClassName));
                        }
                        treatMembers(members, writer);
                        //writer.write("}\n");
                        //writer.close();
                    }
                    writer.write("}\n");
                    writer.close();

                }else {
                    Element premier = childElements.get(0);
                    String parentClassName = null;
                    for (Element element : childElements) {
                        if ("public".equals(element.getAttributeValue("visibility")) && "Class".equals(element.getAttributeValue("type")) && element == premier) {
                            parentClassName = element.getName();

                        }
                    }
                    List<Element> elements = nonPublicElements.get(parentClassName);

                    System.out.println("parent class name: " + parentClassName);
                    if (elements == null) {
                        elements = new ArrayList<>();

                        nonPublicElements.put(parentClassName, elements);

                    }
                    if(!"package".equals(childElement.getAttributeValue("type")) )
                        elements.add(childElement);
                }

            }
            File otheroutputFile =null;
            for (String otherclassName : nonPublicElements.keySet()) {
                String otherfileName = otherclassName + ".java";
                otheroutputFile = new File("src/"+packageName+"/"+otherfileName);
                List<Element> nonPublicElementsForClass = nonPublicElements.get(otherclassName);
                for (Element nonPublicElement : nonPublicElementsForClass) {
                    System.out.println("Nnpublicele"+nonPublicElement.getName());
                    String nopublicvisibility = nonPublicElement.getAttributeValue("visibility");
                    String nopublictype = nonPublicElement.getAttributeValue("type");
                    String classs=nonPublicElement.getName();
                    System.out.println(classs);
                    FileWriter otherwriter = new FileWriter(otheroutputFile, true);
                    String nopubliclassOrInterface = nopublictype.equals("Class") ? "class" : "interface";
                    List<Element> members = nonPublicElement.getChildren();
                    if ("private".equals(nopublicvisibility)  || "protected".equals(nopublicvisibility)) {
                        BufferedReader reader = new BufferedReader(new FileReader(otheroutputFile));
                        List<String> lines = new ArrayList<String>();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            lines.add(line);
                        }
                        reader.close();
                        lines.remove(lines.size() - 1);
                        FileWriter sewriter = new FileWriter(otheroutputFile);
                        for (String ligne : lines) {
                            sewriter.write(ligne + System.lineSeparator());
                        }
                        sewriter.write(String.format("%s %s %s{\n", nopublicvisibility, nopubliclassOrInterface, classs));
                        treatMembers(members, sewriter);
                        sewriter.write("}\n}\n");
                        sewriter.close();
                    }
                    else if ("package".equals(nopublicvisibility)) {
                        System.out.println("classname " + otherclassName);
                        otherwriter.write(String.format("%s %s{\n", nopubliclassOrInterface, classs));
                        treatMembers(members, otherwriter);
                        otherwriter.write("}\n");
                    }
                    otherwriter.close();
                }

            }
        }

    }
}
