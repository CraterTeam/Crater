package dev.crater.utils.dictionary;

import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Dictionary {
    @Getter
    private String name;
    private final static List<Dictionary> dictionaries = new ArrayList<>();
    public Dictionary(String Name){
        this.name = Name;
    }
    public abstract String getWord(WordType type);
    public static List<Dictionary> getDictionaries(){
        return dictionaries;
    }
    public static String createRandomString(int length, String str){
        Random random = new Random();
        StringBuffer stringBuffer = new StringBuffer();
        String[] strArr = str.split("");
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(strArr.length);
            stringBuffer.append(strArr[number]);
        }
        return stringBuffer.toString();
    }
    static {
        dictionaries.add(new CustomDictionary());
    }

}
