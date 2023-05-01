package dev.crater;

public class NumberTest {
    private static int a = 1;
    private static double b = 2.2;
    private static float c = 3.3f;
    private static long d = 4L;
    private static short e = 5;
    public static void main() {
        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println(d);
        System.out.println(e);
        System.out.println(124);
        System.out.println(249239432);
        System.out.println(3f);
        System.out.println(3.3);
        int a = 2000;
        System.out.println(a >> 1);
        System.out.println(a >>> 1);
        System.out.println(a << 1);
        System.out.println(a << 1);
        int b = 5;
        System.out.println(~b);
        System.out.println(b^1);
        System.out.println(b + b);
        System.out.println(b * 2);
        System.out.println((-b) + (3*(b)));
        System.out.println(b * 2 + 1d);
    }
}
