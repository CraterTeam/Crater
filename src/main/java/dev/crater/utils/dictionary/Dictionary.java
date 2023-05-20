package dev.crater.utils.dictionary;

import dev.crater.Main;
import dev.crater.utils.RandomUtils;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Dictionary {
    @Getter
    private static Map<String,Class> dictionaries = new HashMap<>();
    public abstract String next();

    public abstract String randomStr(int length);

    public abstract Dictionary copy();

    // https://en.wikipedia.org/wiki/Bijective_numeration
    static String toBijectiveBase(char[] charset, int decimal) {
        StringBuilder sb = new StringBuilder();
        while (decimal-- > 0) {
            sb.insert(0, charset[decimal % charset.length]);
            decimal /= charset.length;
        }
        return sb.toString();
    }

    public static String randomString(char[] charset, int length) {
        int charsetLength = charset.length;
        char[] buf = new char[length];

        for (int i = 0; i < length; i++) {
            buf[i] = charset[RandomUtils.randomInt(charsetLength)];
        }

        return new String(buf);
    }
    public static Dictionary getDictionary(String type){
        Class<? extends Dictionary> dictionaryClass = dictionaries.get(Main.INSTANCE.getConfig().get("Name.dictionary."+type+".name"));
        List<String> buildArgs = (List<String>) Main.INSTANCE.getConfig().get("Name.dictionary."+type+".args");
        try {
            return buildDictionary(dictionaryClass,buildArgs);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static Dictionary buildDictionary(Class<? extends Dictionary> clazz, List<String> args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return clazz.getConstructor(List.class).newInstance(args);
    }
    static {
        dictionaries.put("CustomCharset",CustomCharsetDictionary.class);
    }

}
