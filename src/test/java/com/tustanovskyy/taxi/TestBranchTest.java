package com.tustanovskyy.taxi;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBranchTest {

    @BeforeClass
    public void prepareBeforeTest(){
        boolean as = true;
        Assert.assertTrue("Verify conditions", as);
        Assert.assertTrue("Verify conditions false2", false);
        Assert.assertTrue("Verify conditions false3", false);
        Assert.assertTrue("Verify conditions false4", false);

    }

    @Test
    public  void verefySmth1234(){
        Assert.assertFalse("Verify", true);
    }

    @Test
    public  void verefySmth2(){
        Assert.assertFalse("Verify", true);
    }
}
