package compiler;

import execute.ExecuteCode;
import lexical.LexicalAnalyzer;
import lexical.Token;
import parsing.Code;
import parsing.Declaration;
import parsing.DeclarationTableParser;
import parsing.Parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    static String sourcePath = "/src/code/test.txt"; // 测试代码地址
    static String testPath = "/src/code/code.pl0code"; // 测试目标代码地址

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input pl/0 file");
        File directory = new File("");// 参数为空
        String courseFile = directory.getCanonicalPath();
//        String sourceCodePath = scanner.next();
        String sourceCodePath = courseFile + sourcePath;

//        System.out.println("\u4f18\u79c0");
        System.out.println("List object code ?(Y/N)");
        String s = scanner.next();
//        String s = "n";
        String sourceCode = SourceCodeFileReader.readFileContent(sourceCodePath, "\n");
        if (s.equals("Y") || s.equals("y")) {
            System.out.println("\n==========源代码==========");
            System.out.println(sourceCode);
        }

        System.out.println("List symbol table ? (Y/N)");
        s = scanner.next();
        ArrayList<Token> wordsToken = LexicalAnalyzer.getsym(sourceCode);
        for (int i = 0; i < wordsToken.size(); i++)
            wordsToken.get(i).setPosition(i);
        if (s.equals("Y") || s.equals("y")) {
            System.out.println("\n==========词法分析后的Token==========");
            for (int a = 0; a < wordsToken.size(); a++) {
                System.out.println(a + " " + wordsToken.get(a).toString());
            }
        }

        System.out.println("<程序>");
        ArrayList<Declaration> declarationList = DeclarationTableParser.parse(wordsToken, 0, 3, 1);

    }
}
