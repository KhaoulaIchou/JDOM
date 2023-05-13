package pack;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class fichier {

    public static void main(String[] args) throws Exception {
        // Chargement du fichier XML
        File inputFile = new File("C:\\Users\\dell\\IdeaProjects\\untitled\\src\\pack\\file.xml");
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(inputFile);
        Map<String, List<Element>> nonPublicElements = new HashMap<>();
        // Récupération de l'élément racine et du nom du package
        Element rootElement = document.getRootElement();
        String packageName = rootElement.getName();
        StringBuilder othersb = new StringBuilder();
        // Génération du code Java pour chaque élément fils de l'élément racine
        // Génération du code Java pour chaque élément fils de l'élément racine
        List<Element> childElements = rootElement.getChildren();
        for (Element childElement : childElements) {
            System.out.println("childElement "+childElement.getName());
            String visibility = childElement.getAttributeValue("visibility");
            String fileName = null;
            File outputFile = null;
            FileWriter writer = null;
            String className = null;
            String classOrInterface = null;
            if ("public".equals(visibility)) {
                String type = childElement.getAttributeValue("type");
                className = childElement.getName();
                fileName = className + (type.equals("Class") ? ".java" : "_interface.java");

                // Création du fichier Java pour l'élément
                outputFile = new File(fileName);
                writer = new FileWriter(outputFile);

                // Écriture du code Java pour l'élément
                classOrInterface = type.equals("Class") ? "class" : "interface";
                String visibilityKeyword = "public";
                writer.write("package " + packageName.toLowerCase() + ";\n");
                Element parentElement = childElement.getChild("parent");
                if (parentElement == null)
                    writer.write(String.format("%s %s %s {\n", visibilityKeyword, classOrInterface, className));
                else
                    writer.write(String.format("%s class %s ", visibilityKeyword, className));
                // Génération des attributs et des méthodes pour chaque élément fils
                List<Element> members = childElement.getChildren();
                for (Element member : members) {
                    //String memberName = member.getName();
                    String memberVisibility = member.getAttributeValue("visibility");
                    //String membertypeRetour = member.getAttributeValue("type");
                    String memberType = member.getAttributeValue("typemembre");
                    System.out.println(memberType + " " + memberVisibility);
                    if (member.getName().equals("parent")) {
                        String parentClass = member.getText();
                        writer.write(String.format("extends %s {\n",parentClass));
                        continue;
                    }
                    if (memberType.equals("method")) {
                        // Génération de la méthode si l'élément est une méthode
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
                        writer.write(");\n");
                    } else {
                        // Génération de l'attribut si l'élément est un attribut
                        String value = member.getText();
                        Attribute typeAttr = member.getAttribute("type");
                        String attributeType = typeAttr != null ? typeAttr.getValue() : "String";
                        String attributeName = member.getName();
                        if (attributeType == "String")
                            writer.write(String.format("\t%s %s %s = \"%s\";\n", memberVisibility, attributeType, attributeName, value));
                        else
                            writer.write(String.format("\t%s %s %s = %s;\n", memberVisibility, attributeType, attributeName, value));
                    }
                }

                writer.write("}\n");
                writer.close();
            } else {
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
            //writer = new FileWriter(outputFile, true);
            //writer.write(othersb.toString());
            //writer.close();
            if (childElement.getAttributeValue("type").equals("package")) {
                String packageNam = childElement.getName();
                String otherfileName = null;
                // Création du fichier Java pour l'élément
                File otheroutputFile = null;
                FileWriter otherwriter = null;
                //writer = new FileWriter(outputFile, false);
                //writer.write(String.format("package %s; \n",packageNam));
                System.out.println("package "+packageNam);
                List<Element> children = childElement.getChildren();
                for (Element child : children) {
                    String classFileName = child.getName();
                    System.out.println("classFileName  "+classFileName);
                    String classFileVisibility = child.getAttributeValue("visibility");
                    String classFileType = child.getAttributeValue("type");
                    otherfileName = classFileName + (classFileType.equals("Class") ? ".java" : "_interface.java");
                    otheroutputFile = new File(otherfileName);
                    otherwriter=new FileWriter(otheroutputFile);
                    otherwriter.write(String.format("package %s.%s; \n",packageName.toLowerCase(), packageNam.toLowerCase()));
                    Element parentPackage = child.getChild("parent");
                    Element implementPackage = child.getChild("implements");
                    List<Element> members = child.getChildren();
                    if (parentPackage != null) {
                        otherwriter.write(String.format("import %s.%s; \n",packageName.toLowerCase(),parentPackage.getText()));
                        otherwriter.write(String.format("%s class %s extends %s {\n",classFileVisibility,classFileName,parentPackage.getText()));
                    }else if(implementPackage != null) {
                        otherwriter.write(String.format("import %s.%s; \n",packageName.toLowerCase(),implementPackage.getText()));
                        otherwriter.write(String.format("%s class %s implements %s {\n",classFileVisibility,classFileName,implementPackage.getText()));
                    }else{
                        otherwriter.write(String.format("%s %s %s {\n",classFileVisibility,classFileType,classFileName));
                    }
                    otherwriter.write("}\n");
                    otherwriter.close();
                    //System.out.println(othersb.toString());

                }
            }
        }
        File outputFile =null;
        for (String className : nonPublicElements.keySet()) {
            // Ouvrir le fichier Java correspondant à la classe/interface parente
            String fileName = className + ".java";
            outputFile = new File("C:\\Users\\dell\\IdeaProjects\\untitled\\"+fileName);
            // Ajouter le code pour chaque élément non publique
            List<Element> nonPublicElementsForClass = nonPublicElements.get(className);
            for (Element nonPublicElement : nonPublicElementsForClass) {
                System.out.println("Nnpublicele"+nonPublicElement.getName());
                String visibility = nonPublicElement.getAttributeValue("visibility");
                String type = nonPublicElement.getAttributeValue("type");
                String classs=nonPublicElement.getName();
                System.out.println(classs);
                String classOrInterface = type.equals("Class") ? "class" : "interface";
                if ("private".equals(visibility)  || "protected".equals(visibility)) {
                    othersb.append(String.format("%s %s %s{\n", visibility, classOrInterface, classs));
                }
                else if ("package".equals(visibility)) {
                    System.out.println("classname " + className);
                    othersb.append(String.format("%s %s{\n", classOrInterface, classs));
                }
                List<Element> members = nonPublicElement.getChildren();
                for (Element member : members) {
                    //String memberName = member.getName();
                    String memberVisibility = member.getAttributeValue("visibility");
                    //String membertypeRetour = member.getAttributeValue("type");
                    String memberType = member.getAttributeValue("typemembre");
                    System.out.println(memberType + " " + memberVisibility);

                    if (memberType.equals("method")) {
                        // Génération de la méthode si l'élément est une méthode
                        String returnType = member.getAttributeValue("typeRetour") != null ? member.getAttributeValue("typeRetour") : "void";
                        String methodName = member.getName();

                        othersb.append(String.format("\t%s %s %s(", memberVisibility, returnType, methodName));
                        List<Element> parameters = member.getChildren("parameter");
                        for (int i = 0; i < parameters.size(); i++) {
                            Element parameter = parameters.get(i);
                            String parameterType = parameter.getAttributeValue("type");
                            String parameterName = parameter.getAttributeValue("nom");
                            othersb.append(String.format("%s %s", parameterType, parameterName));
                            if (i < parameters.size() - 1) {
                                othersb.append(", ");
                            }
                        }
                        othersb.append(");\n");
                    } else {
                        // Génération de l'attribut si l'élément est un attribut
                        String value = member.getText();
                        Attribute typeAttr = member.getAttribute("type");
                        String attributeType = typeAttr != null ? typeAttr.getValue() : "String";
                        String attributeName = member.getName();
                        if (attributeType == "String")
                            othersb.append(String.format("\t%s %s %s = \"%s\";\n", memberVisibility, attributeType, attributeName, value));
                        else
                            othersb.append(String.format("\t%s %s %s = %s;\n", memberVisibility, attributeType, attributeName, value));
                    }
                }

                othersb.append("}\n");
            }
            FileWriter writer = new FileWriter(outputFile, true);
            // écriture du contenu dans le fichier
            writer.write(othersb.toString());
// fermeture du fichier
            writer.close();
        }


    }
}

