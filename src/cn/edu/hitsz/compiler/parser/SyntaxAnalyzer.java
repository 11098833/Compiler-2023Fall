package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.*;

//实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();
    private final LinkedList<Token> tokenInput = new LinkedList<>();
    private LRTable lrTable;

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // 你可以自行选择要如何存储词法单元, 譬如使用迭代器, 或是栈, 或是干脆使用一个 list 全存起来
        // 需要注意的是, 在实现驱动程序的过程中, 你会需要面对只读取一个 token 而不能消耗它的情况,
        // 在自行设计的时候请加以考虑此种情况
        // 构建一个从左向右扫描的符号栈，不妨使用队列实现
        tokens.forEach(tokenInput::add);
    }

    public void loadLRTable(LRTable table) {
        // 加载 LR 分析表
        // 你可以自行选择要如何使用该表格:
        // 是直接对 LRTable 调用 getAction/getGoto, 抑或是直接将 initStatus 存起来使用
        this.lrTable = table;
    }

    public void run() {
        // 实现驱动程序
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce, callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作
        //符号栈
        Stack<Symbol> symbolStack = new Stack<>();
        //状态栈
        Stack<Status> statusStack = new Stack<>();
        // 初始化状态栈和字符栈
        statusStack.push(lrTable.getInit());
        symbolStack.push(new Symbol(Token.eof()));
        // 判断LR分析器是否已经接受
        boolean accepted = false;
        while (!accepted) {
            // 根据状态栈栈顶元素和待读入的下一个 token 查询判断下一个待执行动作
            Action action = lrTable.getAction(statusStack.peek(), tokenInput.getFirst());
            // 根据LR(1)分析表内容进行移进规约
            switch (action.getKind()) {
                // 如果是 Shift，把 Action 的状态压入状态栈，对应的 token 压入符号栈
                case Shift -> {
                    callWhenInShift(statusStack.peek(), tokenInput.peek());
                    statusStack.push(action.getStatus());
                    symbolStack.push(new Symbol(tokenInput.getFirst()));
                    tokenInput.removeFirst();
                }
                // 如果是 Reduce，根据产生式长度，符号栈和状态栈均弹出对应长度个 token 和状态
                // 把产生式左侧的非终结符压入符号栈；根据符号栈和状态栈栈顶状态获取 Goto 表的状态，压入状态栈
                // 通知各个观察者
                case Reduce -> {
                    Production p = action.getProduction();
                    for (int i = 0; i < p.body().size(); i++) {
                        symbolStack.pop();
                        statusStack.pop();
                    }
                    symbolStack.push(new Symbol(p.head()));
                    statusStack.push(lrTable.getGoto(statusStack.peek(), symbolStack.peek().getNonTerminal()));
                    callWhenInReduce(statusStack.peek(), p);
                }
                // 如果是 Accept，语法分析执行结束
                case Accept -> {
                    /*if (tokenInput.getFirst().getKind() == TokenKind.eof()) {
                        tokens.removeFirst();
                        accepted = true;
                    }*/
                    callWhenInAccept(statusStack.peek());
                    accepted = true;
                }
                // 非法句型
                case Error -> throw new RuntimeException("LR分析失败: 输入符号可能有误");
                // 意外的动作类型
                default -> throw new IllegalStateException("Unexpected action kind: " + action.getKind() + "!");
            }
        }
    }
}
