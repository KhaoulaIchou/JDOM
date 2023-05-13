package pack;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import java.io.*;
import java.util.*;

public class JDOMTestt {
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
            Element linksElement = rootElement.getChild("links");
            handlePackages(packagesElement, "");
            handleLinks(linksElement, rootElement);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String capitalizeFirstLetter(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    private static void searchPackage(String sourceLink, String targetLink, Element rootElement) {
        String[] sourceParts = sourceLink.split("-");
        String[] targetParts = targetLink.split("-");
        String sourcePackage = sourceParts[0];
        String sourceClassId = sourceParts[1];
        String targetPackage = targetParts[0];
        String targetClassId = targetParts[1];
        Element packagesElement = rootElement.getChild("Package");
        if(packagesElement !=null){
            List<Element> packageElements = packagesElement.getChildren("Package");
            for (Element packageElement : packageElements) {
                if(packageElement.getChildText("Name").equals(sourcePackage) || packageElement.getChildText("Name").equals(targetPackage)){
                    if(packageElement.getChildText("Name").equals(sourcePackage)){
                        System.out.println("sourcePackage "+sourcePackage);
                        Element classesElement = packageElement.getChild("Classes");
                        if (classesElement != null) {
                            for (Element classElement : classesElement.getChildren("Class")) {
                                if(classElement.getAttributeValue("id").equals(sourceClassId.toString())){
                                    System.out.println("Classe Source "+classElement.getChildText("Name"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private static void handleLinks(Element linksElement, Element rootElement) throws IOException {
        if (linksElement != null) {
            for (Element linkElement : linksElement.getChildren("Link")) {
                String sourceLink = linkElement.getChildText("Source");
                System.out.println("Source link "+sourceLink);
                String targetLink = linkElement.getChildText("Cible");
                System.out.println("Target link "+targetLink);
                searchPackage(sourceLink, targetLink, rootElement);
            }
        }
    }
    private static void handlePackages(Element packagesElement, String parentPackage) throws IOException {
        List<Element> packageElements = packagesElement.getChildren("Package");
        for (Element packageElement : packageElements) {
            // Récupérer le nom du package
            String packageName = packageElement.getChildText("Name");

            // Créer un fichier Java pour le package
            String packagePath = (parentPackage.isEmpty() ? "" : parentPackage + ".") + packageName;
            File packageFile = new File("src/" + packagePath.replace(".", "/"));
            packageFile.mkdirs();

            // Gérer les sous-packages récursivement
            Element childPackagesElement = packageElement.getChild("Packages");
            if (childPackagesElement != null) {
                handlePackages(childPackagesElement, packagePath);
            }


            // Récupérer les classes du package

            Element classesElement = packageElement.getChild("Classes");
            if (classesElement != null) {
                Map<String, List<Element>> nonPublicElements = new HashMap<>();
                for (Element classElement : classesElement.getChildren("Class")) {
                    String parentName = null;
                    String implementsName = null;
                    String parentElementText = classElement.getChildText("Parent");
                    Map<String, String> implementsMap = new HashMap<>();
                    List<Element> implementselements = classElement.getChildren("Implements");
                    for (Element implementsousele : implementselements) {
                        List<Element> implementselementsList = implementsousele.getChildren("Implement");
                        for (Element implementsoussous : implementselementsList) {
                            String implementsText = implementsoussous.getText();
                            //System.out.println("Implements fils "+implementsText);
                            String[] implementsParts = implementsText.split("\\.");

                            // Obtention de la dernière partie commençant par une majuscule
                            String lastPart = null;
                            for (int i = implementsParts.length - 1; i >= 0; i--) {
                                String part = implementsParts[i];
                                if (Character.isUpperCase(part.charAt(0))) {
                                    lastPart = part;
                                    break;
                                }
                            }

                            if (lastPart != null) {
                                implementsMap.put("import "+implementsText+";\n", lastPart);
                            }
                        }
                    }

                    for (Map.Entry<String, String> entry : implementsMap.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        System.out.println("Clé : " + key + ", Valeur : " + value);
                    }

                    String classNameParent = classElement.getChildText("Name");
                    String classNameImplements = null;
                    if (parentElementText != null && !parentElementText.isEmpty()) {
                        int lastDotIndex = parentElementText.lastIndexOf(".");
                        if (lastDotIndex != -1) {
                            parentName = parentElementText.substring(lastDotIndex + 1);
                        }
                    }
                   if (!implementsMap.isEmpty()) {
                        classNameImplements = classElement.getChildText("Name");
                    }
                    // Récupérer le nom de la classe
                    String className = classElement.getChildText("Name");
                    String classVisibility = classElement.getChildText("visibility");

                    // Créer un fichier Java pour la classe
                    if (classVisibility.equals("public")) {
                        File classFile = new File(packageFile, className + ".java");
                        FileWriter fileWriter = new FileWriter(classFile);
                        fileWriter.write("package " + packagePath + ";\n");
                        if (parentName != null && className.equals(classNameParent) && implementsMap.isEmpty()) {
                            if(parentElementText != null)
                                fileWriter.write("import "+parentElementText+";\n");
                            fileWriter.write(classVisibility+" class " + className +" extends "+parentName+"{\n");
                        }else if(parentName != null && className.equals(classNameParent) && !implementsMap.isEmpty() && className.equals(classNameImplements)){
                            StringBuilder importBuilder = new StringBuilder();
                            if(parentElementText != null)
                                fileWriter.write("import "+parentElementText+";\n");
                            if(!implementsMap.isEmpty()){
                                for (String key : implementsMap.keySet()) {
                                    importBuilder.append(key);
                                }
                                importBuilder.append(classVisibility +" class "+className+" extends "+parentName+" implements ");
                                fileWriter.write(importBuilder.toString());
                                Iterator<String> iterator = implementsMap.values().iterator();
                                while (iterator.hasNext()) {
                                    String value = iterator.next();
                                    fileWriter.write(value.toString());
                                    System.out.println("value "+value.toString());
                                    if (iterator.hasNext()) {
                                        fileWriter.write(",");
                                    } else {
                                        fileWriter.write("");
                                    }
                                }
                                fileWriter.write(" {\n");
                            }
                        }else if(parentName == null && !implementsMap.isEmpty() && className.equals(classNameImplements)){
                            StringBuilder importBuilder = new StringBuilder();
                            for (String key : implementsMap.keySet()) {
                                importBuilder.append(key);
                            }
                            importBuilder.append(classVisibility+" class "+className+" implements ");
                            fileWriter.write(importBuilder.toString());
                            Iterator<String> iterator = implementsMap.values().iterator();
                            while (iterator.hasNext()) {
                                String value = iterator.next();
                                fileWriter.write(value.toString());
                                if (iterator.hasNext()) {
                                    fileWriter.write(",");
                                } else {
                                    fileWriter.write("");
                                }
                            }
                            fileWriter.write(" {\n");
                        }
                        else
                            fileWriter.write(classVisibility+" class " + className + "{\n");
                        generateClassContents(fileWriter, classElement);
                        fileWriter.write("}\n");
                        fileWriter.close();
                    }else if(classVisibility.equals("package")){
                        File classFile = new File(packageFile, className + ".java");
                        FileWriter fileWriter = new FileWriter(classFile);
                        fileWriter.write("package " + packagePath + ";\n");
                        if (parentName != null && className.equals(classNameParent) && implementsName == null) {
                            if(parentElementText != null)
                                fileWriter.write("import "+parentElementText+";\n");
                            fileWriter.write("class " + className +" extends "+parentName+"{\n");
                        }else if(parentName != null && className.equals(classNameParent) && !implementsMap.isEmpty() && className.equals(classNameImplements)){
                            if(parentElementText != null)
                                fileWriter.write("import "+parentElementText+";\n");
                            StringBuilder importBuilder = new StringBuilder();
                            if(!implementsMap.isEmpty()){
                                for (String key : implementsMap.keySet()) {
                                    importBuilder.append(key);
                                }
                                importBuilder.append("class "+className+" extends "+parentName+" implements ");
                                fileWriter.write(importBuilder.toString());
                                Iterator<String> iterator = implementsMap.values().iterator();
                                while (iterator.hasNext()) {
                                    String value = iterator.next();
                                    fileWriter.write(value.toString());
                                    if (iterator.hasNext()) {
                                        fileWriter.write(",");
                                    } else {
                                        fileWriter.write("");
                                    }
                                }
                                fileWriter.write(" {\n");
                            }
                        }else if(parentName == null && implementsMap != null && className.equals(classNameImplements)){
                            StringBuilder importBuilder = new StringBuilder();
                            for (String key : implementsMap.keySet()) {
                                importBuilder.append(key);
                            }
                            importBuilder.append("class "+className+" implements ");
                            fileWriter.write(importBuilder.toString());
                            Iterator<String> iterator = implementsMap.values().iterator();
                            while (iterator.hasNext()) {
                                String value = iterator.next();
                                fileWriter.write(value.toString());
                                if (iterator.hasNext()) {
                                    fileWriter.write(",");
                                } else {
                                    fileWriter.write("");
                                }
                            }
                            fileWriter.write(" {\n");
                        }
                        else
                            fileWriter.write("class " + className + "{\n");
                        generateClassContents(fileWriter, classElement);
                        fileWriter.write("}\n");
                        fileWriter.close();
                    }
                    else{
                        if(classVisibility.equals("protected") || classVisibility.equals("private")){
                            Element premier = classesElement.getChildren().get(0);
                            String parentClassName = null;
                            for (Element element : classesElement.getChildren()) {
                                if ("public".equals(element.getChildText("visibility")) && element == premier) {
                                    parentClassName = element.getChildText("Name");

                                }
                            }
                            List<Element> elements = nonPublicElements.get(parentClassName);

                            System.out.println("parent class name: " + parentClassName);
                            if (elements == null) {
                                elements = new ArrayList<>();

                                nonPublicElements.put(parentClassName, elements);

                            }
                            if("private".equals(classElement.getChildText("visibility")) || "protected".equals(classElement.getChildText("visibility"))){
                                elements.add(classElement);
                            }
                        }
                    }
                    Element parentElement = classElement.getChild("Parent");
                    //System.out.println("Fichier Java généré pour la classe : " + className);
                }
                File otheroutputFile =null;
                for (String otherclassName : nonPublicElements.keySet()) {
                    String otherfileName = otherclassName + ".java";
                    System.out.println("otherclassName  "+otherclassName);
                    otheroutputFile = new File("src/"+packagePath+"/"+otherfileName);
                    List<Element> nonPublicElementsForClass = nonPublicElements.get(otherclassName);
                    for (Element nonPublicElement : nonPublicElementsForClass) {
                        String parentName = null;
                        //String implementsName = null;
                        String parentElementText = nonPublicElement.getChildText("Parent");
                        //String implementsElementText = nonPublicElement.getChildText("Implements");
                        Map<String, String> implementsMap = new HashMap<>();
                        List<Element> implementselements = nonPublicElement.getChildren("Implements");
                        for (Element implementsousele : implementselements) {
                            List<Element> implementselementsList = implementsousele.getChildren("Implement");
                            for (Element implementsoussous : implementselementsList) {
                                String implementsText = implementsoussous.getText();
                                //System.out.println("Implements fils "+implementsText);
                                String[] implementsParts = implementsText.split("\\.");

                                // Obtention de la dernière partie commençant par une majuscule
                                String lastPart = null;
                                for (int i = implementsParts.length - 1; i >= 0; i--) {
                                    String part = implementsParts[i];
                                    if (Character.isUpperCase(part.charAt(0))) {
                                        lastPart = part;
                                        break;
                                    }
                                }

                                if (lastPart != null) {
                                    implementsMap.put("import "+implementsText+";\n", lastPart);
                                }
                            }
                        }
                        String classNameParent = nonPublicElement.getChildText("Name");
                        String classNameImplements = null;
                        if (parentElementText != null && !parentElementText.isEmpty()) {
                            int lastDotIndex = parentElementText.lastIndexOf(".");
                            if (lastDotIndex != -1) {
                                parentName = parentElementText.substring(lastDotIndex + 1);
                            }
                        }
                        if (!implementsMap.isEmpty()) {
                            classNameImplements = nonPublicElement.getChildText("Name");
                        }
                        String nopublicvisibility = nonPublicElement.getChildText("visibility");
                        String classs=nonPublicElement.getChildText("Name");
                        System.out.println(classs);
                        FileWriter otherwriter = new FileWriter(otheroutputFile, true);
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
                            if (parentName != null && classs.equals(classNameParent) && implementsMap.isEmpty()) {
                                if(parentElementText != null)
                                    sewriter.write("import " + parentElementText + ";\n");
                                sewriter.write(nopublicvisibility + " class " + classs + " extends " + parentName + "{\n");
                            }
                            else if(parentName != null && classs.equals(classNameParent) && !implementsMap.isEmpty() && classs.equals(classNameImplements)){
                                if(parentElementText != null)
                                    sewriter.write("import "+parentElementText+";\n");
                                StringBuilder importBuilder = new StringBuilder();
                                if(!implementsMap.isEmpty()){
                                    for (String key : implementsMap.keySet()) {
                                        importBuilder.append(key);
                                    }
                                    importBuilder.append("class "+classs+" extends "+parentName+" implements ");
                                    sewriter.write(importBuilder.toString());
                                    Iterator<String> iterator = implementsMap.values().iterator();
                                    while (iterator.hasNext()) {
                                        String value = iterator.next();
                                        sewriter.write(value.toString());
                                        if (iterator.hasNext()) {
                                            sewriter.write(",");
                                        } else {
                                            sewriter.write("");
                                        }
                                    }
                                    sewriter.write(" {\n");
                                }
                            }else if(parentName == null && implementsMap != null && classs.equals(classNameImplements)){
                                StringBuilder importBuilder = new StringBuilder();
                                for (String key : implementsMap.keySet()) {
                                    importBuilder.append(key);
                                }
                                importBuilder.append("class "+classs+" implements ");
                                sewriter.write(importBuilder.toString());
                                Iterator<String> iterator = implementsMap.values().iterator();
                                while (iterator.hasNext()) {
                                    String value = iterator.next();
                                    sewriter.write(value.toString());
                                    if (iterator.hasNext()) {
                                        sewriter.write(",");
                                    } else {
                                        sewriter.write("");
                                    }
                                }
                                sewriter.write(" {\n");
                            }
                            else
                                sewriter.write(String.format("%s class %s{\n", nopublicvisibility, classs));
                            generateClassContents(sewriter, nonPublicElement);
                            sewriter.write("}\n}\n");
                            sewriter.close();
                        }
                        otherwriter.close();
                    }

                }
            }
            Element interfacesElement = packageElement.getChild("Interfaces");
            if (interfacesElement != null) {
                for (Element interfaceElement : interfacesElement.getChildren("Interface")) {
                    // Récupérer le nom de l'interface
                    String interfaceName = interfaceElement.getChildText("Name");

                    // Créer un fichier Java pour l'interface
                    File interfaceFile = new File(packageFile, interfaceName + ".java");

                    // Écrire le contenu de l'interface dans le fichier
                    FileWriter fileWriter = new FileWriter(interfaceFile);
                    fileWriter.write("package " + packagePath + ";\n");
                    fileWriter.write("public interface " + interfaceName + " {\n");

                    // Générer les méthodes de l'interface
                    Element methodsElement = interfaceElement.getChild("Methods");
                    if (methodsElement != null) {
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

                            fileWriter.write(");\n");
                        }
                    }

                    fileWriter.write("}");
                    fileWriter.close();

                    System.out.println("Fichier Java généré pour l'interface : " + interfaceName);
                }
            }
            //Element linksElement = packageElement.getChild("Links");

            //System.out.println("Fichiers Java générés pour le package : " + packagePath);
            // /

        }
    }
    private static void generateClassContents(FileWriter fileWriter, Element classElement) throws IOException {
        // Récupérer les attributs de classe
        if (classElement.getChild("Attributes") != null) {
            Element attributesElement = classElement.getChild("Attributes");
            generateAttributes(fileWriter, attributesElement);
        }
        String className = classElement.getChildText("Name");
        // Générer les constructeurs
        generateConstructors(fileWriter, className);

        // Générer les méthodes de classe
        if (classElement.getChild("Methods") != null) {
            Element methodsElement = classElement.getChild("Methods");
            generateMethods(fileWriter, methodsElement);
        }
    }
    private static void generateAttributes(FileWriter fileWriter, Element attributesElement) throws IOException {
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
    private static void generateConstructors(FileWriter fileWriter, String className) throws IOException {
        fileWriter.write("\n");
        fileWriter.write("\t// Constructeur par défaut\n");
        fileWriter.write("\tpublic " + className + "() {\n");
        fileWriter.write("\t\t// Initialisez les attributs\n");
        fileWriter.write("\t}\n");
    }

    private static void generateMethods(FileWriter fileWriter, Element methodsElement) throws IOException {
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
}
