package dev.crater.utils.dictionary;

import dev.crater.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomCharsetDictionary extends Dictionary{
    private final char[] charset;
    private int length = 1;
    public CustomCharsetDictionary(List<String> args){
        this.charset = args.get(0).toCharArray();
        this.length = Integer.parseInt(args.get(1));
    }
    @Override
    public String next() {
        return randomStr(length);
    }

    @Override
    public String randomStr(int length) {
        return Dictionary.randomString(charset, length);
    }

    @Override
    public Dictionary copy() {
        return new CustomCharsetDictionary(Arrays.asList(new String(charset)));
    }
}
