package cn.edu.hitsz.compiler.parser.table;

import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

/**
 * ClassName: Symbol
 * Package: cn.edu.hitsz.compiler.parser.table
 * Charset: UTF-8
 * <p>
 * Description:在处理符号栈时, 我们可能希望将 Token 和 NonTerminal 同时装在栈中.<br>
 * 但 Token 和 NonTerminal 并没有共同祖先类(除了Object). 我们当然可以使用 Stack<Object>, 但其在使用中有诸多不便. <br>
 * 于是我们期望有一个类似 Union<Token, NonTerminal> 的结构, 就可以将栈定义为: Stack<Union<Token, NonTerminal>>. <br>
 * 因为我们只将 Union 在这里使用一次, 我们可以简单定义一个 Symbol 来实现 Union<Token, NonTerminal> 的功能
 *
 * @Author Anemon_ZY
 * @Create 2023/10/23 18:34
 * @Version 0.1
 */
public class Symbol{
    Token token;
    NonTerminal nonTerminal;
    SourceCodeType sourceCodeType;
    IRValue irValue;

    private Symbol(Token token, NonTerminal nonTerminal){
        this.token = token;
        this.nonTerminal = nonTerminal;
    }

    public Symbol(Token token) {
        this.token = token;
    }

    public Symbol(NonTerminal nonTerminal){
        this.nonTerminal = nonTerminal;
    }

    public Symbol(Token token, SourceCodeType sourceCodeType) {
        this.token = token;
        this.sourceCodeType = sourceCodeType;
    }

    public Symbol(NonTerminal nonTerminal, SourceCodeType sourceCodeType) {
        this.nonTerminal = nonTerminal;
        this.sourceCodeType = sourceCodeType;
    }

    public Symbol(Token token, IRValue irValue) {
        this.token = token;
        this.irValue = irValue;
    }

    public Symbol(NonTerminal nonTerminal, IRValue irValue) {
        this.nonTerminal = nonTerminal;
        this.irValue = irValue;
    }

    public boolean isToken(){
        return this.token != null;
    }

    public boolean isNonTerminal(){
        return this.nonTerminal != null;
    }

    public Token getToken() {
        return token;
    }

    public NonTerminal getNonTerminal() {
        return nonTerminal;
    }

    public SourceCodeType getSourceCodeType() {
        return sourceCodeType;
    }

    public void setSourceCodeType(SourceCodeType sourceCodeType) {
        this.sourceCodeType = sourceCodeType;
    }

    public IRValue getIrValue() {
        return irValue;
    }

    public void setIrValue(IRValue irValue) {
        this.irValue = irValue;
    }
}
