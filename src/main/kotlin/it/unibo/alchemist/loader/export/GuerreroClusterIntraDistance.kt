package it.unibo.alchemist.loader.export

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import java.lang.IllegalStateException

class GuerreroClusterIntraDistance @JvmOverloads constructor(
        val useHopCount: Boolean = false,
        val leaderIdMolecule: Molecule
) : Extractor {
    override fun extractData(environment: Environment<*, *>, reaction: Reaction<*>, time: Time, step: Long): DoubleArray {
        val visitedNodes: MutableSet<Node<*>> = mutableSetOf()
        while (visitedNodes.size != environment.getNodeCount())
        for (node in environment.getNodes()) {
            visitedNodes.add(node)
            val cluster = environment.clusterOf(node)

        }
        val clusters = environment.getNodes().asSequence()
            .filter { it.contains(leaderIdMolecule) }
            .groupBy { it.getConcentration(leaderIdMolecule) }
        return clusters.map { (_, cluster) -> cluster.count() }
    }

    fun Environment<*, *>.clusterOf(node: Node<*>): Sequence<Node<*>> =
        (this as Environment<Nothing, *>).getNeighborhood(node as Node<Nothing>)
        .asSequence()
        .filter { it.leaderId != null && it.leaderId == node.leaderId }
        .flatMap { clusterOf(it) }

    val Node<*>.leaderId: Int? get() =
        if (contains(leaderIdMolecule)) {
            when (val leader = getConcentration(leaderIdMolecule)) {
                is Number -> leader.toInt()
                is String -> leader.toIntOrNull()
                else -> throw IllegalStateException("$leader is not a valid node id (type: ${leader::class.simpleName}")
            }
        } else null

    override fun getNames(): List<String> {
        TODO("Not yet implemented")
    }
}