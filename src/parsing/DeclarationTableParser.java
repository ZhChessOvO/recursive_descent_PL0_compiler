package parsing;

import lexical.Token;
import error.PL0Error;

import java.util.ArrayList;

/**
 * 生成声明变量表的类
 * 声明变量表就是作业中的TABLE
 */
public class DeclarationTableParser {
    private static int tokenListIndex = 0;  // 扫描token表用的指针
    private static int initAdr = 3;  // 初始地址，即为作业中的DX
    private static final ArrayList<Declaration> declarationList = new ArrayList<Declaration>();  // 生成的declaration list

    /**
     * 生成declaration list的方法
     *
     * @param tokenList token列表
     * @param level     初始深度
     * @param adr       初始地址
     * @return declaration list
     */
    public static ArrayList<Declaration> parse(ArrayList<Token> tokenList, int level, int adr, int gradation) {
        // 初始深度，即为作业中的BLOCK
        initAdr = adr;
        while (tokenListIndex < tokenList.size()) {
            switch (tokenList.get(tokenListIndex).getSym()) {
                case "CONSTSYM":
                    tree(gradation, "<常量说明部分>");
                    constantDefinitionParser(tokenList, gradation);
                    break;
                case "VARSYM":
                    tree(gradation, "<变量说明部分>");
                    variableDefinitionParser(tokenList, level, initAdr, gradation);
                    break;
                case "PROCEDURESYM":
                    tree(gradation, "<过程说明部分>");
                    procedureDefinitionParser(tokenList, level, gradation);
                    break;
            }
            tokenListIndex++;
        }
        Parser.init(tokenList, declarationList, 2);
        return declarationList;
    }

    public static void tree(int gradation, String s) {
        for (int i = 0; i < gradation; i++) {
            System.out.print("\t");
        }
        System.out.println(s);
    }

    private static void identifierNumberTree(Token token, int gradation) {
        if (token.getNum().equals("")) {
            gradation++;
            int gradationPlus = gradation + 1;
            String s = token.getId();
            char[] id = s.toCharArray();
            for (char c : id) {
                if (Character.isAlphabetic(c)) {
                    tree(gradation, "<字母>");
                    tree(gradationPlus, String.valueOf(c));
                } else if (Character.isDigit(c)) {
                    tree(gradation, "<无符号整数>");
                    tree(gradationPlus, String.valueOf(c));
                }
            }
        } else {
            int gradationPlus = gradation + 1;
            String s = token.getNum();
            char[] id = s.toCharArray();
            for (char c : id) {
                tree(gradation, "<无符号整数>");
                tree(gradationPlus, String.valueOf(c));
            }
        }
    }

    /**
     * 对const进行语法翻译
     *
     * @param tokenList token列表
     */
    private static void constantDefinitionParser(ArrayList<Token> tokenList, int gradation) {
        tree(++gradation, "CONST");
        while (tokenListIndex < tokenList.size() - 1) {
            tokenListIndex++;
            if (tokenList.get(tokenListIndex).getSym().equals("IDENT")) {
                // 新建一个常量声明
                String identName = tokenList.get(tokenListIndex).getId();
                int position = tokenList.get(tokenListIndex).getPosition();
                Declaration constDeclaration = new Declaration(identName, "CONSTANT", "", "",
                        "", -1, -1, position);
                tree(gradation, "<常量定义>");
                tree(++gradation, "<标识符>");
                identifierNumberTree(tokenList.get(tokenListIndex), gradation);
                // 检测是否对常量进行了赋值
                checkAndAddDeclaration(tokenList, constDeclaration, gradation);

                // 若读取到分号，则结束，若读取到逗号，则什么都不做，若读取到其他，则抛异常
                if (tokenList.get(tokenListIndex).getSym().equals("SYM_;")) {
                    tree(--gradation, ";");
                    break;
                } else if (!tokenList.get(tokenListIndex).getSym().equals("SYM_,")) {
                    PL0Error.log(2);
                    break;
                } else {
                    tree(--gradation, ",");
                }
            } else {
                PL0Error.log(1);
                break;
            }
        }
    }

