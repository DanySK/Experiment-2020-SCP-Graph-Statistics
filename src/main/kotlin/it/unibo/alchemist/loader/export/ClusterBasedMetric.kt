package it.unibo.alchemist.loader.export

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import java.lang.IllegalStateException

@Suppress("UNCHECKED_CAST")
abstract class ClusterBasedMetric @JvmOverloads constructor(
        val leaderIdMolecule: Molecule
) : Extractor {
    final override fun extractData(environment: Environment<*, *>, reaction: Reaction<*>, time: Time, step: Long): DoubleArray {
        val clusters = environment.getNodes().asSequence()
            .map { it.leaderId to it }
            .filterNot { (leaderId, _) -> leaderId == null }
            .map {
                @Suppress("UNCHECKED_CAST") // checked right before this call
                it as Pair<Int, Node<*>>
            }
            .groupBy({it.component1()}, {it.component2()})
        return extractData(
                environment as Environment<Any, *>,
                reaction as Reaction<Any>,
                time,
                step,
                clusters as Map<Int, List<Node<Any>>>)
    }

    protected abstract fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Reaction<T>,
        time: Time,
        step: Long,
        clusters: Map<Int, List<Node<T>>>
    ): DoubleArray

    protected fun Environment<*, *>.clusterOf(node: Node<*>): Sequence<Node<*>> =
        (this as Environment<Nothing, *>).getNeighborhood(node as Node<Nothing>)
        .asSequence()
        .filter { it.leaderId != null && it.leaderId == node.leaderId }
        .flatMap { clusterOf(it) }

    protected val Node<*>.leaderId: Int? get() =
        if (contains(leaderIdMolecule)) {
            when (val leader = getConcentration(leaderIdMolecule)) {
                is Number -> leader.toInt()
                is String -> leader.toIntOrNull()
                else -> throw IllegalStateException("$leader is not a valid node id (type: ${leader::class.simpleName}")
            }
        } else null

}