package de.codesourcery.sim.entitymanager;

public interface IEntityTickListener<T extends IEntity>
{
    void tick(EntityManager entityManager, T entity, float deltaSeconds);
}
