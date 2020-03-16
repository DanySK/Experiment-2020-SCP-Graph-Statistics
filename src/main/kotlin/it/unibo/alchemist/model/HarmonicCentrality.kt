package it.unibo.alchemist.model

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Node

object HarmonicCentrality {
    fun <T: Any> Environment<T, *>.harmonicCentralityOf(node: Node<T>): Double {
        val explored = mutableSetOf(node)
        val toExplore = getNeighborhood(node).toMutableSet()
        var centrality = 0.0
        var depth = 1
        while (!toExplore.isEmpty()) {
            val neighborsAtDepth = toExplore - explored
            explored.addAll(neighborsAtDepth)
            centrality += neighborsAtDepth.size.toDouble() / depth
            depth++
            toExplore.clear()
            neighborsAtDepth.flatMapTo(toExplore) { getNeighborhood(it) }
        }
        return centrality
    }
}
