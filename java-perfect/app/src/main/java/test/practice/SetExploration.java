package test.practice;

import java.util.TreeSet;
import java.util.SortedSet;

public class SetExploration {
    /*
    Implementation Classes
    1. AbstractSet
    2. ConcurrentHashMap
    3. ConcurrentSkipListSet
    4. CopyOnWriteArraySet
    5. EnumSet
    6. LinkedHashSet
    7. TreeSet
    8. hashSet
    9. JobStateReasons
     */
    // SortedSet Interface
    // All the elements inserted into a sorted set must implement the Comparable Interface or be accepted by the specified comprator
    // Returns the Comparator used to order the elements
    // Create a SortedSet of Integers
    SortedSet<Integer> leaderboard = new TreeSet<>();
    // Add scores in random order
    leaderboard.add(450);
    leaderboard.add(1200);
    leaderboard.add(150);
    leaderboard.add(850);
    leaderboard.add(150);

    System.out.println("Leaderboard: ");


}
