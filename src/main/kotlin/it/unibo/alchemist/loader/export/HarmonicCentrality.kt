package it.unibo.alchemist.loader.export
/*
import it.unibo.alchemist.model.implementations.actions.ComputeHarmonicCentrality.Companion.repopulate
import it.unibo.alchemist.model.implementations.actions.ComputeHarmonicCentrality.Companion.toRef
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import org.jgrapht.Graph
import org.jgrapht.alg.scoring.HarmonicCentrality
import org.jgrapht.graph.SimpleGraph


private typealias Edge<T> = Pair<Node<T>, Node<T>>
private typealias NodeGraph<T> = Graph<Node<T>, Pair<Node<T>, Node<T>>>

class HarmonicCentrality @JvmOverloads constructor(
    val alsoInject: Boolean = true,

): Extractor {

    override fun extractData(env: Environment<*, *>, r: Reaction<*>?, time: Time?, step: Long) = doubleArrayOf(
        HarmonicCentrality(env.asGraph).
    )

    override fun getNames() = name

    companion object {
        val name = listOf(this::class.simpleName)
        private val <T> Environment<T, *>.asGraph: NodeGraph<T>
            get() = SimpleGraph<Node<T>, Edge<T>>(null).also { graph ->
                getNodes().asSequence()
                    .map{ getNeighborhood(it) }
                    .forEach { neighborhood ->
                        graph.addVertex(neighborhood.center)
                        neighborhood.asSequence()
                            .filter { it.id < neighborhood.center.id }
                            .forEach { graph.addEdge(neighborhood.center, it, neighborhood.center to it) }
                    }
            }
    }
} */