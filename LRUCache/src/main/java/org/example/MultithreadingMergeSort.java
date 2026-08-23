package org.example;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MultithreadingMergeSort {

    public static void main(String[] args) {
        int[] arr = {38, 27, 43, 3, 9, 82, 10, 19, 5};

        System.out.println("Original Array: " + Arrays.toString(arr));

        mergeSort(arr, 0, arr.length - 1);

        System.out.println("Sorted Array:   " + Arrays.toString(arr));
    }
    public static void mergeSort(int[] arr, int start, int end) {
        /*
        Create the start and end index.
        Calculate the mid point
        assign the worker threads for the sort the halves
        Do the merge operation.
         */
        if(start >= end){
            return;
        }

        int mid = start + (end-start)/2;

        //assign the left part to the a worker
        mergeSort(arr, start, mid);
        //assign the right part to the another worker
        mergeSort(arr, mid+1, end);

        //merge the tow haves
        mergeTwoSortedArrays(start, mid, end, arr);

    }
    public static void customParallelMergeSort(int[] arr, int start, int end) {
        if(arr== null || arr.length <= 1){
            return;
        }
        InPlaceParallelMergeSortTask.sort(arr);
    }

    public static void mergeTwoSortedArrays(int start, int mid, int end, int[] arr) {
        /*
        start the left and right indexes with 0.
        What is the decrease and conquer approach?
        I will solve just the single part of the problem and give the rest o the others in the chain
        -check, which is lesser and put that in the result
        -update the subproblems and pass it to the next employee
         */
        int arrSize = end - start + 1;
        int[] result = new int[arrSize];
        int left = start;
        int right = mid+1;
        int index = 0;

        // do this until left and right both are with the indexes
        while(left <= mid && right <= end) {
            if(arr[left] <= arr[right]){
                result[index] = arr[left];
                left++;
            }else{
                result[index] = arr[right];
                right++;
            }

            index++;
        }
            //which means add all the elements in the arr2 to the result
        while(right<=end) {
            result[index] = arr[right];
            right++;
            index++;
        }

        while(left <= mid) {
            result[index] = arr[left];
            left++;
            index++;
        }

        System.arraycopy(result, 0, arr, start, arrSize);



    }

    public static int binarySearch(int key, int[] arr, int low, int high) {
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= key) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }




}


class InPlaceParallelMergeSortTask extends RecursiveAction {
    // Cutoff threshold to switch to sequential sort to avoid subtask creation overhead
    private static final int THRESHOLD = 8192;
    private final int[] array;
    private final int[] tempBuffer; // Added temporary scratchpad buffer
    private final int start;
    private final int end;
    public InPlaceParallelMergeSortTask(int[] array,int[] tempBuffer , int start, int end) {
        this.array = array;
        this.tempBuffer = tempBuffer;
        this.start = start;
        this.end = end;
    }

    @Override
    protected void compute() {
        if(start >= end){
            return;
        }
        // 1. SEQUENTIAL FALLBACK (Avoids task allocation on small sub-arrays)
        if ((end - start + 1) <= THRESHOLD) {
            MultithreadingMergeSort.mergeSort(array, start, end);
            return;
        }

        int mid = start + (end-start)/2;

        //Divide: Instantiate subtasks on exact array bounds
        InPlaceParallelMergeSortTask leftTask = new InPlaceParallelMergeSortTask(array,tempBuffer, start, mid);
        InPlaceParallelMergeSortTask rightTask = new InPlaceParallelMergeSortTask(array,tempBuffer, mid+1, end);

        //Work Stealing FORK and Join
        invokeAll(leftTask, rightTask);

        // COMBINE :
        // Merge the two sorted arrays.
        //MultithreadingMergeSort.mergeTwoSortedArrays(start, mid, end, array);

        // ✅ FIX 1: Trigger the ParallelMergeTask via median partitioning
        ParallelMergeTask mergeTask = new ParallelMergeTask(
                array, start, mid,
                array, mid + 1, end,
                tempBuffer, start
        );
        mergeTask.invoke();
        System.arraycopy(tempBuffer, start, array, start, end - start + 1);
    }

    public static void sort(int[] arr){
        int[] tempBuffer = new int[arr.length];
        //dedicated pool ensures we dont saturate the shared common pool
        ForkJoinPool pool = new ForkJoinPool();
        try{
            pool.invoke(new InPlaceParallelMergeSortTask(arr,tempBuffer, 0, arr.length-1));
        } finally {
            pool.shutdown();
        }
    }
}

class ParallelMergeTask extends RecursiveAction {

    private static final int MERGE_THRESHOLD = 2048;

    private final int[] arr1;
    private final int p1,r1;
    private final int[] arr2;
    private final int p2,r2;
    private final int[] dest;
    private final int p3;

    public ParallelMergeTask(int[] arr1, int p1, int r1,
                             int[] arr2, int p2, int r2,
                             int[] dest, int p3){
        this.arr1 = arr1;
        this.p1 = p1;
        this.r1 = r1;
        this.arr2 = arr2;
        this.p2 = p2;
        this.r2 = r2;
        this.dest = dest;
        this.p3 = p3;
    }

    @Override
    protected void compute() {
        int len1 = r1-p1 + 1;
        int len2 = r2-p2 + 1;

        // Ensure sub-array 1 is always the longer array for optimal median split
        if(len1 < len2){
            ParallelMergeTask swap = new ParallelMergeTask(arr2, p2, r2, arr1, p1, r1, dest, p3);
            swap.compute();
            return;
        }

        if(len1 <= 0){
            return;
        }

        //Sequential merge
        if((len1 + len2) <= MERGE_THRESHOLD) {
            sequentialMerge(arr1, p1, r1, arr2, p2, r2, dest, p3);
            return;
        }

        //1. Pick the Median of the larger sub array
        int mid1 = p1 + (r1 - p1)/2;
        int pivot = arr1[mid1];

        //2. Binary Search for pivot position
        int mid2 = MultithreadingMergeSort.binarySearch(pivot, arr2, p2, r2);

        //3. Compute destination index for pivot
        int destOffSet = p3 + (mid1-p1) + (mid2-p2);
        dest[destOffSet] = pivot;

        //Fork left and right paralle merges
        ParallelMergeTask leftMerge = new ParallelMergeTask(
                arr1, p1, mid1-1,
                arr2, p2, mid2-1,
                dest, p3
        );

        ParallelMergeTask rightMerge = new ParallelMergeTask(
                arr1, mid1+1, r1,
                arr2, mid2, r2,
                dest, destOffSet+1
        );

        invokeAll(leftMerge, rightMerge);

    }

    private static void sequentialMerge(int[] a1, int p1, int r1,
                                        int[] a2, int p2, int r2,
                                        int[] dest, int p3) {
        int i = p1, j = p2, k = p3;
        while (i <= r1 && j <= r2) {
            if (a1[i] <= a2[j]) dest[k++] = a1[i++];
            else dest[k++] = a2[j++];
        }
        while (i <= r1) dest[k++] = a1[i++];
        while (j <= r2) dest[k++] = a2[j++];
    }
}
