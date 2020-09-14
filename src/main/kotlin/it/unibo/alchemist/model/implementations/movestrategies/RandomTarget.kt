package it.unibo.alchemist.model.implementations.movestrategies

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.environments.Environment2DWithObstacles
import it.unibo.alchemist.nextDouble
import kotlin.math.cos
import kotlin.math.sin
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.PI

/**
 * Selects a target based on a random direction extracted from [directionRng],
 * and a random distance extracted from [distanceDistribution].
 * [getCurrentPosition] should return the current position of the object to move.
 * [T] is the type of the concentration of the node.
 */
class RandomTarget<T>(
    private val environment: Environment<T, Euclidean2DPosition>,
    getCurrentPosition: () -> Euclidean2DPosition,
    private val makePosition: (Double, Double) -> Euclidean2DPosition,
    private val directionRng: RandomGenerator,
    private val distanceDistribution: RealDistribution
) : ChangeTargetOnCollision<Euclidean2DPosition>(getCurrentPosition) {

    /**
     * Handy constructor for Alchemist where the object to move is a [node] in the [env].
     */
    constructor(node: Node<T>, env: Environment<T, Euclidean2DPosition>, directionRng: RandomGenerator, distanceDistribution: RealDistribution) :
        this(env, { env.getPosition(node) }, { x, y -> env.makePosition(x, y) }, directionRng, distanceDistribution)

    override fun chooseTarget() = with(directionRng.nextDouble(0.0, 2 * PI)) {
        val distance = distanceDistribution.sample()
        val current = getCurrentPosition()
        val delta = makePosition(distance * cos(this), distance * sin(this))
        val desired = current + delta
        when (environment) {
            is Environment2DWithObstacles<*, T> -> {
                val (currentX, currentY) = current.coordinates
                val (desiredX, desiredY) = desired.coordinates
                environment.next(Euclidean2DPosition(currentX, currentY), Euclidean2DPosition(desiredX, desiredY))
            }
            else -> desired
        }
    }

}
