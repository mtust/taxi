package com.tustanovskyy.taxi;

import org.junit.Assert;
import org.junit.BeforeClass;

public class TestBranchTest {

    @BeforeClass
    public void prepareBeforeTest(){
        boolean a = true;
        Assert.assertTrue("Verify conditions", a);
        Assert.assertTrue("Verify conditions false2", false);
        Assert.assertTrue("Verify conditions false3", false);
        Assert.assertTrue("Verify conditions false4", false);

    }
}
