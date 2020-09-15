package it.unibo.alchemist.loader.export

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.SimpleWeightedGraph
import java.util.ArrayDeque
import java.util.WeakHashMap

class GuerreroClosestNeighborDistance @JvmOverloads constructor(
        private val useHopCount: Boolean = false,
        leaderIdMolecule: Molecule,
        private val immutableEnvironment: Boolean = false
) : ClusterBasedMetric(leaderIdMolecule) {

    private val pathCache : MutableMap<Pair<Any, Any>, Double> = mutableMapOf()

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Reaction<T>?,
        time: Time,
        step: Long,
        clusters: Map<Int, List<Node<T>>>
    ): DoubleArray {
        val shortestPaths = computeShortestPaths(environment)
        val pairs = mutableListOf<Pair<Node<T>, Node<T>>>()
        val leaders = clusters.map { (leaderId, _) -> environment.getNodeByID(leaderId) }
        for (i in leaders.indices) {
            for (j in i + 1 until leaders.size) {
                pairs.add(leaders[i] to leaders [j])
            }
        }
        return doubleArrayOf(
            pairs.asSequence()
                .map { nodes ->
                    runCatching {
                        if (immutableEnvironment) {
                            pathCache.computeIfAbsent(nodes) {
                                shortestPaths.getPathWeight(nodes.first, nodes.second)
                            }
                        } else {
                            shortestPaths.getPathWeight(nodes.first, nodes.second)
                        }
                    }
                }
                .filter { it.isSuccess }
                .map { it.getOrThrow() }
                .average()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> computeShortestPaths(environment: Environment<T, *>): DijkstraShortestPath<Node<T>, Any> =
        if (immutableEnvironment) {
            dijkstraPathCache.computeIfAbsent(this) {
                environment.shortestPaths() as DijkstraShortestPath<Node<Any>, Any>
            } as DijkstraShortestPath<Node<T>, Any>
        } else {
            environment.shortestPaths()
        }

    override fun getNames(): List<String> = listOf(
        "${"HopCount".takeIf { useHopCount } ?: "" }ClusterClosestNeighborDistance[$leaderIdMolecule]"
    )

    fun <T> Environment<T, *>.shortestPaths() = shortestPaths { center, other ->
        if (useHopCount) 1.0 else getDistanceBetweenNodes(center, other)
    }

    companion object {
        private val dijkstraPathCache: MutableMap<GuerreroClosestNeighborDistance, DijkstraShortestPath<Node<Any>, Any>> = WeakHashMap()

        private fun <T> Environment<T, *>.shortestPaths(
            metric: (Node<T>, Node<T>) -> Double
        ): DijkstraShortestPath<Node<T>, Any> {
            val graph = SimpleWeightedGraph<Node<T>, Any>(null, { Any() })
            val visited = mutableSetOf<Node<T>>()
            val toVisit = ArrayDeque(getNodes())
            while (toVisit.isNotEmpty()) {
                var current = toVisit.poll()
                graph.addVertex(current)
                val neighbors = getNeighborhood(current).neighbors.filter { it !in visited }
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
    }

}