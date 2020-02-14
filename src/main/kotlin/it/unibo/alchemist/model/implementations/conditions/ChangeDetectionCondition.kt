package it.unibo.alchemist.model.implementations.conditions

import it.unibo.alchemist.model.interfaces.Node

abstract class ChangeDetectionCondition<T, X> @JvmOverloads constructor(
    node: Node<T>
) : AbstractCondition<T>(node) {

    private var hasFlipped: Boolean = false
    private var previousValue: X? = null

    abstract val currentValue: X?
    protected open val propensityContributionWhenValid: Double = 1.0

    override final fun getPropensityContribution(): Double {
        if (!hasFlipped && currentValue != previousValue) {
            hasFlipped = true
            previousValue = currentValue
        }
        return if(hasFlipped) propensityContributionWhenValid else 0.0
    }


    override final fun isValid() = hasFlipped.also {
        // The condition can never switch to true when calling isValid,
        // the current value is returned and then switched to false
        hasFlipped = false
    }

}