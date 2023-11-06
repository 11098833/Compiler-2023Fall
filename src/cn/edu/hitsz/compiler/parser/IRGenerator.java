package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// 实验三: 实现 IR 生成

/**
 *  IR 是指令与变量分离的三地址形式的中间表示，各指令接受数量不定的变量作为参数，<br>
 *  代表执行某种动作，如果有运算结果的话还会将结果存放于变量中。<br>
 *  而变量有可能被多条指令使用，代表某种能存放信息的位置，在指令之间传递信息与结果。
 */
public class IRGenerator implements ActionObserver {

    /** 符号表 */
    SymbolTable symbolTable;
    /** 语义分析栈 */
    Stack<Symbol> stack = new Stack<>();
    /** 中间代码序列 */
    List<Instruction> instructions = new ArrayList<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        if ("IntConst".equals(currentToken.getKindId())) {
            IRImmediate irImmediate = IRImmediate.of(Integer.parseInt(currentToken.getText()));
            stack.push(new Symbol(currentToken, irImmediate));
        } else {
            // 非id的终结符在Token里命名都为空串, 在规约时直接跳过也不会产生影响
            // 故将所有非id的终结符视为名为""的id处理
            IRVariable irVariable = IRVariable.named(currentToken.getText());
            stack.push(new Symbol(currentToken, irVariable));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // 在执行 reduce 的时候生成中间代码
        IRVariable result;
        IRValue lhs;
        IRValue rhs;
        switch (production.index()) {
            /* P -> S_list
               S_list -> S Semicolon S_list
               S_list -> S Semicolon
               S -> D id
               D -> int */
            case 1, 2, 3, 4, 5 -> { // 不产生中间代码的表达式
                // 直接将右部符号弹出, 将左部非终结符压入占位
                for (int i = 0; i < production.body().size(); i++) {
                    stack.pop();
                }
                stack.push(new Symbol(production.head()));
            }
            case 6 -> { // S -> id = E
                // 赋值语句
                rhs = stack.pop().getIrValue();
                stack.pop();
                lhs = stack.pop().getIrValue();
                if (lhs.isIRVariable()) {
                    instructions.add(Instruction.createMov((IRVariable) lhs, rhs));
                } else {
                    throw new RuntimeException(lhs + " is not an IRVariable!");
                }
                stack.push(new Symbol(production.head()));
            }
            case 7 -> { // S -> return E
                // 返回语句
                instructions.add(Instruction.createRet(stack.pop().getIrValue()));
                stack.push(new Symbol(production.head()));
            }
            case 8 -> {  // E -> E + A
                // 加法运算
                rhs = stack.pop().getIrValue();
                stack.pop();
                lhs = stack.pop().getIrValue();
                result = IRVariable.temp();
                instructions.add(Instruction.createAdd(result, lhs, rhs));
                stack.push(new Symbol(production.head(), result));
            }
            case 9 -> {  // E -> E - A
                // 减法运算
                rhs = stack.pop().getIrValue();
                stack.pop();
                lhs = stack.pop().getIrValue();
                result = IRVariable.temp();
                instructions.add(Instruction.createSub(result, lhs, rhs));
                stack.push(new Symbol(production.head(), result));
            }
            case 11 -> { // A -> A * B
                // 乘法运算
                rhs = stack.pop().getIrValue();
                stack.pop();
                lhs = stack.pop().getIrValue();
                result = IRVariable.temp();
                instructions.add(Instruction.createMul(result, lhs, rhs));
                stack.push(new Symbol(production.head(), result));
            }
            /* E -> A
               A -> B
               B -> id
               B -> IntConst */
            case 10, 12, 14, 15 -> { // 单纯传递IRValue的产生式
                IRValue irValue = stack.pop().getIrValue();
                stack.push(new Symbol(production.head(), irValue));
            }
            // 虽然也是传递IRValue的产生式, 但因为格式不同, 要单独处理
            case 13 -> { // B -> ( E )
                stack.pop();
                IRValue irValue = stack.pop().getIrValue();
                stack.pop();
                stack.push(new Symbol(production.head(), irValue));
            }
            default -> {
                throw new RuntimeException("Unknown production index");
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // 该过程在遇到 Accept 时清空当前栈
        while (!stack.empty()) {
            stack.pop();
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
    }

    public List<Instruction> getIR() {
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

