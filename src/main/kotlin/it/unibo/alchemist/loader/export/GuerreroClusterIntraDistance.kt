package it.unibo.alchemist.loader.export

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.SimpleGraph
import org.jgrapht.graph.SimpleWeightedGraph
import java.util.*

class GuerreroClusterIntraDistance @JvmOverloads constructor(
        private val useHopCount: Boolean = false,
        leaderIdMolecule: Molecule
) : ClusterBasedMetric(leaderIdMolecule) {

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Reaction<T>?,
        time: Time,
        step: Long,
        clusters: Map<Int, List<Node<T>>>
    ): DoubleArray = doubleArrayOf(
        clusters.map { (leaderId, clusterNodes) ->
            val center = environment.getNodeByID(leaderId)
            val shortestPath = environment.shortestPathInCluster(clusterNodes, center) { first, other ->
                if (useHopCount) 1.0 else environment.getDistanceBetweenNodes(first, other)
            }
            clusterNodes.asSequence()
                .flatMap {
                    try {
                        sequenceOf(shortestPath.getPathWeight(center, it))
                    } catch (e: IllegalArgumentException) {
                        // The cluster is not well formed, some node got cut off
                        emptySequence<Double>()
                    }
                }
                .average()
        }.average()
    )

    private fun <T> Environment<T, *>.shortestPathInCluster(
        cluster: List<Node<T>>,
        center: Node<T>,
        metric: (Node<T>, Node<T>) -> Double
    ): DijkstraShortestPath<Node<T>, Any> {
        val graph = SimpleWeightedGraph<Node<T>, Any>(null, { Any() })
        val visited = mutableSetOf<Node<T>>()
        val toVisit = ArrayDeque<Node<T>>().also { it.add(center) }
        while (toVisit.isNotEmpty()) {
            var current = toVisit.poll()
            graph.addVertex(current)
            val neighbors = getNeighborhood(current).neighbors.filter { it in cluster && it !in visited }
            neighbors.forEach {
                graph.addVertex(it)
                graph.addEdge(current, it)
                graph.setEdgeWeight(current, it, metric(current, it))
            }
            visited.add(current)
            toVisit.addAll(neighbors)
        }
        return DijkstraShortestPath(graph)
    }

    override fun getNames(): List<String> = listOf(
        "${"HopCount".takeIf { useHopCount } ?: "" }ClusterIntraDistance[$leaderIdMolecule]"
    )

}