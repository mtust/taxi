package com.tustanovskyy.taxi;

import org.junit.Assert;
import org.junit.BeforeClass;

public class TestBranchTest {

    @BeforeClass
    public void prepareBeforeTest(){
        boolean a = true;
        boolean b = true;
        Assert.assertTrue("Verify conditions", a);
        Assert.assertFalse("Verify conditions", b);
    }
}
