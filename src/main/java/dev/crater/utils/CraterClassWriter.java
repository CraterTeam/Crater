package dev.crater.utils;

import dev.crater.Crater;
import dev.crater.Main;
import lombok.SneakyThrows;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import dev.crater.utils.jar.ClassWrapper;

public class CraterClassWriter extends ClassWriter {
    public CraterClassWriter(int flags) {
        super(flags);
    }

    public CraterClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        Crater crater = Main.INSTANCE;
        if ("java/lang/Object".equals(type1) || "java/lang/Object".equals(type2))
            return "java/lang/Object";

        String first = deriveCommonSuperName(type1, type2,crater);
        String second = deriveCommonSuperName(type2, type1,crater);
        if (!"java/lang/Object".equals(first))
            return first;

        if (!"java/lang/Object".equals(second))
            return second;

        return getCommonSuperClass(crater.getClasspathWrapper(type1).getSuperName(), crater.getClasspathWrapper(type2).getSuperName());
    }

    @SneakyThrows
    private String deriveCommonSuperName(final String type1, final String type2, Crater crater) {
        ClassWrapper first = crater.getClasspathWrapper(type1);
        ClassWrapper second = crater.getClasspathWrapper(type2);
        if (crater.isAssignableFrom(type1, type2))
            return type1;
        else if (crater.isAssignableFrom(type2, type1))
            return type2;
        else if (first.isInterface() || second.isInterface())
            return "java/lang/Object";
        else {
            String temp;

            do {
                temp = first.getSuperName();
                first = crater.getClasspathWrapper(temp);
            } while (!crater.isAssignableFrom(temp, type2));
            return temp;
        }
    }
}
