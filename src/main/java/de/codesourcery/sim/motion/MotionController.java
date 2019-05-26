package de.codesourcery.sim.motion;

public class MotionController
{
    private static final int MAX_ENTITIES = 10000;
    private static final int STRUCT_SIZE = 5;

    private float[] entityData = new float[MAX_ENTITIES * STRUCT_SIZE];

    private int size;

    private static final int OFFSET_X = 0;
    private static final int OFFSET_Y = 1;
    private static final int OFFSET_AX = 2;
    private static final int OFFSET_AY = 3;
    private static final int OFFSET_VX = 4;
    private static final int OFFSET_VY = 5;
    private static final int OFFSET_MAX_SPEED = 6;

    public int registerEntity(float x, float y, float ax, float ay, float vx, float vy, float maxSpeed)
    {
        if (size == entityData.length)
        {
            final int len = entityData.length / STRUCT_SIZE;
            float[] tmp = new float[(1 + len + len / 2) * STRUCT_SIZE];
            System.arraycopy(entityData, 0, tmp, 0, entityData.length);
            entityData = tmp;
        }
        final int ptr = size;
        entityData[ptr + OFFSET_X] = x;
        entityData[ptr + OFFSET_Y] = y;
        entityData[ptr + OFFSET_AX] = ax;
        entityData[ptr + OFFSET_AY] = ay;
        entityData[ptr + OFFSET_VX] = vx;
        entityData[ptr + OFFSET_VY] = vy;
        entityData[ptr + OFFSET_MAX_SPEED] = maxSpeed;
        int id = size;
        size += STRUCT_SIZE;
        return id;
    }

    public void removeEntity(int id)
    {
        // 0|1|2
        final int dstOffset = id * STRUCT_SIZE;
        final int srcOffset = dstOffset + STRUCT_SIZE;
        final int floatsToCopy = (size - id) * STRUCT_SIZE;
        System.arraycopy(entityData, srcOffset, entityData, dstOffset, floatsToCopy);
    }

    public void setAcceleration(int id,float ax,float ay)
    {
        final int ptr = id * STRUCT_SIZE;
        entityData[ptr + OFFSET_AX] = ax;
        entityData[ptr + OFFSET_AY] = ay;
    }

    public void tick(float deltaSeconds)
    {
        // v = a * t^2
        // s = v*t
        final float seconds2 = deltaSeconds*deltaSeconds;
        final float[] array = entityData;
        for (int i = 0, ptr = 0 , len = size; i < len; i++, ptr += STRUCT_SIZE)
        {
            float x  = array[ptr + OFFSET_X];
            float y  = array[ptr + OFFSET_Y];
            final float ax = array[ptr + OFFSET_AX];
            final float ay = array[ptr + OFFSET_AY];
            float vx = array[ptr + OFFSET_VX];
            float vy = array[ptr + OFFSET_VY];
            final float maxSpeed = array[ptr + OFFSET_MAX_SPEED];

            // clamp speed
            final float maxSpeed2 = maxSpeed * maxSpeed;

            vx += ax * seconds2;
            vy += ay * seconds2;

            final float vLen2 = vx * vx + vy * vy;
            if (vLen2 > maxSpeed2)
            {
                float factor = (float) Math.sqrt(maxSpeed2 / vLen2);
                vx *= factor;
                vy *= factor;
            }

            x += vx * deltaSeconds;
            y += vy * deltaSeconds;

            array[ ptr + OFFSET_X]  =  x;
            array[ ptr + OFFSET_Y]  =  y;
            array[ ptr + OFFSET_VX] = vx;
            array[ ptr + OFFSET_VY] = vy;
        }

    }
}