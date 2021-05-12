package parsing;

import error.PL0Error;
import lexical.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * 语句分析类，生成目标代码
 */
public class Parser {
    private static int tokenListIndex = 0;  // 扫描token表用的指针
    private static final ArrayList<Code> code = new ArrayList<Code>();  // 生成的output list，即为作业中的code数组

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

    public static void init(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        tree(gradation, "<语句>");
        for (Declaration declaration : declarationList) {
            if (declaration.getKind().equals("PROCEDURE")) {
                parse(tokenList, declarationList, declaration.getStart(), declaration.getEnd(), gradation);
            }
        }
        parse(tokenList, declarationList, declarationList.get(declarationList.size() - 1).getEnd(), tokenList.size(), gradation);
    }

    private static boolean isFunction(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList) {
        String id = tokenList.get(tokenListIndex).getId();
        for (Declaration declaration : declarationList) {
            if (id.equals(declaration.getName()) && declaration.getKind().equals("PROCEDURE")) {
                return true;
            }
        }
        return false;
    }

    public static void readParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        tree(++gradation, "READ");
        Token token = getNext(tokenList);
        assert token != null;
        if (token.getSym().equals("SYM_(")) {
            tree(gradation, "(");
            tokenListIndex++;
            identifierNumberTree(tokenList.get(tokenListIndex), gradation);
            token = tokenList.get(++tokenListIndex);
            while (true) {
                assert token != null;
                if (!token.getSym().equals("SYM_,")) break;
                tree(gradation, ",");
                identifierNumberTree(tokenList.get(++tokenListIndex), gradation);
                token = tokenList.get(++tokenListIndex);
            }
            tree(gradation, ")");
        } else {
            PL0Error.log(17);
        }
    }

    public static void writeParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        tree(++gradation, "WRITE");
        Token token = getNext(tokenList);
        assert token != null;
        if (token.getSym().equals("SYM_(")) {
            tree(gradation, "(");
            expressionParser(tokenList, declarationList, gradation);
            token = tokenList.get(tokenListIndex);
            while (true) {
                assert token != null;
                if (!token.getSym().equals("SYM_,")) break;
                tree(gradation, ",");
                expressionParser(tokenList, declarationList, gradation);
                token = tokenList.get(tokenListIndex);
            }
            tree(gradation, ")");
        } else {
            PL0Error.log(18);
        }
    }

    public static boolean isDefined(Token token, ArrayList<Declaration> declarationList) {
        for (Declaration declaration : declarationList) {
            if (token.getId().equals(declaration.getName()) && token.getPosition() >= declaration.getPosition())
                return true;
        }
        return false;
    }

    /**
     * 对语句进行翻译，生成code list
     * read和write暂时不进行处理（因为不知道怎么处理，demo中也并没有给出io相关的指令）
     *
     * @param tokenList token列表
     */
    private static void parse(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int startIndex,
                              int endIndex, int gradation) {
        gradation++;
        while (tokenListIndex < endIndex) {
            if (isFunction(tokenList, declarationList)) {
                tokenListIndex++;
                continue;
            }
            int former = tokenListIndex - 1;
            Token token = tokenList.get(tokenListIndex);
            switch (token.getSym()) {
                case "IDENT":
                    if (isDefined(token, declarationList)) {
                        int index = tokenListIndex + 1;
                        if (!tokenList.get(index).getSym().equals("SYM_:="))
                            break;
                        if (former >= 0 && tokenList.get(former).getSym().equals("SYM_;")) {
                            tree(gradation, ";");
                        }
                        tree(gradation, "<赋值语句>");
                        identParser(tokenList, declarationList, gradation);
                    } else {
                        PL0Error.log(0);
                    }
                    break;
                case "BEGINSYM":
                    tree(gradation, "<复合语句>");
                    beginParser(tokenList, declarationList, startIndex, gradation);
                    break;
                case "IFSYM":
                    if (former >= 0 && tokenList.get(former).getSym().equals("SYM_;")) {
                        tree(gradation, ";");
                    }
                    tree(gradation, "<条件语句>");
                    ifParser(tokenList, declarationList, startIndex, endIndex, gradation);
                    break;
                case "CALLSYM":
                    if (former >= 0 && tokenList.get(former).getSym().equals("SYM_;")) {
                        tree(gradation, ";");
                    }
                    tree(gradation, "<过程调用语句>");
                    callParser(tokenList, declarationList, gradation);
                    break;
                case "WHILESYM":
                    if (former >= 0 && tokenList.get(former).getSym().equals("SYM_;")) {
                        tree(gradation, ";");
                    }
                    tree(gradation, "<当型循环语句>");
                    whileParser(tokenList, declarationList, startIndex, endIndex, gradation);
                    break;
                case "READSYM":
                    if (former >= 0 && tokenList.get(former).getSym().equals("SYM_;")) {
                        tree(gradation, ";");
                    }
                    tree(gradation, "<读语句>");
                    readParser(tokenList, declarationList, gradation);
                    break;
                case "WRITESYM":
                    if (former >= 0 && tokenList.get(former).getSym().equals("SYM_;")) {
                        tree(gradation, ";");
                    }
                    tree(gradation, "<写语句>");
                    writeParser(tokenList, declarationList, gradation);
                    break;
            }
            tokenListIndex++;
        }
    }

    /**
     * 翻译ident，例如 x := x + y 或 x := 20
     *
     * @param tokenList       token列表
     * @param declarationList 声明table
     */
    private static void identParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        gradation++;
        tree(gradation, "<标识符>");
        Token left = tokenList.get(tokenListIndex);
        identifierNumberTree(left, gradation);
        int leftIndex = getItemIndexInDeclarationList(left, declarationList);
        if (leftIndex != -1) {
            Token equals = getNext(tokenList);
            if (equals != null && equals.getSym().equals("SYM_:=")) {
                tree(gradation, ":=");
                expressionParser(tokenList, declarationList, gradation);
            } else if (equals != null && !(equals.getSym().equals("SYM_,") || equals.getSym().equals("SYM_;"))) {
                PL0Error.log(8);
            }
        } else {
            PL0Error.log(9);
        }
    }

    private static void factorParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        gradation++;
        tree(gradation, "<因子>");
        Token token = tokenList.get(tokenListIndex);
        switch (token.getSym()) {
            case "IDENT":
                tree(++gradation, "<标识符>");
                identifierNumberTree(tokenList.get(tokenListIndex), gradation);
                break;
            case "NUMBER":
                gradation++;
                identifierNumberTree(tokenList.get(tokenListIndex), gradation);
                break;
            case "SYM_(":                                                       //要测试
                tree(gradation, "(");
                expressionParser(tokenList, declarationList, gradation);
                tree(gradation, ")");
                break;
            default:
                PL0Error.log(8);
                break;
        }
        int next = tokenListIndex + 1;
        token = tokenList.get(next);
        String op = token.getSym();
        if (op.equals("SYM_*") || op.equals("SYM_/")) {
            tree(gradation, op.equals("SYM_*") ? "*" : "/");
            tokenListIndex += 2;
            factorParser(tokenList, declarationList, gradation);
        }
    }

    private static void termParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        tree(gradation, "<项>");
        factorParser(tokenList, declarationList, gradation);
        Token token = getNext(tokenList);
        if (!token.getSym().equals("SYM_;")) {
            int next = tokenListIndex + 1;
            Token token1 = tokenList.get(next);
            if ((token.getSym().equals("SYM_+") || token.getSym().equals("SYM_-")) &&
                    (token1.getSym().equals("IDENT") || token1.getSym().equals("NUMBER") || token1.getSym().equals("SYM_("))) {
                tree(gradation, "<加法运算符>");
                int i = gradation + 1;
                tree(i, tokenList.get(tokenListIndex).getSym().equals("SYM_+") ? "+" : "-");
                tree(gradation, "<项>");
                tokenListIndex++;
                factorParser(tokenList, declarationList, gradation);
            }
        }
    }

    /**
     * 处理赋值时 := 的右半部分，即处理表达式
     * TODO：可简化代码
     *
     * @param tokenList       token列表
     * @param declarationList 变量声明表
     *                        //* @param left            := 的左半部分
     */
    private static void expressionParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        tree(gradation, "<表达式>");
        gradation++;
        Token first = getNext(tokenList);

        assert first != null;
        if (first.getSym().equals("SYM_+") || first.getSym().equals("SYM_-")) {
            tree(gradation, first.getSym().equals("SYM_+") ? "+" : "-");
            first = getNext(tokenList);
        }
        assert first != null;
        if (first.getSym().equals("IDENT") || first.getSym().equals("NUMBER") || first.getSym().equals("SYM_(")) {
            termParser(tokenList, declarationList, gradation);
        }
    }

    /**
     * 翻译call
     *
     * @param tokenList       token列表
     * @param declarationList 声明列表
     */
    private static void callParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int gradation) {
        gradation++;
        tree(gradation, "CALL");
        Token ident = getNext(tokenList);
        if (ident != null && ident.getSym().equals("IDENT")) {
            Token end = getNext(tokenList);
            if (end != null && end.getSym().equals("SYM_;")) {
                tree(gradation, "<标识符>");
                identifierNumberTree(ident, gradation);
            } else {
                PL0Error.log(16);
            }
        } else {
            PL0Error.log(15);
        }
    }

    private static void beginParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int startIndex, int gradation) {

        tree(gradation, "BEGIN");
        int endIndex = findEnd(tokenList, tokenListIndex);
        while (tokenListIndex < endIndex) {
            Token next = getNext(tokenList);
            if (next != null && next.getSym().equals("ENDSYM")) {
                Token end = getNext(tokenList);
                if (end != null && (end.getSym().equals("SYM_.") || end.getSym().equals("SYM_;"))) {
//                    gen("OPR", "0", "0");
                    tree(gradation, end.getSym().equals("SYM_.") ? "." : ";");
                    return;
                } else {
                    PL0Error.log(14);
                }
            } else {
                tree(gradation, "<语句>");
                parse(tokenList, declarationList, startIndex, endIndex, gradation);
            }
        }
        tree(gradation, "END");
    }


    /**
     * 翻译布尔表达式，如 x < y
     * <p>
     * //     * @param left            左面的元素
     * //     * @param right           右面的元素
     * //     * @param operator        操作符
     * //     * @param declarationList 声明列表
     */
    private static void booleanParser(Token operator, int gradation) {
        tree(gradation, "<关系运算符>");
        gradation++;
        switch (operator.getSym()) {
            case "SYM_==":
                tree(gradation, "==");
                break;
            case "SYM_<>":
                tree(gradation, "<>");
                break;
            case "SYM_#":
                tree(gradation, "#");
                break;
            case "SYM_<":
                tree(gradation, "<");
                break;
            case "SYM_<=":
                tree(gradation, "<=");
                break;
            case "SYM_>":
                tree(gradation, ">");
                break;
            case "SYM_>=":
                tree(gradation, ">=");
                break;
        }
    }

    private static void conditionParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList,
                                        Token left, Token right, Token operator, int gradation) {
        tree(gradation++, "<条件>");
        if (left.getSym().equals("IDENT") || left.getSym().equals("NUMBER") || left.getSym().equals("SYM_(")) {
            expressionParser(tokenList, declarationList, gradation);
            booleanParser(operator, gradation);
            expressionParser(tokenList, declarationList, gradation);
        } else {
            PL0Error.log(11);
        }


    }

    /**
     * 翻译if
     *
     * @param tokenList       token列表
     * @param declarationList 声明列表
     */
    private static void ifParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList, int startIndex,
                                 int endIndex, int gradation) {
        int leftIndex = tokenListIndex + 1;
        int operatorIndex = tokenListIndex + 2;
        int rightIndex = tokenListIndex + 3;
        int thenIndex = tokenListIndex + 4;
        Token left = tokenList.get(leftIndex);
        Token operator = tokenList.get(operatorIndex);
        Token right = tokenList.get(rightIndex);
        Token then = tokenList.get(thenIndex);

        if (left != null && operator != null && right != null && then != null && then.getSym().equals("THENSYM")) {
            tree(gradation, "IF");
            conditionParser(tokenList, declarationList, left, right, operator, gradation);
            Token next = getNext(tokenList);
            tree(gradation, "THEN");
            tree(gradation, "<语句>");
            if (next != null && next.getSym().equals("BEGINSYM")) {
                tree(++gradation, "<复合语句>");
                gradation++;
                beginParser(tokenList, declarationList, startIndex, gradation);
            } else {
                PL0Error.log(11);
            }
        } else {
            PL0Error.log(11);
        }
    }

    /**
     * 翻译while
     *
     * @param tokenList       token列表
     * @param declarationList 声明列表
     */
    private static void whileParser(ArrayList<Token> tokenList, ArrayList<Declaration> declarationList,
                                    int startIndex, int endIndex, int gradation) {
        int leftIndex = tokenListIndex + 1;
        int operatorIndex = tokenListIndex + 2;
        int rightIndex = tokenListIndex + 3;
        int doIndex = tokenListIndex + 4;
        Token left = tokenList.get(leftIndex);
        Token operator = tokenList.get(operatorIndex);
        Token right = tokenList.get(rightIndex);
        Token doIdent = tokenList.get(doIndex);
        if (left != null && operator != null && right != null && doIdent != null && doIdent.getSym().equals("DOSYM")) {
            tree(gradation, "WHILE");
            conditionParser(tokenList, declarationList, left, right, operator, gradation);
            Token next = getNext(tokenList);
            tree(gradation, "DO");
            tree(gradation, "<语句>");

            if (next != null && next.getSym().equals("BEGINSYM")) {
                tree(++gradation, "<复合语句>");
                gradation++;
                beginParser(tokenList, declarationList, startIndex, gradation);
            } else {
                PL0Error.log(13);
            }
        } else {
            PL0Error.log(13);
        }
    }

    private static Token getNext(ArrayList<Token> tokenList) {
        if (tokenListIndex < tokenList.size() - 1) {
            tokenListIndex++;
            return tokenList.get(tokenListIndex);
        } else {
            return null;
        }
    }

    /**
     * 判断某个token是否已经在declaration list中声明
     *
     * @param token           token
     * @param declarationList 声明列表
     * @return 如果有，返回index，若没有，返回-1
     */
    private static int getItemIndexInDeclarationList(Token token, ArrayList<Declaration> declarationList) {
        for (int a = 0; a < declarationList.size(); a++) {
            if (token.getId().equals(declarationList.get(a).getName())) {
                return a;
            }
        }
        return -1;
    }

    /**
     * 匹配与当前begin对应的end
     *
     * @param tokenArrayList token列表
     * @param index          begin的位置
     * @return 对应的end的位置
     */
    private static int findEnd(ArrayList<Token> tokenArrayList, int index) {
        int begin = 1;
        for (int a = index + 1; a < tokenArrayList.size(); a++) {
            if (tokenArrayList.get(a).getSym().equals("BEGINSYM")) {
                begin++;
            } else if (tokenArrayList.get(a).getSym().equals("ENDSYM")) {
                begin--;
                if (begin == 0) {
                    return a;
                }
            }
        }
        return 0;
    }

}
