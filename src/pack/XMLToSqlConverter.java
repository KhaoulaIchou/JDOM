package pack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class XMLToSqlConverter {

    private static FileWriter fileWriter;

    public static void main(String[] args) {
        try {
            File inputFile = new File("src/pack/model.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();

            Element packagesElement = rootElement.getChild("Packages");
            Element linksElement = rootElement.getChild("links");
            fileWriter = new FileWriter(new File("src/ddl/sql.ddl"));
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            fileWriter.write("-- Generated by Etudiants de master SID_BD (yassir and khaoula)\n");
            fileWriter.write("-- Timestamp: " + timestamp + "\n\n");
            handlePackages(packagesElement, "");
            handleLinks(linksElement, rootElement);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handlePackages(Element packagesElement, String parentPackage) throws IOException {
        List<Element> packageElements = packagesElement.getChildren("Package");

        for (Element packageElement : packageElements) {
            String packageName = packageElement.getChildText("Name");

            Element childPackagesElement = packageElement.getChild("Packages");
            if (childPackagesElement != null) {
                handlePackages(childPackagesElement, packageName);
            }

            Element classesElement = packageElement.getChild("Classes");
            if (classesElement != null) {
                for (Element classElement : classesElement.getChildren("Class")) {
                    String className = classElement.getChildText("Name");
                    generateDDLStatements(packageName, className, classElement);
                }
            }
        }
    }

    private static void generateDDLStatements(String packageName, String className, Element classElement) throws IOException {
        fileWriter.write("-- SQL DDL statements for class: " + className + "\n");

        fileWriter.write("CREATE TABLE " + className + " (\n");
        fileWriter.write("\tid INTEGER PRIMARY KEY,\n");
        generateTableContents(classElement);
        fileWriter.write(");\n\n");
    }

    private static void generateTableContents(Element classElement) throws IOException {
        Element attributesElement = classElement.getChild("Attributes");
        if (attributesElement != null) {
            List<Element> attributeElements = attributesElement.getChildren("Attribute");
            for (Element attributeElement : attributeElements) {
                String attributeName = attributeElement.getChildText("Name");
                String attributeType = attributeElement.getChildText("Type");
                String sqlType = getSqlType(attributeType);
                fileWriter.write("\t" + attributeName + "\t" + sqlType + ",\n");
            }
        }
    }

    private static String getSqlType(String attributeType) {
        switch (attributeType) {
            case "int":
                return "INTEGER";
            case "String":
                return "VARCHAR2(255)";
            case "float":
                return "FLOAT";
            case "double":
                return "DOUBLE";
            case "boolean":
                return "BOOLEAN";
            case "char":
                return "CHAR";
            default:
                return "UNKNOWN";
        }
    }

    private static void handleLinks(Element linksElement, Element rootElement) throws IOException {
        if (linksElement != null) {
            for (Element linkElement : linksElement.getChildren("Link")) {
                String associationType = linkElement.getAttributeValue("AssociationType");
                String sourceLink = linkElement.getChildText("Source");
                String targetLink = linkElement.getChildText("Cible");
                String sourceClassName = sourceLink.substring(sourceLink.lastIndexOf(".") + 1);
                String targetClassName = targetLink.substring(targetLink.lastIndexOf(".") + 1);
                String sourceMultiplicity = linkElement.getChildText("MultiplicitySource");
                String targetMultiplicity = linkElement.getChildText("MultiplicityCible");

                switch (associationType) {
                    case "Aggregation":
                        createAggregationConstraint(sourceClassName, targetClassName);
                        break;

                    case "Composition":
                        createCompositionConstraint(sourceClassName, targetClassName);
                        break;

                    case "Simple":
                        createSimpleConstraint(sourceClassName, targetClassName, sourceMultiplicity, targetMultiplicity);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static void createAggregationConstraint(String sourceClass, String targetClass) throws IOException {
        String constraintName = "FK_" + sourceClass + "_" + targetClass;
        String foreignKeyStatement = "ALTER TABLE " + sourceClass + " ADD CONSTRAINT " + constraintName
                + " FOREIGN KEY (" + targetClass + "_id) REFERENCES " + targetClass + "(id);";

        fileWriter.write("-- Aggregation Constraint\n");
        fileWriter.write(foreignKeyStatement + "\n\n");
    }

    private static void createCompositionConstraint(String sourceClass, String targetClass) throws IOException {
        String constraintName = "FK_" + sourceClass + "_" + targetClass;
        String foreignKeyStatement = "ALTER TABLE " + sourceClass + " ADD CONSTRAINT " + constraintName
                + " FOREIGN KEY (" + targetClass + "_id) REFERENCES " + targetClass + "(id) ON DELETE CASCADE;";

        fileWriter.write("-- Composition Constraint\n");
        fileWriter.write(foreignKeyStatement + "\n\n");
    }

    private static void createSimpleConstraint(String sourceClass, String targetClass, String sourceMultiplicity, String targetMultiplicity) throws IOException {
        boolean isMultipleInTwoClass = generateMultiplicityConstraint(sourceClass,targetClass, sourceMultiplicity,targetMultiplicity);

        if(!isMultipleInTwoClass) {
            String constraintName = "FK_" + sourceClass + "_" + targetClass;
            String foreignKeyStatement = "ALTER TABLE " + sourceClass + " ADD CONSTRAINT " + constraintName
                    + " FOREIGN KEY (" + targetClass + "_id) REFERENCES " + targetClass + "(id);";

            fileWriter.write("-- Simple Constraint\n");
            fileWriter.write(foreignKeyStatement + "\n\n");
        }
    }

    private static boolean generateMultiplicityConstraint(String sourceClass, String targetClass, String multiplicitySource, String multiplicityTarget) throws IOException {
        if(multiplicitySource.equals("0..*") && multiplicityTarget.equals("0..*")) {
            String associationTableName = sourceClass + "_" + targetClass;
            String createTableStatement = "CREATE TABLE " + associationTableName + " ("
                    + sourceClass + "_id INTEGER,"
                    + targetClass + "_id INTEGER,"
                    + "PRIMARY KEY (" + sourceClass + "_id, " + targetClass + "_id),"
                    + "FOREIGN KEY (" + sourceClass + "_id) REFERENCES " + sourceClass + "(id),"
                    + "FOREIGN KEY (" + targetClass + "_id) REFERENCES " + targetClass + "(id));";

            fileWriter.write("-- Association Table\n");
            fileWriter.write(createTableStatement + "\n\n");
            return true;
        }
        return false;
    }

}
