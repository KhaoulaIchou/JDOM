package pack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class JDOMTest {

    public static void main(String[] args) throws Exception {
        // Chargement du fichier XML
        File inputFile = new File("C:\\Users\\dell\\IdeaProjects\\untitled\\src\\pack\\file.xml");
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(inputFile);

        // Récupération de l'élément racine et du nom du package
        Element rootElement = document.getRootElement();
        String packageName = rootElement.getName();

        // Génération du code Java pour chaque élément fils de l'élément racine
        List<Element> childElements = rootElement.getChildren();
        StringBuilder othersb = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        String fileName=null;
        for (Element childElement : childElements) {
            String type = childElement.getAttributeValue("type");
            String visibility = childElement.getAttributeValue("visibility");
            String className = childElement.getName();
            String[] valeurs = new String[childElements.size()];
            System.out.println("classname "+className);
            // Création de la classe ou de l'interface en fonction de l'attribut "type"
            String classOrInterface = type.equals("Class") ? "class" : "interface";
            sb.append("package " + packageName.toLowerCase() + "\n");
            if(visibility == "public"){
                //fileName = String.format("%s.java", className);
                //System.out.println("filename "+fileName);
                sb.append(String.format("%s %s %s {\n", visibility, classOrInterface, className));

                // Génération des attributs et des méthodes pour chaque élément fils
                List<Element> members = childElement.getChildren();
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

                        sb.append(String.format("\t%s %s %s(", memberVisibility, returnType, methodName));
                        List<Element> parameters = member.getChildren("parameter");
                        for (int i = 0; i < parameters.size(); i++) {
                            Element parameter = parameters.get(i);
                            String parameterType = parameter.getAttributeValue("type");
                            String parameterName = parameter.getAttributeValue("nom");
                            sb.append(String.format("%s %s", parameterType, parameterName));
                            if (i < parameters.size() - 1) {
                                sb.append(", ");
                            }
                        }
                        sb.append(");\n");
                    } else {
                        // Génération de l'attribut si l'élément est un attribut
                        String value = member.getText();
                        Attribute typeAttr = member.getAttribute("type");
                        String attributeType = typeAttr != null ? typeAttr.getValue() : "String";
                        String attributeName = member.getName();
                        if (attributeType == "String")
                            sb.append(String.format("\t%s %s %s = \"%s\";\n", memberVisibility, attributeType, attributeName, value));
                        else
                            sb.append(String.format("\t%s %s %s = %s;\n", memberVisibility, attributeType, attributeName, value));
                    }
                }

                sb.append("}");
            }
            else{
                if (visibility == "package")
                    othersb.append(String.format("%s %s {\n", classOrInterface, className));
                else
                    othersb.append(String.format("%s %s %s {\n", visibility, classOrInterface, className));
                // Génération des attributs et des méthodes pour chaque élément fils
                List<Element> members = childElement.getChildren();
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



            /*if (visibility.equals("public")) {
                fileName = String.format("%s.java", className);
                outputFile = new File(fileName);
                writer = new FileWriter(outputFile);
                writer.write(sb.toString());
                //publicWriter.close();
            } else {
                writer = new FileWriter(outputFile, true);
                writer.write(sb.toString());
                //privateWriter.close();
            }

                /*if (visibility == "public") {
                    fileName = String.format("%s.java", className);
                    outputFile = new File(fileName);
                    writer = new FileWriter(outputFile);
                    writer.write(sb.toString());
                    writer.close();
                } /*else if (visibility != "public"){
                    writer.write(sb.toString());
                    writer.close();
                }*/
            if (othersb.length() != 0) {
                sb.append(othersb);
                //System.out.println(sb.toString());
            }else{
                //System.out.println(sb.toString());
            }
        }
        System.out.println(sb);
        /*System.out.println(fileName);
        File outputFile = new File(fileName);
        FileWriter writer = new FileWriter(outputFile);
        writer.write(sb.toString());
        writer.write("\n" + othersb.toString());
        writer.close();*/

       /*if (visibility.equals("public")) {
            writer = new FileWriter(outputFile, true); // ouvrir en mode "append" pour ajouter le code à la fin du fichier existant
            writer.write("\n" + othersb.toString()); // ajouter le code de l'élément privé à la fin du fichier public
            writer.close();
        }*/
    }

}
