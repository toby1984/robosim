package de.codesourcery.robosim.render;

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
     * Sort the first N elements of an integer array.
     *
     * @param arr array to be sorted
     * @param sortLen number of elements in the array that should be sorted
     * @param c The Comparator<Integer> defining the custom sort order.
     */
    public static void sort(int[] arr, int sortLen, IntComparator c) {
        if (arr == null || sortLen < 2) {
            return;
        }
        quickSort(arr, 0, sortLen - 1, c);
    }

    private static void quickSort(int[] arr, int low, int high, IntComparator c) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high, c);
            quickSort(arr, low, pivotIndex - 1, c);
            quickSort(arr, pivotIndex + 1, high, c);
        }
    }

    private static int partition(int[] arr, int low, int high, IntComparator c) {
        int pivot = arr[high];
        int i = (low - 1);

        for (int j = low; j < high; j++) {
            if (c.compare(arr[j], pivot) < 0) {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, high);
        return i + 1;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}