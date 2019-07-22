package io.eider.test.serialization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.eider.serialization.HeaderHelper;
import io.eider.serialization.IpcHeaderData;

public class HeaderTests
{
    @Test
    public void canWriteReadHeader()
    {
        final HeaderHelper underTest = new HeaderHelper();
        final byte[] toRead = underTest.writeIpcHeader("foo", 9);
        IpcHeaderData ipcHeaderData = underTest.readIpcHeader(toRead);

        Assertions.assertEquals(9, ipcHeaderData.getMessageType());
        Assertions.assertEquals("foo", ipcHeaderData.getFrom());
    }
}
