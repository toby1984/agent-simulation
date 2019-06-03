package de.codesourcery.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TaskTreeNode
{
    public TaskTreeNode parent;
    public final List<TaskTreeNode> children = new ArrayList<>();

    public static final class TaskNode extends TaskTreeNode
    {
        public final Task task;

        public TaskNode(Task task)
        {
            this.task = task;
        }
    }

    public static final class Sequence extends TaskTreeNode
    {
    }

    public static final class Parallel extends TaskTreeNode
    {
    }

    public final TaskTreeNode root()
    {
        if ( parent != null ) {
            return parent.root();
        }
        return this;
    }

    public void add(TaskTreeNode child)
    {
        this.children.add( child );
        child.parent = this;
    }

    public void visitInOrder(Consumer<TaskTreeNode> visitor) {
        for ( TaskTreeNode child : children ) {
            child.visitInOrder( visitor );
        }
        visitor.accept( this );
    }
}