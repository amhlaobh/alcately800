import org.testng.annotations.Test;
import static org.junit.Assert.*;

public class StatsObjectTest {

    private final StatsObject compareObject = new StatsObject(1234,5678);
    private final String compareString = "1234:5678";
    private final StatsObject compareObject1 = new StatsObject(1,5);
    private final String compareString1 = "1:5";
    private final String compareString2 = ":";

    @Test
    public void toStringTest() {
        assertEquals(compareString, compareObject.toString());
    }

    @Test
    public void fromStringTest() {
        StatsObject obj = StatsObject.fromString(compareString);
        assertEquals(compareObject.timestamp, obj.timestamp);
        assertEquals(compareObject.usage, obj.usage);
    }

    @Test
    public void fromStringTest1() {
        StatsObject obj = StatsObject.fromString(compareString1);
        assertEquals(compareObject1.timestamp, obj.timestamp);
        assertEquals(compareObject1.usage, obj.usage);
    }

    @Test
    public void fromStringTest2() {
        StatsObject obj = StatsObject.fromString(compareString2);
        assertNull(obj);
    }
}
