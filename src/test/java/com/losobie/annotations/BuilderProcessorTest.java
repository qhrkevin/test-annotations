package com.losobie.annotations;

import org.junit.Assert;
import org.junit.Test;

public class BuilderProcessorTest {

    @Test
    public void testProcess() {
        TestObject object = new TestObjectBuilder()
                   .setId( 1 )
                   .setName( "Test" )
                   .build();
        Assert.assertEquals( 1, object.getId() );
        Assert.assertEquals( "Test", object.getName() );
    }

}