    /**
     * 对var进行语法翻译
     *
     * @param tokenList token列表
     */
    private static void variableDefinitionParser(ArrayList<Token> tokenList, int level, int adr, int gradation) {
        tree(++gradation, "VAR");
        while (tokenListIndex < tokenList.size() - 1) {
            tokenListIndex++;
            if (tokenList.get(tokenListIndex).getSym().equals("IDENT")) {
                // 新建一个变量声明
                String identName = tokenList.get(tokenListIndex).getId();
                int position = tokenList.get(tokenListIndex).getPosition();
                Declaration varDeclaration = new Declaration(identName, "VARIABLE", "", level + "",
                        (adr++) + "", -1, -1, position);
                tree(gradation, "<标识符>");
                identifierNumberTree(tokenList.get(tokenListIndex), gradation);

                // 检测是否对变量进行了赋值
                checkAndAddDeclaration(tokenList, varDeclaration, gradation);

                // 若读取到分号，则结束，若读取到逗号，则什么都不做，若读取到其他，则抛异常
                if (tokenList.get(tokenListIndex).getSym().equals("SYM_;")) {
                    tree(gradation, ";");
                    break;
                } else if (!tokenList.get(tokenListIndex).getSym().equals("SYM_,")) {
                    PL0Error.log(4);
                    break;
                } else {
                    tree(gradation, ",");
                }
            } else {
                PL0Error.log(3);
                break;
            }
        }
    }

    /**
     * 对procedure进行语法翻译
     *
     * @param tokenList token列表
     */
    private static void procedureDefinitionParser(ArrayList<Token> tokenList, int level, int gradation) {
        if (tokenListIndex < tokenList.size() - 1) {
            tokenListIndex++;
            if (tokenList.get(tokenListIndex).getSym().equals("IDENT")) {
                String identName = tokenList.get(tokenListIndex).getId();
                int position = tokenList.get(tokenListIndex).getPosition();
                Declaration procedureDeclaration = new Declaration(identName, "PROCEDURE", "",
                        level + "", "", tokenListIndex - 1, -1, position);
                declarationList.add(procedureDeclaration);
                gradation++;
                tree(gradation++, "<过程首部>");
                tree(gradation, "PROCEDURE");
                tree(gradation, "<标识符>");
                identifierNumberTree(tokenList.get(tokenListIndex), gradation);
                tokenListIndex++;

                tree(--gradation, "<分程序>");
                label:
                while (tokenListIndex < tokenList.size() - 1) {
                    // 如果在函数中还有var或者procedure，则递归继续执行
                    switch (tokenList.get(tokenListIndex).getSym()) {
                        case "CONSTSYM":
                            tree(gradation, "<常量说明部分>");
                            constantDefinitionParser(tokenList, gradation);
                            break;
                        case "VARSYM":
                            tree(gradation, "<变量说明部分>");
                            variableDefinitionParser(tokenList, level + 1, initAdr, gradation);
                            break;
                        case "PROCEDURESYM":
                            tree(gradation, "<过程说明部分>");
                            if (level < 3) {
                                procedureDefinitionParser(tokenList, level + 1, gradation);
                            } else {
                                PL0Error.log(6);
                            }
                            break;
                        case "ENDSYM":
                            procedureDeclaration.setEnd(tokenListIndex);
                            break label;
                    }
                    tokenListIndex++;
                }
                procedureDefinitionParser(tokenList, level, gradation);
            } else {
//                PL0Error.log(7);
            }
        }
    }

    /**
     * 检查是否对某个变量/常量进行了赋值，即判断x := 20;
     *
     * @param tokenList   token列表
     * @param declaration 声明
     */
    private static void checkAndAddDeclaration(ArrayList<Token> tokenList, Declaration declaration, int gradation) {
        tokenListIndex++;
        if (tokenList.get(tokenListIndex).getSym().equals("SYM_=") && tokenList.get(tokenListIndex + 1).getSym().equals("NUMBER")) {
            declaration.setVal(tokenList.get(tokenListIndex + 1).getNum());
            declarationList.add(declaration);
            tree(gradation, "=");
            identifierNumberTree(tokenList.get(++tokenListIndex), gradation);
            tokenListIndex++;
        } else {
            declarationList.add(declaration);
        }
    }
}
