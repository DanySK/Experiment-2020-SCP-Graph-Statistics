package it.unibo.alchemist.model.implementations.conditions

import it.unibo.alchemist.model.interfaces.Context
import it.unibo.alchemist.model.interfaces.Dependency
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Neighborhood
import it.unibo.alchemist.model.interfaces.Node

/**
 * This condition is considered valid when there is a change in the neighborhood of a [Node]
 *
 * @param T concentration tyoe
 * @property environment
 * @constructor
 *
 * @param node
 * @param positionsDefineNeighborhoods if true (default) only movement of nodes will be considered
 * as source of neighborhoods modifications.
 */
class NeighborhoodChanged<T> @JvmOverloads constructor(
    val environment: Environment<T, *>,
    node: Node<T>,
    positionsDefineNeighborhoods: Boolean = true
) : ChangeDetectionCondition<T, Neighborhood<T>>(node) {

    init {
        declareDependencyOn(if (positionsDefineNeighborhoods) Dependency.MOVEMENT else Dependency.EVERYTHING)
    }

    override val currentValue: Neighborhood<T>?
        get() = environment.getNeighborhood(node)

    /**
     * @return The context for this condition.
     */
    override fun getContext() = Context.LOCAL
}