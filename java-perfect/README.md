# Modern Java Mastery (2025 Edition)

A hands-on learning repository for mastering **modern Java (Java 21+)** in 2025. Designed for experienced developers transitioning from Python, Rust, or other languages who want to think and code like a senior Java engineer — fast.

Each file is a **self-contained, runnable "mastery playground"** — open in VS Code, click the green play button above `main()`, and instantly see concepts in action with clear output and explanations.

No boilerplate. No outdated tutorials. Just the features that matter **today**.

## Why This Repo Exists
Most Java learning resources are stuck in Java 8 or earlier. This repo focuses on **2025 Java**:
- Real modern idioms (`record`, `var`, sealed classes)
- Powerful Streams & Collectors (including internals)
- Project Loom / Virtual Threads (the concurrency revolution)
- Generics mastery (bounded types, wildcards, PECS)
- JVM performance & memory (escape analysis, GC tuning, object layout)

Rust/Python comparisons are included throughout to help map what you already know.

Perfect for interview prep, personal growth, or onboarding to modern Java.

## Topics Covered
1. **Java Idioms** – `var`, diamond operator, `record`, switch expressions, pattern matching `instanceof`, immutable collections
2. **Streams & Collectors** – Pipelines, `groupingBy`, `mapping`, custom collectors, internals (`CollectorImpl`)
3. **Project Loom / Virtual Threads** – Millions of threads, structured concurrency, scoped values
4. **Generics Mastery** – Bounded types, wildcards (PECS), generic methods, type erasure
5. **JVM Performance & Memory** – Escape analysis, GC tuning (ZGC/Shenandoah), object layout, JFR profiling

More coming: Advanced Pattern Matching, Modules, Testing, Spring Boot + Loom

## Project Structure
```text
src/main/java/test/practice/
├── ModernJavaIdioms.java
├── StreamsMastery.java
├── VirtualThreadsMastery.java
├── GenericsMastery.java
├── JVMPerformanceMastery.java
└── (more mastery files coming)
```

## How to Run
1. Clone the repo
2. Open in VS Code with **Extension Pack for Java** + **Gradle for Java**
3. Open any file in `src/main/java/test/practice/`
4. Click the **green play button** above `main()`
5. Or run: `./gradlew run` (after setting the main class if needed)

## Contributions Welcome
Found a better explanation? Want to add a new mastery file? PRs are very welcome!

Happy coding! 🚀