package de.codesourcery.sim;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

public class Message
{
    private static final AtomicLong MSG_ID = new AtomicLong();

    /**
     * Sorts ascending by priority (first message has highest priority)
     */
    public static final Comparator<Message> PRIO_COMPERATOR = (a,b) -> Integer.compare(b.priority,a.priority);

    public static final int LOW_PRIORITY = 0;
    public static final int MEDIUM_PRIORITY = 1;
    public static final int HIGH_PRIORITY = 2;

    public static final int DEFAULT_PRIORITY = MEDIUM_PRIORITY;

    public enum MessageKind
    {
        OFFER,
        REQUEST
    }

    public enum MessageType
    {
        ITEM_AVAILABLE(MessageKind.OFFER),
        ITEM_NEEDED(MessageKind.REQUEST);

        public final MessageKind kind;

        MessageType(MessageKind kind)
        {
            this.kind = kind;
        }
    }

    public final long id = MSG_ID.incrementAndGet();
    public final Entity sender;
    public final int priority;
    public final MessageType type;
    public final Object payload;

    public Message(Entity sender, MessageType type)
    {
        this.sender = sender;
        this.type = type;
        this.payload = null;
        this.priority = DEFAULT_PRIORITY;
    }

    public Message(Entity sender, MessageType type, Object payload)
    {
        this.sender = sender;
        this.type = type;
        this.payload = payload;
        this.priority = DEFAULT_PRIORITY;
    }

    public Message(Entity sender, MessageType type, Object payload,int priority)
    {
        this.sender = sender;
        this.type = type;
        this.payload = payload;
        this.priority = priority;
    }

    public final boolean hasType(MessageType t) {
        return this.type == t;
    }

    public ItemAndAmount getItemAndAmount() {
        return (ItemAndAmount) payload;
    }

    @Override
    public String toString()
    {
        return "Message{" +
                "id=" + id +
                ", prio=" + priority +
                ", type=" + type +
                ", payload=" + payload +
                ", sender=" + sender +
                '}';
    }
}