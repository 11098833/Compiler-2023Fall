package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    private final List<Token> tokenList = new ArrayList<>();

    StringBuffer buffer = new StringBuffer();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // 词法分析前的缓冲区实现
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            String readTemp = null;
            while ((readTemp = bufferedReader.readLine()) != null) {
                buffer.append(readTemp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // 自动机实现的词法分析过程
        int index = 0;                      // 待检查的符号的索引
        int lengthMax = buffer.length();    // 代码段的总长度
        char temp;                          // 待检测的符号
        int state = 0;                      // 状态机当前的状态

        // 检测到的标志符或保留字
        StringBuffer str = new StringBuffer();
        String text;

        // 跳过无意义的代码段
        index = skipNonsense(index);

        // 通过状态机，逐个字符检查代码，分析各段的单词成分
        while (index < lengthMax) {
            temp = buffer.charAt(index);
            switch (state) {
                case 0:
                    if (isLetter(temp)) {
                        // 拼接，记录当前检测到的标识符或保留字
                        str.append(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 1;
                    } else if (isDigit(temp)) {
                        // 拼接，记录当前检查到的数字
                        str.append(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 3;
                    } else {
                        // 分界符与运算符的情况，直接加入到Token列表中
                        dealSymbol(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 999;
                    }
                    break;
                case 1:
                    if (isLetter(temp)) {
                        // 拼接，记录当前检测到的标识符或保留字
                        str.append(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 1;
                    } else if (isDigit(temp)) {
                        // 拼接，记录当前检测到的标识符或保留字
                        str.append(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 2;
                    } else {
                        // 标识符或关键字检测结束
                        // 先将标识符或关键字添加到Token列表
                        text = str.toString();
                        switch (text) {
                            case "int", "return":
                                // 保留字的情况
                                tokenList.add(Token.simple(text));
                                break;
                            default:
                                // 标识符的情况
                                tokenList.add(Token.normal("id",text));
                                // 在读到一个标识符时 (即生成类别为 id 的词法单元时), 检测符号表中是否已含有该标识符, 若无向符号表加入该标识符
                                if (!symbolTable.has(text)) {
                                    symbolTable.add(text);
                                }
                                break;
                        }
                        // 重置标识符的缓存str
                        str = new StringBuffer();

                        // 再处理当前符号，即分界符或运算符，也加到Token列表
                        dealSymbol(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 999;
                    }
                    break;
                case 2:
                    // 此时一定是个标识符
                    if (isDigit(temp) || isLetter(temp)) {
                        // 拼接，记录当前检测到的标识符或保留字
                        str.append(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 2;
                    } else {
                        // 标识符检测结束
                        // 先将标识符添加到Token列表
                        text = str.toString();
                        tokenList.add(Token.normal("id",text));
                        // 在读到一个标识符时 (即生成类别为 id 的词法单元时), 检测符号表中是否已含有该标识符, 若无向符号表加入该标识符
                        if (!symbolTable.has(text)) {
                            symbolTable.add(text);
                        }
                        // 重置标识符的缓存str
                        str = new StringBuffer();

                        // 再处理当前符号，即分界符或运算符，也加到Token列表
                        dealSymbol(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 999;
                    }
                    break;
                case 3:
                    // 整数常量的情况
                    if (isDigit(temp)) {
                        // 拼接，记录当前检测到的标识符或保留字
                        str.append(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 3;
                    } else {
                        // 整数常量检测结束
                        // 先将整数常量添加到Token列表
                        text = str.toString();
                        tokenList.add(Token.normal("IntConst",text));
                        // 重置标识符的缓存str
                        str = new StringBuffer();

                        // 再处理当前符号，即分界符或运算符，也加到Token列表
                        dealSymbol(temp);
                        // 索引后移，状态转移
                        index++;
                        state = 999;
                    }
                    break;
                default:
                    // 执行状态999的操作，跳过无意义的代码段
                    index = skipNonsense(index);
                    state = 0;
                    break;
            }
        }
        tokenList.add(Token.eof());




    }


    /**
     * 对于buffer，指定开始区间index，跳过无意义的代码段
     * @param index 开始检查的索引值
     * @return 跳过无意义的代码段后待检查的索引值
     */
    private int skipNonsense(int index) {
        boolean skipped;
        char temp;
        do{
            skipped = false;
            temp = buffer.charAt(index);
            if (isVain(temp)) {
                skipped = true;
                index++;
            }
        } while(skipped);
        return index;
    }

    /**
     * 处理遇到的分界符或运算符，直接将它添加到Token列表中，非法符号视为空格，不作处理
     * @param c 待处理的符号
     */
    private void dealSymbol(char c) {
         String text = switch (c) {
            case '=' -> "=";
            case ',' -> ",";
            case ';' -> "Semicolon";
            case '+' -> "+";
            case '-' -> "-";
            case '*' -> "*";
            case '/' -> "/";
            case '(' -> "(";
            case  ')' -> ")";
            default -> "";
        };
        if (!("".equals(text))) {
            tokenList.add(Token.simple(text));
        }
    }

    /**
     * 判断待检查的符号是否为无意义的符号，即空格、回车、换行、制表符
     * @param c 待检查的符号
     * @return 是否为无效符号
     */
    private boolean isVain(char c) {
        return ((c == '\n') || (c == '\r') || (c == ' ') || (c == '\t'));
    }

    /**
     * 判断待检查的符号是否为字母
     * @param c 待检查的符号
     * @return 是否为字母
     */
    private boolean isLetter(char c) {
        return (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')));
    }

    /**
     * 判断待检查的符号是否为数字(含0)
     * @param c 待检查的符号
     * @return 是否为数字(含0)
     */
    private boolean isDigit(char c) {
        return ((c >= '0') && (c <= '9'));
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
