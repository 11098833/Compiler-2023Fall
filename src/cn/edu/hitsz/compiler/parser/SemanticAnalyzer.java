package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

// 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    /** 符号表 */
    SymbolTable symbolTable;
    /** 语义分析栈 */
    Stack<Symbol> stack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // 该过程在遇到 Accept 时清空当前栈
        while (!stack.empty()) {
            stack.pop();
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // 该过程在遇到 reduce production 时更新符号表标识符的type 属性,根据 production 的 index 来判断当前是哪条产生式,
        // 编写该产生式的具体翻译动作
        switch (production.index()) {
            // 我们推荐在 case 后面使用注释标明产生式
            // 这样能比较清楚地看出产生式索引与产生式的对应关系
            case 4 -> { // S -> D id
                // 取得id
                String idName = stack.pop().getToken().getText();
                // 取得D的type
                SourceCodeType type = stack.pop().getSourceCodeType();
                if (symbolTable.has(idName)) {
                    // 将D的type传递给id
                    symbolTable.get(idName).setType(type);
                } else {
                    System.out.println(idName + ": No Such id!");
                }
                // 将S压入栈中
                stack.push(new Symbol(production.head(), type));
            }
            case 7 -> { // S -> return E
                Symbol symbol = stack.pop();
                stack.pop();
                stack.push(new Symbol(production.head(), symbol.getSourceCodeType()));
            }
            case 1, 2, 3, 8, 9, 10, 11, 12 -> { // 其实这里可以作类型检验, 但不在本实验要求内了
                // 从栈中弹出除第一个以外的符号, 再弹出第一个, 取得其type记录到产生式左侧的符号, 压入栈
                for (int i = 0; i < production.body().size()-1; i++) {
                    stack.pop();
                }
                stack.push(new Symbol(production.head(),stack.pop().getSourceCodeType()));
            }
            case 13 -> { // B -> ( E )
                stack.pop();
                Symbol symbol = stack.pop();
                stack.pop();
                stack.push(new Symbol(production.head(), symbol.getSourceCodeType()));
            }
            case 6, 14 -> { // B -> id 或 S -> id = E
                for (int i = 0; i < production.body().size()-1; i++) {
                    stack.pop();
                }
                // 取得id
                String idName = stack.pop().getToken().getText();
                // 将B或S的type指定为id的type, 压入栈
                if (symbolTable.has(idName)) {
                    stack.push(new Symbol(production.head(), symbolTable.get(idName).getType()));
                } else {
                    System.out.println(idName + ": No Such id!");
                    stack.push(new Symbol(production.head()));
                }
            }
            case 5, 15 -> { // D -> int 或 B -> IntConst
                // 将D或B的type指定为Int, 压入栈
                stack.pop();
                stack.push(new Symbol(production.head(), SourceCodeType.Int));
            }
            // ...
            default -> { //
                 throw new RuntimeException("Unknown production index");
                // 或者任何默认行为
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        stack.push(new Symbol(currentToken));
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        symbolTable = table;
    }
}

