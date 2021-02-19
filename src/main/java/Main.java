import net.intelie.introspective.BloomObjectSizer;
import net.intelie.introspective.ObjectSizer;
import net.intelie.introspective.reflect.ReflectionCache;
import net.intelie.introspective.util.BloomVisitedSet;
import net.intelie.introspective.util.ExpiringVisitedSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        testSmallObject();
        testSmallObjectBfs();
        testLargeObject();
        testLargeObjectBfs();
    }

    public static void testSmallObject() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 15);
        ObjectSizer sizer = new ObjectSizer(new ReflectionCache(), set, 1 << 15);
        Map test = new HashMap();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        testSizer("small", sizer, test, 10000000);
    }

    public static void testSmallObjectBfs() {
        BloomObjectSizer sizer = new BloomObjectSizer(new ReflectionCache(), 1 << 10, 1 << 15, 1 << 15);
        Map test = new HashMap();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        testBfsSizer("small.bfs", sizer, test, 10000000);
    }

    public static void testLargeObject() {
        ObjectSizer sizer = new ObjectSizer(new ReflectionCache(), new BloomVisitedSet(1 << 24, 3), 1 << 15);

        Object[] objs = IntStream.range(0, 10000).mapToObj(x -> {
            Map test = new HashMap();
            test.put(111 + x * 10000, Arrays.asList("aaa" + x, 222 + x * 10000));
            test.put(333.0 + x * 10000, Collections.singletonMap("bbb" + x, 444 + x * 10000));
            return test;
        }).toArray(Object[]::new);

        testSizer("large", sizer, objs, 1000);
    }

    public static void testLargeObjectBfs() {
        BloomObjectSizer sizer = new BloomObjectSizer(new ReflectionCache(), 1 << 20, 1 << 15, 1 << 15);

        Object[] objs = IntStream.range(0, 10000).mapToObj(x -> {
            Map test = new HashMap();
            test.put(111 + x * 10000, Arrays.asList("aaa" + x, 222 + x * 10000));
            test.put(333.0 + x * 10000, Collections.singletonMap("bbb" + x, 444 + x * 10000));
            return test;
        }).toArray(Object[]::new);

        testBfsSizer("large.bfs", sizer, objs, 1000);

    }

    private static void testSizer(String testName, ObjectSizer sizer, Object test, int measureCount) {
        //warmup
        for (int i = 0; i < measureCount / 100; i++) {
            sizer.resetTo(test);
            while (sizer.moveNext()) ;
        }

        long start = System.nanoTime();
        long total = 0;
        for (int i = 0; i < measureCount; i++) {
            sizer.resetTo(test);
            while (sizer.moveNext()) total += 1;
        }
        long end = System.nanoTime();
        System.out.println(System.getProperty("java.version") + "\t" + testName + "\t" + (end - start) / 1e9 + "\t" + (long) (total * 1e9 / (end - start)));
    }

    private static void testBfsSizer(String testName, BloomObjectSizer sizer, Object test, int measureCount) {
        //warmup
        for (int i = 0; i < measureCount / 10; i++) {
            sizer.softClear();
            sizer.visit(test);
        }

        long start = System.nanoTime();
        long total = 0;
        for (int i = 0; i < measureCount; i++) {
            sizer.softClear();
            sizer.visit(test);
            total += sizer.count();
        }
        long end = System.nanoTime();
        System.out.println(System.getProperty("java.version") + "\t" + testName + "\t" + (end - start) / 1e9 + "\t" + (long) (total * 1e9 / (end - start)));
    }
}
