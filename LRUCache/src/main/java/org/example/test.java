package org.example;

public class test {
    public static void main(String[] args) {
        /*
        1. We have a list a transactions.
        2. Aggregate by User by category.
        3. Store it in a database
         */
    }

    public TransactionProcessing(List<Trasnaction> transactions){
        /*
        Operations implment on these transactions.
        Parallelize it
        How do we access the categorization service.

       Transaction
       - ID
       - userID
       - amount
       - business
       - category

        Categorization- DB credentials S2S,
        Background thread is running updaing the local cache(K-V)
         */

        try {
            ResultTransaction aggTransactions = transactions.stream().map(Amount::processing).map(categorization.get(transaction.busiess))
                    .groupingBy((trasaction.userID) -> List<transaction>)
                    .collect(
                            //supplier interface
                            TrasactionSuplier::new,
                            //accumlator
                            (t1, t2) -> {
                                //sum, mean, max, min
                                t1.sum += t2.sum,
                                t1.max = Math.max(t1.amount, t2.amount),
                            }
                            //combiner
                            ()
                            //result container


                    ).parallel(numberOfcores);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /
    *
            userID-> {Category: aggregatedCount}
    *
}
