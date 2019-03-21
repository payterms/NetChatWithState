package JunitSample;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class Test11 {
    private JunitTask task;

    private int[] arr;
    private int[] res;

    @Parameters
    public static Collection params() {
        return Arrays.asList(
                new Object[][]{
                        {new int[] {1, 2, 2, 3, 7}, new int[] {1, 7}},
                        {new int[] {1, 9, 2, 3,  26, 7, }, new int[] {9}},
                        {new int[] {1, -4, 8, 2, 3}, new int[] {1, -4, 8, 2, 3}},
                        {new int[] {-88, 8, 2, 3}, new int[] {}}
                }
        );
    }

    public Test11(int[] arr, int[] res) {
        this.arr = arr;
        this.res = res;
    }

    @Before
    public void init() {
        task = new JunitTask();
    }

    @After
    public void tearDown() throws Exception { task = null; }

    @Test(expected = RuntimeException.class)
    public void testGetPartOfArrayWithParams() { Assert.assertArrayEquals(res, task.getPartOfArrayAfter4(arr)); }

}
