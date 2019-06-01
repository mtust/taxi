package com.tustanovskyy.taxi;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBranchTest {

    @BeforeClass
    public void prepareBeforeTest(){
        boolean a = true;
        Assert.assertTrue("Verify conditions", a);
        Assert.assertTrue("Verify conditions false2", false);
    }

    @Test
    public  void verefySmth(){
        Assert.assertFalse("Verify", true);
    }

    @Test
    public  void verefySmth2(){
        Assert.assertFalse("Verify", true);
    }
}
