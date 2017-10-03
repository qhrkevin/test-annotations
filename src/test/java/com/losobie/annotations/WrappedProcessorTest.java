package com.losobie.annotations;

import com.losobie.wrappers.ConnectionWrapper;
import org.junit.Test;
import java.sql.Connection;
import java.sql.SQLException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WrappedProcessorTest {

    @Wrapped
    private final Connection delegate;
    private final Connection connectionMock;

    public WrappedProcessorTest() {
        connectionMock = mock( Connection.class );
        delegate = new ConnectionWrapper( connectionMock );
    }

    @Test
    public void testProcess() throws SQLException {
        delegate.close();
        verify( connectionMock ).close();
    }
}
