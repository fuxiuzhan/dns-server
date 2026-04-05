package com.fxz.console.service;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java-like evaluate-expression (no Groovy): bean roots, {@code bean.prop} (getter/field),
 * indexing {@code [i]} / {@code ["k"]}, method calls (e.g. {@link List#add}), assignment, basic operators.
 */
public final class JavaLikeExpressionExecutor {

    public static final class ExpressionException extends Exception {
        public ExpressionException(String message) {
            super(message);
        }
    }

    private static final Class<?>[] EMPTY_TYPES = new Class<?>[0];

    public Object evaluate(String source, Map<String, Object> roots) throws ExpressionException {
        if (source == null || source.trim().isEmpty()) {
            throw new ExpressionException("表达式为空");
        }
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer);
        BlockNode program = parser.parseProgram();
        return Evaluator.evalProgram(program, roots);
    }

    private enum Tk {
        EOF, IDENT, INT, LONG, DOUBLE, STRING,
        LPAREN, RPAREN, LBRACK, RBRACK, DOT, COMMA, SEMI,
        EQ, EQ_EQ, NE, LT, LE, GT, GE, AND_AND, OR_OR,
        BANG, PLUS, MINUS, STAR, SLASH, PERCENT,
        NULL, TRUE, FALSE
    }

    private static final class Token {
        final Tk type;
        final String text;
        final Number number;

        Token(Tk type, String text, Number number) {
            this.type = type;
            this.text = text;
            this.number = number;
        }
    }

    private static final class Lexer {
        private final String s;
        private int pos;
        private Token peek;

        Lexer(String source) {
            this.s = source;
            this.pos = 0;
        }

        Token peek() throws ExpressionException {
            if (peek == null) {
                peek = read();
            }
            return peek;
        }

        Token next() throws ExpressionException {
            Token t = peek();
            peek = null;
            return t;
        }

        boolean eof() throws ExpressionException {
            return peek().type == Tk.EOF;
        }

        private void skipWsAndComments() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                    continue;
                }
                if (c == '/' && pos + 1 < s.length()) {
                    if (s.charAt(pos + 1) == '/') {
                        pos += 2;
                        while (pos < s.length() && s.charAt(pos) != '\n') {
                            pos++;
                        }
                        continue;
                    }
                    if (s.charAt(pos + 1) == '*') {
                        pos += 2;
                        while (pos + 1 < s.length()) {
                            if (s.charAt(pos) == '*' && s.charAt(pos + 1) == '/') {
                                pos += 2;
                                break;
                            }
                            pos++;
                        }
                        continue;
                    }
                }
                break;
            }
        }

        private Token read() throws ExpressionException {
            skipWsAndComments();
            if (pos >= s.length()) {
                return new Token(Tk.EOF, null, null);
            }
            char c = s.charAt(pos);
            int start = pos;

            if (c == '(') {
                pos++;
                return new Token(Tk.LPAREN, null, null);
            }
            if (c == ')') {
                pos++;
                return new Token(Tk.RPAREN, null, null);
            }
            if (c == '[') {
                pos++;
                return new Token(Tk.LBRACK, null, null);
            }
            if (c == ']') {
                pos++;
                return new Token(Tk.RBRACK, null, null);
            }
            if (c == '{') {
                throw new ExpressionException("不支持 '{'，Map 请使用 [\"key\"] 形式");
            }
            if (c == '}') {
                throw new ExpressionException("不支持的字符 '}'");
            }
            if (c == '.') {
                pos++;
                return new Token(Tk.DOT, null, null);
            }
            if (c == ',') {
                pos++;
                return new Token(Tk.COMMA, null, null);
            }
            if (c == ';') {
                pos++;
                return new Token(Tk.SEMI, null, null);
            }
            if (c == '!') {
                if (pos + 1 < s.length() && s.charAt(pos + 1) == '=') {
                    pos += 2;
                    return new Token(Tk.NE, null, null);
                }
                pos++;
                return new Token(Tk.BANG, null, null);
            }
            if (c == '=') {
                if (pos + 1 < s.length() && s.charAt(pos + 1) == '=') {
                    pos += 2;
                    return new Token(Tk.EQ_EQ, null, null);
                }
                pos++;
                return new Token(Tk.EQ, null, null);
            }
            if (c == '<') {
                if (pos + 1 < s.length() && s.charAt(pos + 1) == '=') {
                    pos += 2;
                    return new Token(Tk.LE, null, null);
                }
                pos++;
                return new Token(Tk.LT, null, null);
            }
            if (c == '>') {
                if (pos + 1 < s.length() && s.charAt(pos + 1) == '=') {
                    pos += 2;
                    return new Token(Tk.GE, null, null);
                }
                pos++;
                return new Token(Tk.GT, null, null);
            }
            if (c == '&' && pos + 1 < s.length() && s.charAt(pos + 1) == '&') {
                pos += 2;
                return new Token(Tk.AND_AND, null, null);
            }
            if (c == '|' && pos + 1 < s.length() && s.charAt(pos + 1) == '|') {
                pos += 2;
                return new Token(Tk.OR_OR, null, null);
            }
            if (c == '+') {
                pos++;
                return new Token(Tk.PLUS, null, null);
            }
            if (c == '-') {
                pos++;
                return new Token(Tk.MINUS, null, null);
            }
            if (c == '*') {
                pos++;
                return new Token(Tk.STAR, null, null);
            }
            if (c == '/') {
                pos++;
                return new Token(Tk.SLASH, null, null);
            }
            if (c == '%') {
                pos++;
                return new Token(Tk.PERCENT, null, null);
            }
            if (c == '"' || c == '\'') {
                char q = c;
                pos++;
                StringBuilder sb = new StringBuilder();
                while (pos < s.length()) {
                    char ch = s.charAt(pos++);
                    if (ch == '\\' && pos < s.length()) {
                        char esc = s.charAt(pos++);
                        if (esc == 'n') {
                            sb.append('\n');
                        } else if (esc == 't') {
                            sb.append('\t');
                        } else if (esc == 'r') {
                            sb.append('\r');
                        } else {
                            sb.append(esc);
                        }
                    } else if (ch == q) {
                        return new Token(Tk.STRING, sb.toString(), null);
                    } else {
                        sb.append(ch);
                    }
                }
                throw new ExpressionException("未闭合的字符串");
            }

            if (Character.isDigit(c)) {
                return readNumber(start);
            }
            if (Character.isJavaIdentifierStart(c)) {
                return readIdentOrKeyword(start);
            }
            throw new ExpressionException("非法字符: '" + c + "' 位置 " + pos);
        }

        private Token readNumber(int start) throws ExpressionException {
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                pos++;
            }
            boolean isDouble = false;
            if (pos < s.length() && s.charAt(pos) == '.') {
                isDouble = true;
                pos++;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                    pos++;
                }
            }
            String num = s.substring(start, pos);
            boolean isLong = pos < s.length() && (s.charAt(pos) == 'L' || s.charAt(pos) == 'l');
            if (isLong) {
                pos++;
                try {
                    return new Token(Tk.LONG, num, Long.parseLong(num));
                } catch (NumberFormatException e) {
                    throw new ExpressionException("非法 long: " + num);
                }
            }
            if (isDouble) {
                try {
                    return new Token(Tk.DOUBLE, num, Double.parseDouble(num));
                } catch (NumberFormatException e) {
                    throw new ExpressionException("非法 double: " + num);
                }
            }
            try {
                if (num.length() > 9) {
                    return new Token(Tk.LONG, num, Long.parseLong(num));
                }
                return new Token(Tk.INT, num, Integer.parseInt(num));
            } catch (NumberFormatException e) {
                return new Token(Tk.LONG, num, Long.parseLong(num));
            }
        }

        private Token readIdentOrKeyword(int start) {
            while (pos < s.length() && Character.isJavaIdentifierPart(s.charAt(pos))) {
                pos++;
            }
            String id = s.substring(start, pos);
            if ("null".equals(id)) {
                return new Token(Tk.NULL, id, null);
            }
            if ("true".equals(id)) {
                return new Token(Tk.TRUE, id, null);
            }
            if ("false".equals(id)) {
                return new Token(Tk.FALSE, id, null);
            }
            return new Token(Tk.IDENT, id, null);
        }
    }

    private abstract static class AstNode {
    }

    private static final class BlockNode extends AstNode {
        final List<AstNode> statements = new ArrayList<AstNode>();
    }

    private static final class AssignNode extends AstNode {
        final AstNode left;
        final AstNode right;

        AssignNode(AstNode left, AstNode right) {
            this.left = left;
            this.right = right;
        }
    }

    private static final class BinaryNode extends AstNode {
        final AstNode left;
        final AstNode right;
        final Tk op;

        BinaryNode(AstNode left, Tk op, AstNode right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }
    }

    private static final class UnaryNode extends AstNode {
        final Tk op;
        final AstNode expr;

        UnaryNode(Tk op, AstNode expr) {
            this.op = op;
            this.expr = expr;
        }
    }

    private static final class IdentNode extends AstNode {
        final String name;

        IdentNode(String name) {
            this.name = name;
        }
    }

    private static final class LiteralNode extends AstNode {
        final Object value;

        LiteralNode(Object value) {
            this.value = value;
        }
    }

    /**
     * Property access args == null; method call args != null (may be empty).
     */
    private static final class MemberNode extends AstNode {
        final AstNode target;
        final String name;
        final List<AstNode> args;

        MemberNode(AstNode target, String name, List<AstNode> args) {
            this.target = target;
            this.name = name;
            this.args = args;
        }
    }

    private static final class IndexNode extends AstNode {
        final AstNode target;
        final AstNode index;

        IndexNode(AstNode target, AstNode index) {
            this.target = target;
            this.index = index;
        }
    }

    private static final class Parser {
        private final Lexer lexer;

        Parser(Lexer lexer) {
            this.lexer = lexer;
        }

        BlockNode parseProgram() throws ExpressionException {
            BlockNode block = new BlockNode();
            skipSemicolons();
            while (!lexer.eof()) {
                block.statements.add(parseAssign());
                skipSemicolons();
            }
            return block;
        }

        private void skipSemicolons() throws ExpressionException {
            while (lexer.peek().type == Tk.SEMI) {
                lexer.next();
            }
        }

        private AstNode parseAssign() throws ExpressionException {
            AstNode left = parseLogicalOr();
            if (lexer.peek().type == Tk.EQ) {
                lexer.next();
                AstNode right = parseAssign();
                return new AssignNode(left, right);
            }
            return left;
        }

        private AstNode parseLogicalOr() throws ExpressionException {
            AstNode n = parseLogicalAnd();
            while (lexer.peek().type == Tk.OR_OR) {
                lexer.next();
                n = new BinaryNode(n, Tk.OR_OR, parseLogicalAnd());
            }
            return n;
        }

        private AstNode parseLogicalAnd() throws ExpressionException {
            AstNode n = parseEquality();
            while (lexer.peek().type == Tk.AND_AND) {
                lexer.next();
                n = new BinaryNode(n, Tk.AND_AND, parseEquality());
            }
            return n;
        }

        private AstNode parseEquality() throws ExpressionException {
            AstNode n = parseRelational();
            while (true) {
                Tk t = lexer.peek().type;
                if (t == Tk.EQ_EQ || t == Tk.NE) {
                    lexer.next();
                    n = new BinaryNode(n, t, parseRelational());
                } else {
                    break;
                }
            }
            return n;
        }

        private AstNode parseRelational() throws ExpressionException {
            AstNode n = parseAdditive();
            while (true) {
                Tk t = lexer.peek().type;
                if (t == Tk.LT || t == Tk.LE || t == Tk.GT || t == Tk.GE) {
                    lexer.next();
                    n = new BinaryNode(n, t, parseAdditive());
                } else {
                    break;
                }
            }
            return n;
        }

        private AstNode parseAdditive() throws ExpressionException {
            AstNode n = parseMultiplicative();
            while (true) {
                Tk t = lexer.peek().type;
                if (t == Tk.PLUS || t == Tk.MINUS) {
                    lexer.next();
                    n = new BinaryNode(n, t, parseMultiplicative());
                } else {
                    break;
                }
            }
            return n;
        }

        private AstNode parseMultiplicative() throws ExpressionException {
            AstNode n = parseUnary();
            while (true) {
                Tk t = lexer.peek().type;
                if (t == Tk.STAR || t == Tk.SLASH || t == Tk.PERCENT) {
                    lexer.next();
                    n = new BinaryNode(n, t, parseUnary());
                } else {
                    break;
                }
            }
            return n;
        }

        private AstNode parseUnary() throws ExpressionException {
            Tk t = lexer.peek().type;
            if (t == Tk.BANG || t == Tk.MINUS || t == Tk.PLUS) {
                lexer.next();
                return new UnaryNode(t, parseUnary());
            }
            return parsePostfix();
        }

        private AstNode parsePostfix() throws ExpressionException {
            AstNode node = parsePrimary();
            while (true) {
                if (lexer.peek().type == Tk.DOT) {
                    lexer.next();
                    expect(Tk.IDENT, "需要属性或方法名");
                    String name = lexer.next().text;
                    if (lexer.peek().type == Tk.LPAREN) {
                        lexer.next();
                        List<AstNode> args = parseArgList();
                        expect(Tk.RPAREN, "需要 ')'");
                        lexer.next();
                        node = new MemberNode(node, name, args);
                    } else {
                        node = new MemberNode(node, name, null);
                    }
                } else if (lexer.peek().type == Tk.LBRACK) {
                    lexer.next();
                    AstNode idx = parseAssign();
                    expect(Tk.RBRACK, "需要 ']'");
                    lexer.next();
                    node = new IndexNode(node, idx);
                } else {
                    break;
                }
            }
            return node;
        }

        private List<AstNode> parseArgList() throws ExpressionException {
            List<AstNode> args = new ArrayList<AstNode>();
            if (lexer.peek().type == Tk.RPAREN) {
                return args;
            }
            args.add(parseAssign());
            while (lexer.peek().type == Tk.COMMA) {
                lexer.next();
                args.add(parseAssign());
            }
            return args;
        }

        private AstNode parsePrimary() throws ExpressionException {
            Token t = lexer.peek();
            switch (t.type) {
                case LPAREN:
                    lexer.next();
                    AstNode inner = parseAssign();
                    expect(Tk.RPAREN, "需要 ')'");
                    lexer.next();
                    return inner;
                case NULL:
                    lexer.next();
                    return new LiteralNode(null);
                case TRUE:
                    lexer.next();
                    return new LiteralNode(Boolean.TRUE);
                case FALSE:
                    lexer.next();
                    return new LiteralNode(Boolean.FALSE);
                case INT:
                    lexer.next();
                    return new LiteralNode(t.number);
                case LONG:
                    lexer.next();
                    return new LiteralNode(t.number);
                case DOUBLE:
                    lexer.next();
                    return new LiteralNode(t.number);
                case STRING:
                    lexer.next();
                    return new LiteralNode(t.text);
                case IDENT:
                    lexer.next();
                    return new IdentNode(t.text);
                default:
                    throw new ExpressionException("意外的 token: " + t.type);
            }
        }

        private void expect(Tk type, String msg) throws ExpressionException {
            if (lexer.peek().type != type) {
                throw new ExpressionException(msg + "，实际为 " + lexer.peek().type);
            }
        }
    }

    private static final class Evaluator {

        static Object evalProgram(BlockNode block, Map<String, Object> roots) throws ExpressionException {
            Object last = null;
            for (AstNode stmt : block.statements) {
                last = evalStatement(stmt, roots);
            }
            return last;
        }

        private static Object evalStatement(AstNode stmt, Map<String, Object> roots) throws ExpressionException {
            if (stmt instanceof AssignNode) {
                AssignNode a = (AssignNode) stmt;
                if (!isAssignable(a.left)) {
                    throw new ExpressionException("赋值目标必须是 bean.属性 或 [...] 索引链");
                }
                Object rhs = evalExpr(a.right, roots);
                assign(a.left, rhs, roots);
                return rhs;
            }
            return evalExpr(stmt, roots);
        }

        private static boolean isAssignable(AstNode n) {
            if (n instanceof MemberNode) {
                MemberNode m = (MemberNode) n;
                if (m.args != null) {
                    return false;
                }
                return m.target instanceof IdentNode || isAssignable(m.target);
            }
            if (n instanceof IndexNode) {
                return isAssignable(((IndexNode) n).target);
            }
            return false;
        }

        private static void assign(AstNode lhs, Object value, Map<String, Object> roots) throws ExpressionException {
            if (lhs instanceof MemberNode) {
                MemberNode m = (MemberNode) lhs;
                Object parent = evalExpr(m.target, roots);
                writeProperty(parent, m.name, value);
                return;
            }
            if (lhs instanceof IndexNode) {
                IndexNode in = (IndexNode) lhs;
                Object parent = evalExpr(in.target, roots);
                Object key = evalExpr(in.index, roots);
                writeIndex(parent, key, value);
                return;
            }
            throw new ExpressionException("不支持的赋值目标");
        }

        private static Object evalExpr(AstNode n, Map<String, Object> roots) throws ExpressionException {
            if (n instanceof LiteralNode) {
                return ((LiteralNode) n).value;
            }
            if (n instanceof IdentNode) {
                String name = ((IdentNode) n).name;
                if (!roots.containsKey(name)) {
                    throw new ExpressionException("未知变量或 Bean: " + name);
                }
                return roots.get(name);
            }
            if (n instanceof UnaryNode) {
                UnaryNode u = (UnaryNode) n;
                Object v = evalExpr(u.expr, roots);
                if (u.op == Tk.BANG) {
                    return !truthy(v);
                }
                if (u.op == Tk.MINUS) {
                    return negate(v);
                }
                if (u.op == Tk.PLUS) {
                    return v;
                }
                throw new ExpressionException("未知一元运算符");
            }
            if (n instanceof BinaryNode) {
                BinaryNode b = (BinaryNode) n;
                if (b.op == Tk.AND_AND) {
                    Object l = evalExpr(b.left, roots);
                    if (!truthy(l)) {
                        return Boolean.FALSE;
                    }
                    return truthy(evalExpr(b.right, roots));
                }
                if (b.op == Tk.OR_OR) {
                    Object l = evalExpr(b.left, roots);
                    if (truthy(l)) {
                        return Boolean.TRUE;
                    }
                    return truthy(evalExpr(b.right, roots));
                }
                Object left = evalExpr(b.left, roots);
                Object right = evalExpr(b.right, roots);
                switch (b.op) {
                    case EQ_EQ:
                        return ObjectsCompat.equals(left, right);
                    case NE:
                        return !ObjectsCompat.equals(left, right);
                    case LT:
                        return compare(left, right) < 0;
                    case LE:
                        return compare(left, right) <= 0;
                    case GT:
                        return compare(left, right) > 0;
                    case GE:
                        return compare(left, right) >= 0;
                    case PLUS:
                        if (left instanceof String || right instanceof String) {
                            return String.valueOf(left) + String.valueOf(right);
                        }
                        return addNumbers(left, right);
                    case MINUS:
                        return subNumbers(left, right);
                    case STAR:
                        return mulNumbers(left, right);
                    case SLASH:
                        return divNumbers(left, right);
                    case PERCENT:
                        return modNumbers(left, right);
                    default:
                        throw new ExpressionException("未知二元运算符: " + b.op);
                }
            }
            if (n instanceof MemberNode) {
                MemberNode m = (MemberNode) n;
                Object target = evalExpr(m.target, roots);
                if (m.args == null) {
                    return readProperty(target, m.name);
                }
                List<Object> argVals = new ArrayList<Object>();
                for (AstNode arg : m.args) {
                    argVals.add(evalExpr(arg, roots));
                }
                return invokeMethod(target, m.name, argVals);
            }
            if (n instanceof IndexNode) {
                IndexNode in = (IndexNode) n;
                Object target = evalExpr(in.target, roots);
                Object key = evalExpr(in.index, roots);
                return readIndex(target, key);
            }
            if (n instanceof AssignNode) {
                throw new ExpressionException("此处不能使用赋值表达式");
            }
            throw new ExpressionException("无法求值节点: " + n.getClass().getSimpleName());
        }

        private static boolean truthy(Object v) {
            if (v == null) {
                return false;
            }
            if (v instanceof Boolean) {
                return (Boolean) v;
            }
            return true;
        }

        private static Object negate(Object v) throws ExpressionException {
            if (v instanceof Integer) {
                return -((Integer) v);
            }
            if (v instanceof Long) {
                return -((Long) v);
            }
            if (v instanceof Double) {
                return -((Double) v);
            }
            if (v instanceof Float) {
                return -((Float) v).doubleValue();
            }
            throw new ExpressionException("无法取负: " + v);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static int compare(Object a, Object b) throws ExpressionException {
            if (a == null || b == null) {
                throw new ExpressionException("无法比较 null");
            }
            if (a instanceof Comparable && a.getClass().isAssignableFrom(b.getClass())) {
                return ((Comparable) a).compareTo(b);
            }
            if (b instanceof Comparable && b.getClass().isAssignableFrom(a.getClass())) {
                return -((Comparable) b).compareTo(a);
            }
            if (a instanceof Number && b instanceof Number) {
                return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            }
            throw new ExpressionException("无法比较类型: " + a.getClass() + " 与 " + b.getClass());
        }

        private static Object addNumbers(Object a, Object b) throws ExpressionException {
            if (a instanceof Double || b instanceof Double) {
                return ((Number) a).doubleValue() + ((Number) b).doubleValue();
            }
            if (a instanceof Float || b instanceof Float) {
                return ((Number) a).doubleValue() + ((Number) b).doubleValue();
            }
            if (a instanceof Long || b instanceof Long) {
                return ((Number) a).longValue() + ((Number) b).longValue();
            }
            if (a instanceof Number && b instanceof Number) {
                return ((Number) a).intValue() + ((Number) b).intValue();
            }
            throw new ExpressionException("加法需要数字操作数");
        }

        private static Object subNumbers(Object a, Object b) throws ExpressionException {
            if (!(a instanceof Number) || !(b instanceof Number)) {
                throw new ExpressionException("减法需要数字操作数");
            }
            return ((Number) a).doubleValue() - ((Number) b).doubleValue();
        }

        private static Object mulNumbers(Object a, Object b) throws ExpressionException {
            if (!(a instanceof Number) || !(b instanceof Number)) {
                throw new ExpressionException("乘法需要数字操作数");
            }
            return ((Number) a).doubleValue() * ((Number) b).doubleValue();
        }

        private static Object divNumbers(Object a, Object b) throws ExpressionException {
            if (!(a instanceof Number) || !(b instanceof Number)) {
                throw new ExpressionException("除法需要数字操作数");
            }
            return ((Number) a).doubleValue() / ((Number) b).doubleValue();
        }

        private static Object modNumbers(Object a, Object b) throws ExpressionException {
            if (!(a instanceof Number) || !(b instanceof Number)) {
                throw new ExpressionException("取模需要数字操作数");
            }
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a) % ((Integer) b);
            }
            return ((Number) a).longValue() % ((Number) b).longValue();
        }

        private static Object readProperty(Object target, String name) throws ExpressionException {
            target = unwrapProxy(target);
            if (target == null) {
                throw new ExpressionException("对 null 取属性: " + name);
            }
            Class<?> c = target.getClass();
            String cap = capitalize(name);
            try {
                Method gm = findMethod(c, "get" + cap, EMPTY_TYPES);
                if (gm != null) {
                    gm.setAccessible(true);
                    return unwrapProxy(gm.invoke(target));
                }
            } catch (Exception ignored) {
            }
            try {
                Method im = findMethod(c, "is" + cap, EMPTY_TYPES);
                if (im != null && (im.getReturnType() == boolean.class || im.getReturnType() == Boolean.class)) {
                    im.setAccessible(true);
                    return unwrapProxy(im.invoke(target));
                }
            } catch (Exception ignored) {
            }
            try {
                Field f = findField(c, name);
                if (f != null) {
                    f.setAccessible(true);
                    return unwrapProxy(f.get(target));
                }
            } catch (Exception e) {
                throw new ExpressionException("读取属性失败: " + name + " — " + e.getMessage());
            }
            throw new ExpressionException("类型 " + c.getName() + " 上不存在属性: " + name);
        }

        private static void writeProperty(Object target, String name, Object value) throws ExpressionException {
            target = unwrapProxy(target);
            if (target == null) {
                throw new ExpressionException("对 null 赋值属性: " + name);
            }
            Class<?> c = target.getClass();
            String cap = capitalize(name);
            try {
                Method sm = findSetter(c, "set" + cap, value);
                if (sm != null) {
                    sm.setAccessible(true);
                    sm.invoke(target, coerce(sm.getParameterTypes()[0], value));
                    return;
                }
            } catch (ExpressionException e) {
                throw e;
            } catch (Exception ignored) {
            }
            try {
                Field f = findField(c, name);
                if (f != null) {
                    f.setAccessible(true);
                    f.set(target, coerce(f.getType(), value));
                    return;
                }
            } catch (Exception e) {
                throw new ExpressionException("写入属性失败: " + name + " — " + e.getMessage());
            }
            throw new ExpressionException("类型 " + c.getName() + " 上不存在可写属性: " + name);
        }

        private static Method findSetter(Class<?> c, String setterName, Object value) {
            Class<?> t = c;
            while (t != null && t != Object.class) {
                for (Method m : t.getDeclaredMethods()) {
                    if (m.getName().equals(setterName) && m.getParameterTypes().length == 1) {
                        return m;
                    }
                }
                t = t.getSuperclass();
            }
            return null;
        }

        private static Object readIndex(Object target, Object key) throws ExpressionException {
            target = unwrapProxy(target);
            if (target == null) {
                throw new ExpressionException("对 null 取下标");
            }
            if (target instanceof Map) {
                return unwrapProxy(((Map<?, ?>) target).get(key));
            }
            if (target instanceof List) {
                int i = asInt(key);
                List<?> list = (List<?>) target;
                if (i < 0 || i >= list.size()) {
                    throw new ExpressionException("List 索引越界: " + i);
                }
                return unwrapProxy(list.get(i));
            }
            if (target instanceof java.util.Collection) {
                int i = asInt(key);
                java.util.Collection<?> coll = (java.util.Collection<?>) target;
                if (i < 0 || i >= coll.size()) {
                    throw new ExpressionException("Collection 索引越界: " + i);
                }
                int j = 0;
                for (Object o : coll) {
                    if (j++ == i) {
                        return unwrapProxy(o);
                    }
                }
                throw new ExpressionException("Collection 索引越界: " + i);
            }
            if (target.getClass().isArray()) {
                int i = asInt(key);
                int len = Array.getLength(target);
                if (i < 0 || i >= len) {
                    throw new ExpressionException("数组索引越界: " + i);
                }
                return unwrapProxy(Array.get(target, i));
            }
            throw new ExpressionException("类型不支持 [] 访问: " + target.getClass().getName());
        }

        @SuppressWarnings("unchecked")
        private static void writeIndex(Object target, Object key, Object value) throws ExpressionException {
            target = unwrapProxy(target);
            if (target == null) {
                throw new ExpressionException("对 null 下标赋值");
            }
            if (target instanceof Map) {
                ((Map<Object, Object>) target).put(key, value);
                return;
            }
            if (target instanceof List) {
                int i = asInt(key);
                List<Object> list = (List<Object>) target;
                list.set(i, value);
                return;
            }
            if (target.getClass().isArray()) {
                int i = asInt(key);
                Array.set(target, i, value);
                return;
            }
            throw new ExpressionException("类型不支持 [] 赋值: " + target.getClass().getName());
        }

        private static int asInt(Object key) throws ExpressionException {
            if (key instanceof Integer) {
                return (Integer) key;
            }
            if (key instanceof Long) {
                return ((Long) key).intValue();
            }
            if (key instanceof Number) {
                return ((Number) key).intValue();
            }
            throw new ExpressionException("索引必须是整数，实际: " + key);
        }

        private static Object invokeMethod(Object target, String name, List<Object> argValues) throws ExpressionException {
            target = unwrapProxy(target);
            if (target == null) {
                throw new ExpressionException("对 null 调用方法: " + name);
            }
            Class<?> c = target.getClass();
            int n = argValues.size();
            List<Method> candidates = new ArrayList<Method>();
            Class<?> t = c;
            IdentityHashMap<Class<?>, Boolean> seen = new IdentityHashMap<Class<?>, Boolean>();
            while (t != null && t != Object.class) {
                if (seen.put(t, Boolean.TRUE) == null) {
                    for (Method m : t.getDeclaredMethods()) {
                        if (!m.getName().equals(name)) {
                            continue;
                        }
                        if (Modifier.isStatic(m.getModifiers())) {
                            continue;
                        }
                        Class<?>[] pt = m.getParameterTypes();
                        if (m.isVarArgs()) {
                            if (n >= pt.length - 1) {
                                candidates.add(m);
                            }
                        } else if (pt.length == n) {
                            candidates.add(m);
                        }
                    }
                }
                t = t.getSuperclass();
            }
            for (Method m : candidates) {
                try {
                    m.setAccessible(true);
                    Object[] args = convertArguments(m, argValues);
                    return unwrapProxy(m.invoke(target, args));
                } catch (IllegalArgumentException ignored) {
                } catch (ExpressionException e) {
                    throw e;
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    throw new ExpressionException("调用 " + name + " 失败: " + cause.getMessage());
                }
            }
            throw new ExpressionException("找不到匹配方法 " + name + "(" + n + " 参数) 于 " + c.getName());
        }

        private static Object[] convertArguments(Method m, List<Object> argValues) throws ExpressionException {
            Class<?>[] pt = m.getParameterTypes();
            if (m.isVarArgs()) {
                int fixed = pt.length - 1;
                Object[] out = new Object[pt.length];
                for (int i = 0; i < fixed; i++) {
                    out[i] = coerce(pt[i], argValues.get(i));
                }
                Class<?> varType = pt[fixed].getComponentType();
                int extra = argValues.size() - fixed;
                Object arr = Array.newInstance(varType, extra);
                for (int i = 0; i < extra; i++) {
                    Array.set(arr, i, coerce(varType, argValues.get(fixed + i)));
                }
                out[fixed] = arr;
                return out;
            }
            Object[] out = new Object[pt.length];
            for (int i = 0; i < pt.length; i++) {
                out[i] = coerce(pt[i], argValues.get(i));
            }
            return out;
        }

        private static Object coerce(Class<?> type, Object value) throws ExpressionException {
            if (value == null) {
                if (type.isPrimitive()) {
                    throw new ExpressionException("null 不能转为 " + type.getName());
                }
                return null;
            }
            if (type.isAssignableFrom(value.getClass())) {
                return value;
            }
            if (type == int.class || type == Integer.class) {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            }
            if (type == long.class || type == Long.class) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }
            if (type == double.class || type == Double.class) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            }
            if (type == float.class || type == Float.class) {
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
            }
            if (type == boolean.class || type == Boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                }
            }
            if (type == char.class || type == Character.class) {
                if (value instanceof Character) {
                    return value;
                }
                if (value instanceof String && ((String) value).length() == 1) {
                    return ((String) value).charAt(0);
                }
            }
            if (type == byte.class || type == Byte.class) {
                if (value instanceof Number) {
                    return ((Number) value).byteValue();
                }
            }
            if (type == short.class || type == Short.class) {
                if (value instanceof Number) {
                    return ((Number) value).shortValue();
                }
            }
            throw new ExpressionException("无法将 " + value.getClass().getName() + " 转为 " + type.getName());
        }

        private static String capitalize(String name) {
            if (name == null || name.isEmpty()) {
                return name;
            }
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        private static Method findMethod(Class<?> type, String name, Class<?>[] paramTypes) {
            Class<?> c = type;
            while (c != null && c != Object.class) {
                try {
                    return c.getDeclaredMethod(name, paramTypes);
                } catch (NoSuchMethodException ignored) {
                }
                c = c.getSuperclass();
            }
            return null;
        }

        private static Field findField(Class<?> type, String fieldName) {
            Class<?> c = type;
            while (c != null && c != Object.class) {
                try {
                    return c.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
                c = c.getSuperclass();
            }
            return null;
        }

        private static Object unwrapProxy(Object candidate) {
            Object current = candidate;
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<Object, Boolean>();
            while (current != null && visited.put(current, Boolean.TRUE) == null) {
                Object next = extractProxyTarget(current);
                if (next == null || next == current) {
                    return current;
                }
                current = next;
            }
            return current;
        }

        private static Object extractProxyTarget(Object candidate) {
            Object fromJdk = extractJdkProxyTarget(candidate);
            if (fromJdk != null && fromJdk != candidate) {
                return fromJdk;
            }
            Object fromCglib = extractCglibProxyTarget(candidate);
            if (fromCglib != null && fromCglib != candidate) {
                return fromCglib;
            }
            return candidate;
        }

        private static Object extractJdkProxyTarget(Object candidate) {
            if (candidate == null || !Proxy.isProxyClass(candidate.getClass())) {
                return null;
            }
            try {
                InvocationHandler handler = Proxy.getInvocationHandler(candidate);
                return extractTargetFromAdvised(handler);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static Object extractCglibProxyTarget(Object candidate) {
            if (candidate == null) {
                return null;
            }
            Class<?> type = candidate.getClass();
            while (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    if (!field.getName().startsWith("CGLIB$CALLBACK_")) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object callback = field.get(candidate);
                        Object target = extractTargetFromAdvised(callback);
                        if (target != null) {
                            return target;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                type = type.getSuperclass();
            }
            return null;
        }

        private static Object extractTargetFromAdvised(Object source) {
            if (source == null) {
                return null;
            }
            Object advised = getFieldValue(source, "advised");
            if (advised == null) {
                advised = getFieldValue(source, "advisedSupport");
            }
            if (advised == null) {
                return null;
            }
            try {
                Method getTargetSource = advised.getClass().getMethod("getTargetSource");
                Object targetSource = getTargetSource.invoke(advised);
                if (targetSource == null) {
                    return null;
                }
                Method getTarget = targetSource.getClass().getMethod("getTarget");
                return getTarget.invoke(targetSource);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static Object getFieldValue(Object source, String fieldName) {
            Field field = findField(source.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            try {
                field.setAccessible(true);
                return field.get(source);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static final class ObjectsCompat {
        static boolean equals(Object a, Object b) {
            if (a == null) {
                return b == null;
            }
            return a.equals(b);
        }
    }
}
