package de.codesourcery.robosim.render;

import java.util.Comparator;

/**
 * A custom QuickSort implementation for primitive int[] arrays that uses a
 * Comparator<Integer> to define the sorting order, avoiding full array boxing.
 * This is crucial for performance-sensitive applications like real-time graphics.
 */
public class IntegerQuicksort {

    public interface IntComparator {
        int compare(int a, int b);
    }

    /**
     * Public entry point to sort the entire array.
     * * @param arr The primitive int array to be sorted.
     * @param c The Comparator<Integer> defining the custom sort order.
     */
    public static void sort(int[] arr, int len, IntComparator c) {
        if (arr == null || len < 2) {
            return;
        }
        quickSort(arr, 0, len - 1, c);
    }

    // --- Private Recursive QuickSort Implementation ---

    private static void quickSort(int[] arr, int low, int high, IntComparator c) {
        if (low < high) {
            // Find the pivot element such that elements smaller than the pivot
            // are on the left and elements greater are on the right.
            int pivotIndex = partition(arr, low, high, c);

            // Recursively sort the subarrays
            quickSort(arr, low, pivotIndex - 1, c);
            quickSort(arr, pivotIndex + 1, high, c);
        }
    }

    /**
     * Partition method for QuickSort. It selects a pivot, moves elements
     * smaller than the pivot to the left, and elements greater to the right.
     * * @return The final index of the pivot element.
     */
    private static int partition(int[] arr, int low, int high, IntComparator c) {
        // We use the element at the 'high' index as the pivot
        int pivot = arr[high];
        int i = (low - 1); // Index of smaller element

        for (int j = low; j < high; j++) {
            // The Comparator's compare method is used here.
            // We temporarily cast the primitive ints to Integer (boxing)
            // only for this single comparison, which is much less overhead
            // than boxing the whole array.

            // Check if current element arr[j] is "less than" the pivot based on the Comparator.
            // A result < 0 means arr[j] is ordered before the pivot.
            if (c.compare(arr[j], pivot) < 0) {
                i++;
                // Swap arr[i] and arr[j]
                swap(arr, i, j);
            }
        }

        // Swap the pivot element with the element at i + 1
        swap(arr, i + 1, high);

        return i + 1;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}