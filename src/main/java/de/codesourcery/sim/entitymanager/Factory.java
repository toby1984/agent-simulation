package de.codesourcery.sim.entitymanager;

import de.codesourcery.sim.Vec2D;

public class Factory extends Building
{
    public final int cyclesTillProduction;
    public int cyclesLeft;

    public final Output output;
    public final Input[] inputs;

    public Factory(int id, BuildingType type, Vec2D location,int cyclesTillProduction,Output output,Input... inputs)
    {
        super(id, BuildingType.FACTORY, location);
        this.cyclesTillProduction = cyclesTillProduction;
        this.output = output;
        this.inputs = inputs == null ? new Input[0] : inputs;
    }

    public boolean canProduce()
    {
        if ( output.isFull() ) {
            return false;
        }
        for (int i1 = 0, inputsLength = inputs.length; i1 < inputsLength; i1++)
        {
            final Input i = inputs[i1];
            if ( i.lacksInput() )
            {
                return false;
            }
        }
        return true;
    }

    public boolean produce()
    {
        if ( canProduce() )
        {
            output.amountStored += output.amountProducedPerCycle;
            for (int i1 = 0, inputsLength = inputs.length; i1 < inputsLength; i1++)
            {
                final Input i = inputs[i1];
                i.amountStored -= i.amountConsumedPerCycle;
            }
            return true;
        }
        return false;
    }
}
