package memory;

public class Testing {
    public static void main(String[] args) {
        int[] addresses = new int[]{6677,
                50704,
                51883,
                16916,
                62493,
                30198,
                53683,
                40185,
                28781,
                24462
        };

        for (int a : addresses) {
            System.out.println(a + ": " + MemoryManager.getPageNumber(a) + "  ---  " + MemoryManager.getPageOffset(a));

        }
    }
}
