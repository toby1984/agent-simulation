package de.codesourcery.sim;

import java.util.concurrent.atomic.AtomicLong;

public class Message
{
    private static final AtomicLong MSG_ID = new AtomicLong();

    public enum MessageType {
        ITEM_AVAILABLE,
        ITEM_NEEDED,
        PICKING_UP,
        DROPPING_OFF
    }

    public final long id = MSG_ID.incrementAndGet();
    public final Entity sender;
    public final MessageType type;
    public final Object payload;
    public final Message replyTo;

    public Message(Entity sender, MessageType type)
    {
        this.sender = sender;
        this.type = type;
        this.payload = null;
        this.replyTo = null;
    }

    public Message(Entity sender, MessageType type, Object payload)
    {
        this.sender = sender;
        this.type = type;
        this.payload = payload;
        this.replyTo = null;
    }

    public Message(Entity sender, MessageType type, Object payload, Message replyTo)
    {
        this.sender = sender;
        this.type = type;
        this.payload = payload;
        this.replyTo = replyTo;
    }

    public Message createReply(Entity sender, MessageType type, Object payload)
    {
        return new Message(sender,type,payload,this);
    }

    public final boolean hasType(MessageType t) {
        return this.type == t;
    }

    public ItemAndAmount getItemAndAmount() {
        return (ItemAndAmount) payload;
    }

    public boolean isReplyTo(Message other) {
        return this.replyTo != null && this.replyTo == other;
    }

    @Override
    public String toString()
    {
        return "Message{" +
                "id=" + id +
                ", type=" + type +
                ", payload=" + payload +
                ", sender=" + sender +
                ", replyTo=" + replyTo +
                '}';
    }
}