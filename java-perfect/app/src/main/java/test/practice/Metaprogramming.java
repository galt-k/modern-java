package test.practice;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.invoke.*;
import java.util.Arrays;

public class Metaprogramming {
    
    static void basicReflection() {
        System.out.print("1. basic Reflection");

        Class<String> stringClass = String.class;

        // Get Class name, superclass, interfaces
        System.out.println("   Class name: " + stringClass.getName());
        System.out.println("   Superclass: " + stringClass.getSuperclass());
        System.out.println("   Interfaces: " + Arrays.toString(stringClass.getInterfaces()));

        // Fields
        System.out.println(" Fields:");
        for (Field f: stringClass.getDeclaredFields()) {
            System.out.println("  " + Modifier.toString(f.getModifiers()) + " " + f.getType());
        }

        //Methods
        System.out.println(" Methods (first 5):");
        Arrays.stream(stringClass.getMethods())
            .limit(5)
            .forEach(m -> System.out.println("  " + m));
        
        // Create instance and invoke method
        try {
            String str = stringClass.getConstructor(char[].class)
                                    .newInstance(new char[] {'h','i'});
            Method length = stringClass.getMethod("length");
            System.out.println("  Created string length: " + length.invoke(str)); 
        } catch (Exception e) {e.printStackTrace();}
        System.out.println();


    }
    // 2. Annotations- Metadata on code
    // Define custom annotations
    @Retention(RetentionPolicy.RUNTIME)
    @interface JsonField {
        String name() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface DeprecatedApi {
        String reason();
    }

    record User(@JsonField(name = "user_name") String name, int age) {}
    
    @DeprecatedApi(reason  = "Use v2")
    static class OldService {
        public void doWork() { System.out.println(" Old Service working"); }
    } 

    static void annotations() {
        System.out.println("2. Annotations");

        // Read annotation on record component
        for (Field field : User.class.getDeclaredFields()) {
            JsonField json = field.getAnnotation(JsonField.class);
            if (json != null) {
                System.out.println("  Field '" + field.getName() + "'' should serialize as '"+ json.name() + "'");
            }
        }

        //check deprecated class
        DeprecatedApi dep = OldService.class.getAnnotation(DeprecatedApi.class);
        if (dep != null) {
            System.out.println("  "+ OldService.class.getSimpleName() + " is deprecated: " + dep.reason() );
        }
        System.out.println();        
    }

    // 3. Dynamic Proxies - Intercept method calls
    interface Calculator {
        int add(int a, int b);
        int multiply(int a, int b); 
    }

    static class RealCalculator implements Calculator {
        public int add(int a, int b) { return a + b ;}
        public int multiply(int a, int b) {return a * b; }
    }

    static void dynamicProxies() {
        System.out.println("3. Dynamic Proxies ");
        Calculator real = new RealCalculator();

        Calculator proxy = (Calculator) Proxy.newProxyInstance(
                Calculator.class.getClassLoader(),
                new Class<?>[] { Calculator.class },
                (obj, method, args) -> {
                    System.out.println("   Proxy: Calling " + method.getName() + Arrays.toString(args));
                    long start = System.nanoTime();
                    Object result = method.invoke(real, args);
                    System.out.println("   Proxy: Result = " + result + " in " + (System.nanoTime() - start)/1000 + "µs");
                    return result;
                }
            );
        proxy.add(10, 20);
        proxy.multiply(5,6);
        System.out.println();
    }


    public static void main(String[] args) throws Exception {
        System.out.println("METAPROGRAMMING MASTERY 2025\n" + "=".repeat(60));

        basicReflection();
        annotations();
        dynamicProxies();
        
    }
}
