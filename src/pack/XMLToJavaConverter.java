package pack;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import java.io.*;
import java.util.List;

public class XMLToJavaConverter {
    public static void main(String[] args) {
        try {
            // Charger le document XML à partir d'un fichier
            File inputFile = new File("src/pack/model.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            // Obtenir l'élément racine du document
            Element rootElement = document.getRootElement();

            // Récupérer les packages
            Element packagesElement = rootElement.getChild("Packages");
            for (Element packageElement : packagesElement.getChildren("Package")) {
                // Récupérer le nom du package
                String packageName = packageElement.getChildText("Name");

                // Créer un fichier Java pour le package
                String packagePath = packageName.replace(".", "/");
                File packageFile = new File("src/" + packagePath);
                packageFile.mkdirs();

                // Récupérer les classes du package
                if(packageElement.getChild("Classes") != null){
                    Element classesElement = packageElement.getChild("Classes");
                    for (Element classElement : classesElement.getChildren("Class")) {
                        // Récupérer le nom de la classe
                        String className = classElement.getChildText("Name");

                        // Créer un fichier Java pour la classe
                        File classFile = new File(packageFile, className + ".java");

                        // Écrire le contenu de la classe dans le fichier
                        FileWriter fileWriter = new FileWriter(classFile);
                        fileWriter.write("package "+packageName+";\n");
                        fileWriter.write("public class " + className + " {\n");

                        // Générer les attributs de classe
                        if(classElement.getChild("Attributes") !=null){
                            Element attributesElement = classElement.getChild("Attributes");
                            for (Element attributeElement : attributesElement.getChildren("Attribute")) {
                                String attributeName = attributeElement.getChildText("Name");
                                String attributeType = attributeElement.getChildText("Type");
                                String attributeVisibility = attributeElement.getAttributeValue("visibility");

                                // Générer la déclaration de l'attribut
                                fileWriter.write("\t" + attributeVisibility + " " + attributeType + " " + attributeName + ";\n");
                                fileWriter.write("\n");
                                fileWriter.write("\t" + attributeVisibility + " " + attributeType + " get" + capitalizeFirstLetter(attributeName) + "() {\n");
                                fileWriter.write("\t\treturn " + attributeName + ";\n");
                                fileWriter.write("\t}\n");

                                // Générer le setter
                                fileWriter.write("\n");
                                fileWriter.write("\t" + attributeVisibility + " void set" + capitalizeFirstLetter(attributeName) + "(" + attributeType + " " + attributeName + ") {\n");
                                fileWriter.write("\t\tthis." + attributeName + " = " + attributeName + ";\n");
                                fileWriter.write("\t}\n");
                            }
                        }
                        // Générer les constructeurs
                        fileWriter.write("\n");
                        fileWriter.write("\t// Constructeur par défaut\n");
                        fileWriter.write("\tpublic " + className + "() {\n");
                        fileWriter.write("\t\t// Initialisez les attributs\n");
                        fileWriter.write("\t}\n");

                        if(classElement.getChild("Methods") !=null){
                            // Générer les méthodes de classe
                            Element methodsElement = classElement.getChild("Methods");
                            for (Element methodElement : methodsElement.getChildren("method")) {
                                String methodName = methodElement.getChildText("Name");
                                String methodType = methodElement.getChildText("Type");
                                String methodVisibility = methodElement.getAttributeValue("visibility");

                                // Générer la signature de la méthode
                                fileWriter.write("\n");
                                fileWriter.write("\t" + methodVisibility + " " + methodType + " " + methodName + "(");

                                // Générer les paramètres de la méthode
                                Element parametersElement = methodElement.getChild("Parameters");
                                if (parametersElement != null) {
                                    List<Element> parameterElements = parametersElement.getChildren("Parameter");
                                    for (int i = 0; i < parameterElements.size(); i++) {
                                        Element parameterElement = parameterElements.get(i);
                                        String parameterName = parameterElement.getChildText("Name");
                                        String parameterType = parameterElement.getChildText("Type");
                                        fileWriter.write(parameterType + " " + parameterName);
                                        if (i < parameterElements.size() - 1) {
                                            fileWriter.write(", ");
                                        }
                                    }
                                }

                                fileWriter.write(") {\n");
                                fileWriter.write("\t\t// Ajoutez le code de la méthode ici\n");
                                if(!methodType.equals("void")){
                                    Object valeurParDefaut = null;

                                    if (methodType.equals("int") || methodType.equals("float")) {
                                        valeurParDefaut = 0;
                                    } else if (methodType.equals("double")) {
                                        valeurParDefaut = 0.0;
                                    } else if (methodType.equals("boolean")) {
                                        valeurParDefaut = false;
                                    } else if (methodType.equals("String")) {
                                        valeurParDefaut = null;
                                    } else {
                                        valeurParDefaut = null;
                                    }
                                    fileWriter.write("\treturn "+valeurParDefaut+";\n");
                                }

                                fileWriter.write("\t}\n");
                            }
                        }

                        fileWriter.write("}");
                        fileWriter.close();

                        System.out.println("Fichier Java généré pour la classe : " + className);
                    }
                }
                System.out.println("Fichiers Java générés pour le package : " + packageName);
            }

            System.out.println("Conversion XML vers Java terminée avec succès.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String capitalizeFirstLetter(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

