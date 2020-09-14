package it.unibo.alchemist.loader.export

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.SimpleGraph
import java.util.*
import javax.print.attribute.standard.MediaSize

class GuerreroClosestNeighborDistance @JvmOverloads constructor(
        val useHopCount: Boolean = false,
        leaderIdMolecule: Molecule
) : ClusterBasedMetric(leaderIdMolecule) {

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Reaction<T>,
        time: Time,
        step: Long,
        clusters: Map<Int, List<Node<T>>>
    ): DoubleArray {
        val shortestPaths = environment.shortestPaths { center, other ->
            if (useHopCount) 1.0 else environment.getDistanceBetweenNodes(center, other)
        }
        val pairs = mutableListOf<Pair<Node<T>, Node<T>>>()
        val leaders = clusters.map { (leaderId, _) -> environment.getNodeByID(leaderId) }
        for (i in leaders.indices) {
            for (j in i + 1 until leaders.size) {
                pairs.add(leaders[i] to leaders [j])
            }
        }
        return doubleArrayOf(
            pairs.asSequence()
                .map { (first, second) -> shortestPaths.getPathWeight(first, second) }
                .average()
        )
    }

    private fun <T> Environment<T, *>.shortestPaths(
        metric: (Node<T>, Node<T>) -> Double
    ): DijkstraShortestPath<Node<T>, Double> {
        val graph = SimpleGraph<Node<T>, Double>(null, null, true)
        val visited = mutableSetOf<Node<T>>()
        val toVisit = ArrayDeque(getNodes())
        while (toVisit.isNotEmpty()) {
            var current = toVisit.poll()
            graph.addVertex(current)
            val neighbors = getNeighborhood(current).neighbors.filter { it !in visited }
            neighbors.forEach {
                graph.addVertex(it)
                graph.addEdge(current, it, metric(current, it))
            }
            toVisit.addAll(neighbors)
        }
        return DijkstraShortestPath(graph)
    }

    override fun getNames(): List<String> = listOf("ClusterIntraDistance")

}