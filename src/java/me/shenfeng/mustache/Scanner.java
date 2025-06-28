package me.shenfeng.mustache;

public class Scanner {
    public static final String TAGS = "#^>!/{&?";

    String template;
    int idx;

    public Scanner(String template) {
        this.template = template;
        idx = 0;
    }

    public boolean eos() { // end of string
        return idx == template.length() - 1;
    }
  //向前扫描直到遇到对应的模式
    public String scanUtil(String patten) {
      //从idx处遍历模板
        int pos = template.indexOf(patten, idx);
        if (pos == -1) {
          //并未找到对应的模式，将从idx处剩余字串切割出去
            String match = template.substring(idx);
            // idx回退一个位置
            idx = template.length() - 1;
            // 返回剩余的字串
            return match;
        } else if (pos == 0) {
          //正好在idx处发现模式，那么直接跳过模式的长度
            idx += patten.length();
            //返回null
            return null;
        } else {
          //剪切当前位置道模式前的子串
            String match = template.substring(idx, pos);
            // 将位置调整为模式后的第一个字符
            idx = pos + patten.length();
            //返回字串
            return match;
        }
    }
  //向前移动直到第一个非空，或者到字串结尾了
    public void skipeWhiteSpace() {
        while (Character.isWhitespace(next()) && !eos()) {
            ++idx;
        }
    }

    private char next() {
        return template.charAt(idx);
    }
  //直接回退count个字节
    public void pushBack(int count) {
        idx -= count;
    }
  //得到下个Char
    public char nextType() {
        skipeWhiteSpace();
        if (!eos()) {
          char c = next(); //下一个字符
          if (TAGS.indexOf(c) != -1) { //是否是TAGS
            idx += 1; //是TAGS，就继续
            if (c == Token.TRUE) {
              System.err.println("WARN: use nonstandard ?");
            }
            return c;
          } else {
            return Token.NAME;
          }
        }
        return 0;
    }
}
