package dev.crater.utils.dictionary;

import dev.crater.Crater;
import dev.crater.Main;

import java.util.ArrayList;
import java.util.List;

public class CustomDictionary extends Dictionary{
    private List<String> used = new ArrayList<>();
    public CustomDictionary() {
        super("Custom");
    }

    @Override
    public String getWord(WordType type) {
        Crater crater = Main.INSTANCE;
        String word = null;
        while (word == null || used.contains(word)) {
            word = Dictionary.createRandomString((Integer) crater.getConfig().get("Name.dictionary.length"), (String) crater.getConfig().get("Name.dictionary.word"));
        }
        return word;
    }
}
