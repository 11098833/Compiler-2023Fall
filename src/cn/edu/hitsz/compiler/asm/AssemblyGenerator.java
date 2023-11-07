package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;


/**
 * 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    List<Instruction> instructions = new ArrayList<>();
    List<String> AsmInstructions = new ArrayList<>();
    /** 记录寄存器的占用信息 */
    BMap<String, IRValue> registerUsage = new BMap<>();
    /** 记录某个变量对寄存器的使用, 当该变量在中间代码出现一次, 引用数加一,
     * 当该变量在代码生成被使用一次时，就对应的引用次数减一 */
    Map<IRValue, Integer> usageCounter = new HashMap<>();
    /** 在代码生成时, 约定使用RISC-V临时寄存器: t0-t6 */
    List<String> registers = List.of("t0", "t1", "t2", "t3", "t4", "t5", "t6");

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // 读入前端提供的中间代码并生成所需要的信息
        /* 对读入的代码进行预处理：
        * 将操作两个立即数的指令直接进行求值得到结果, 然后替换成MOV指令;
        * 将操作一个立即数的指令 (除了乘法和左立即数减法) 进行调整, 使之满足a := b op imm 的格式;
        * 将操作一个立即数的乘法和左立即数减法调整, 前插一条MOV a, imm, 用a替换原立即数, 将指令调整为无立即数指令;
        * 对于一个操作数的指令:
        * 根据语言规定，当遇到 Ret 指令后直接舍弃后续指令  */
        for (Instruction instruction : originInstructions) {
            switch (instruction.getKind()) {
                case MOV -> {
                    // MOV指令无需处理
                    instructions.add(instruction);
                    // 记录产生式变量的引用
                    doCount(instruction.getResult());
                    doCount(instruction.getFrom());
                }
                case ADD -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    if (lhs.isImmediate()) {
                        if (rhs.isImmediate()) {
                            // 左右操作数全是立即数, 立即求值, 用MOV指令代替
                            IRImmediate ilhs = (IRImmediate) lhs;
                            IRImmediate irhs = (IRImmediate) rhs;
                            int result = ilhs.getValue() + irhs.getValue();
                            IRImmediate iResult = IRImmediate.of(result);
                            instructions.add(Instruction.createMov(instruction.getResult(), iResult));
                            doCount(instruction.getResult());
                        } else {
                            // 只有左操作数为立即数, 调换左右操作数
                            instructions.add(Instruction.createAdd(instruction.getResult(), rhs, lhs));
                            doCount(rhs);
                            doCount(instruction.getResult());
                        }
                    } else {
                        // 左操作数不是立即数, 不用处理
                        instructions.add(instruction);
                        doCount(lhs);
                        doCount(rhs);
                        doCount(instruction.getResult());
                    }
                }
                case SUB -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    if (lhs.isImmediate()) {
                        if (rhs.isImmediate()) {
                            // 左右操作数全是立即数, 立即求值, 用MOV指令代替
                            IRImmediate ilhs = (IRImmediate) lhs;
                            IRImmediate irhs = (IRImmediate) rhs;
                            int result = ilhs.getValue() - irhs.getValue();
                            IRImmediate iResult = IRImmediate.of(result);
                            instructions.add(Instruction.createMov(instruction.getResult(), iResult));
                            doCount(instruction.getResult());
                        } else {
                            // 只有左操作数为立即数, 前插一条MOV指令, 使其变为对变量的操作
                            IRVariable temp = IRVariable.temp();
                            instructions.add(Instruction.createMov(temp, lhs));
                            instructions.add(Instruction.createSub(instruction.getResult(), temp, rhs));
                            doCount(temp);
                            doCount(lhs);
                            doCount(rhs);
                            doCount(instruction.getResult());
                        }
                    } else {
                        // 左操作数不是立即数, 不用处理
                        instructions.add(instruction);
                        doCount(lhs);
                        doCount(rhs);
                        doCount(instruction.getResult());
                    }
                }
                case MUL -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    if (lhs.isImmediate()) {
                        if (rhs.isImmediate()) {
                            // 左右操作数全是立即数, 立即求值, 用MOV指令代替
                            IRImmediate ilhs = (IRImmediate) lhs;
                            IRImmediate irhs = (IRImmediate) rhs;
                            int result = ilhs.getValue() * irhs.getValue();
                            IRImmediate iResult = IRImmediate.of(result);
                            instructions.add(Instruction.createMov(instruction.getResult(), iResult));
                            doCount(instruction.getResult());
                        } else {
                            // 只有左操作数为立即数, 前插一条MOV指令, 使其变为对变量的操作
                            IRVariable temp = IRVariable.temp();
                            instructions.add(Instruction.createMov(temp, lhs));
                            instructions.add(Instruction.createAdd(instruction.getResult(), temp, rhs));
                            doCount(temp);
                            doCount(lhs);
                            doCount(rhs);
                            doCount(instruction.getResult());
                        }
                    } else if (rhs.isImmediate()) {
                        // 只有左操作数为立即数, 也要前插一条MOV指令, 使其变为对变量的操作
                        IRVariable temp = IRVariable.temp();
                        instructions.add(Instruction.createMov(temp, rhs));
                        instructions.add(Instruction.createAdd(instruction.getResult(), lhs, temp));
                        doCount(temp);
                        doCount(lhs);
                        doCount(rhs);
                        doCount(instruction.getResult());
                    } else {
                        // 对于乘法, 只有左右操作数全为变量, 才可以不用处理
                        instructions.add(instruction);
                        doCount(lhs);
                        doCount(rhs);
                        doCount(instruction.getResult());
                    }
                }
                case RET -> {
                    instructions.add(instruction);
                    // return语句使用a0寄存器返回, 故不用再寄存器表中注册引用
                    return;
                }
                default -> throw new RuntimeException("Unknown instruction kind: " + instruction.getKind());
            }
        }
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // 执行寄存器分配与代码生成
        for (Instruction instruction : instructions) {
            IRValue rst;
            String resultValue = "";
            if (!instruction.getKind().isReturn()) {
                rst = instruction.getResult();
                resultValue = registerAllocate(rst);
            }
            IRValue lhs;
            IRValue rhs;
            switch (instruction.getKind()) {
                case MOV -> {
                    IRValue form = instruction.getFrom();
                    String num = "";
                    String asm = "";
                    if (form.isImmediate()) {
                        IRImmediate iForm = (IRImmediate) form;
                        num = iForm.toString();
                        asm = "li " + resultValue + ", " + num + " \t\t#" + instruction;
                    } else {
                        // 操作数是变量
                        num = registerAllocate(form);
                        asm = "mv " + resultValue + ", " + num + " \t\t#" + instruction;
                    }
                    AsmInstructions.add(asm);
                }
                case ADD -> {
                    lhs = instruction.getLHS();
                    rhs = instruction.getRHS();
                    String a = registerAllocate(lhs);
                    String b = "";
                    String asm = "";
                    if (rhs.isImmediate()) {
                        IRImmediate irhs = (IRImmediate) rhs;
                        b = irhs.toString();
                        asm = "addi " + resultValue + ", " + a + ", " + b + " \t\t#" + instruction;
                    } else {
                        // 两个操作数全是变量
                        b = registerAllocate(rhs);
                        asm = "add " + resultValue + ", " + a + ", " + b + " \t\t#" + instruction;
                    }
                    AsmInstructions.add(asm);
                }
                case SUB, MUL -> {
                    // 经过预处理, 乘法和减法的两个操作数全为变量
                    lhs = instruction.getLHS();
                    rhs = instruction.getRHS();
                    String a = registerAllocate(lhs);
                    String b = registerAllocate(rhs);
                    String op = "";
                    switch (instruction.getKind()) {
                        case SUB -> op = "sub";
                        case MUL -> op = "mul";
                        default -> throw new RuntimeException("Unexpected Wrong!");
                    }
                    String asm = op + " " + resultValue + ", " + a + ", " + b + " \t\t#" + instruction;
                    AsmInstructions.add(asm);
                }
                case RET -> {
                    String returnValue = registerAllocate(instruction.getReturnValue());
                    String asm = "mv a0, " + returnValue + " \t\t#" + instruction;
                    AsmInstructions.add(asm);
                    return;
                }
                default -> throw new RuntimeException("Unknown instruction kind: " + instruction.getKind());
            }
        }
    }

    /**
     * 对irValue在usageCounter中做计数
     * @param irValue 待检查的IRValue
     */
    private void doCount(IRValue irValue) {
        if (irValue.isIRVariable()) {
            IRVariable irVariable = (IRVariable) irValue;
            if (usageCounter.containsKey(irVariable)) {
                // 在表中注册过的变量, 将其计数加一
                Integer num = usageCounter.get(irValue);
                usageCounter.put(irValue, num+1);
            } else {
                // 该变量首次被引用, 在表中注册, 初始化计数为1
                usageCounter.put(irValue, 1);
            }
        }
    }

    /**
     * 不完备的寄存器分配算法: 若有寄存器空闲, 则指派任意空闲寄存器分配;
     * 当无寄存器空闲时, 判断当前是否有寄存器被不再使用的变量占用,
     * 若有则指派任意满足该条件的寄存器, 若无则直接报错
     * @return 分配到的寄存器名称
     */
    private String registerAllocate(IRValue irValue) {
        String register = "";
        if (registerUsage.containsValue(irValue)) {
            // 该变量已经在寄存器中, 使用原本的寄存器
            register = registerUsage.getByValue(irValue);
            // 更新该变量对寄存器的引用次数
            int cnt = usageCounter.get(irValue);
            usageCounter.put(irValue, cnt-1);
        } else {
            for (String t : registers) {
                if (!registerUsage.containsKey(t)) {
                    // 遍历寄存器表, 找到一个未被引用的可用寄存器
                    register = t;
                    registerUsage.replace(register, irValue);
                    // 更新该变量对寄存器的引用次数
                    int cnt = usageCounter.get(irValue);
                    usageCounter.put(irValue, cnt-1);
                    return register;
                }
            }
        }
        if ("".equals(register)) {
            // 无引用寄存器分配失败, 试图获取一个未被引用的寄存器
            for (IRValue i : usageCounter.keySet()) {
                Integer num = usageCounter.get(i);
                if (num == 0) {
                    register = registerUsage.getByValue(i);
                    if (register != null) {
                        // 得到了一个空闲的寄存器
                        registerUsage.replace(register, irValue);
                        // 更新该变量对寄存器的引用次数
                        int cnt = usageCounter.get(irValue);
                        usageCounter.put(irValue, cnt-1);
                        return register;
                    }
                }
            }
            // 获取不到未被引用的寄存器, 直接报错
            throw new RuntimeException("There is no more register");
        }
        return register;
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        List<String> opt = new LinkedList<>();
        opt.add(".text");
        for (String instruction : AsmInstructions) {
            opt.add("\t" + instruction);
        }
        FileUtils.writeLines(path, opt);
    }
}

