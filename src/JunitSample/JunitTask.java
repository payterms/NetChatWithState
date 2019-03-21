package JunitSample;

import java.util.Arrays;

public class JunitTask {

    public int[] getPartOfArrayAfter4(int[] arr) throws RuntimeException {
        if (arr.length == 0) { throw new ArrayIndexOutOfBoundsException(); }

        int lastIndexOf4 = -1;// позиция последней 4-ки
        for (int i = 0; i < arr.length; i++) if (arr[i] == 4) lastIndexOf4 = i + 1;

        if (lastIndexOf4 == -1) throw new RuntimeException(); //Входной массив должен содержать хотя бы одну четверку, иначе в методе необходимо выбросить RuntimeException
        else return Arrays.copyOfRange(arr, lastIndexOf4, arr.length);
    }

    public boolean checkArrayFor1And4(int[] arr) {
        int counterOf1 = 0, counterOf4 = 0;
        for (int x : arr) {
            if (x == 1) counterOf1++;
            else if (x == 4) counterOf4++;
            else return false;
        }
        return ((counterOf1 > 0) && (counterOf4 > 0));
    }
}