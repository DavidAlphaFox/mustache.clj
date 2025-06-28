package me.shenfeng.mustache;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import clojure.lang.Keyword;

public class Mustache {

    public static final String BEGIN = "{{";
    public static final String END = "}}";

    List<Token> tokens;
  //内嵌缓存
    public static final ConcurrentMap<String, Mustache> CACHE = new ConcurrentHashMap<String, Mustache>();
  //在转义内部的内嵌模式
  //对section进行进一步的处理，得到更细致的的token列表
    private List<Token> nestedToken(List<Token> input) throws ParserException {
        List<Token> output = new ArrayList<Token>();
        Deque<Token> sections = new LinkedList<Token>(); // stack
        Token section;
        List<Token> collector = output; // 字串收集，最后的返回结果
        //遍历Token列表
        for (Token token : input) {
            switch (token.type) {
            case Token.INVERTED:
            case Token.TRUE:
            case Token.SECTION:
                token.tokens = new ArrayList<Token>();
                sections.add(token);
                collector.add(token);
                collector = token.tokens;
                break;
            case '/':
                if (sections.isEmpty()) {
                    throw new ParserException("Unopened section: " + token.value);
                }
                //得到最后一个Section
                section = sections.removeLast();
                //名字不同
                if (!section.value.equals(token.value)) {
                    throw new ParserException("Unclosed " + token.value + "; in section"
                            + section.value);
                }
                if (sections.size() > 0) {
                  //如果还有sessions没闭合
                  //那么就将内容收集器，设置为最后一个sections
                    collector = sections.peekLast().tokens;
                } else {
                    collector = output;
                }
                break;
            default:
              collector.add(token); //如果是普通字串，直接加入结果中
                break;
            }
        }
        if (sections.size() > 0) {
            throw new ParserException("Unclosed section: " + sections.peek().value);
        }
        //返回最后的结果
        return output;
    }

    public static Mustache preprocess(String template) throws ParserException {
        Mustache m = CACHE.get(template);
        if (m == null) {
            m = new Mustache(template);
            CACHE.put(template, m);
        }
        return m;
    }

    private Mustache(String template) throws ParserException {
        List<Token> tokens = new LinkedList<Token>();
        Scanner scanner = new Scanner(template);
        while (!scanner.eos()) {
          //去寻找mustache的开头字串
            String value = scanner.scanUtil(BEGIN);
            //截取字串有值
            if (value != null) {
              //将字串放入token表
              tokens.add(new Token(Token.TEXT, value));
            }
            //向下读取类型
            char type = scanner.nextType();
            //跳过所有空格
            scanner.skipeWhiteSpace();
            switch (type) {
              case '{': // not escape
                //是{{{，说明不是转义开头
                //向后扫描，直到发现}}}为止
                value = scanner.scanUtil("}}}");
                tokens.add(new Token(type, value));
                break;
              default:
                //否则向前寻找}}
                //把这个字串错位name保存起来
                value = scanner.scanUtil(END);
                tokens.add(new Token(type, value));
            }

        }
        //当所有的tokens都处理完成了，则对tokens内部进行处理
        this.tokens = nestedToken(tokens);
    }

    public String render(Context ctx) {
        try {
            return Token.renderTokens(tokens, ctx, null);
        } catch (ParserException e) {
            return ""; // can not happen
        }
    }

    public String render(Context ctx, Map<Keyword, String> partials) throws ParserException {
        return Token.renderTokens(tokens, ctx, partials);
    }
}
