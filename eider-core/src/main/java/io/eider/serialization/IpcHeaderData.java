package io.eider.serialization;

public class IpcHeaderData
{
    private final int messageType;
    private final String from;

    public IpcHeaderData(final int messageType, final String from)
    {
        this.messageType = messageType;
        this.from = from;
    }

    public int getMessageType()
    {
        return messageType;
    }

    public String getFrom()
    {
        return from;
    }

    @Override
    public String toString()
    {
        return "IpcHeaderData{"
            +
            "messageType=" + messageType
            +
            ", from='" + from + '\''
            +
            '}';
    }
}
