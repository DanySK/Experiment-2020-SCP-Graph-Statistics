package it.unibo.alchemist.model.implementations.actions

import com.google.common.collect.ImmutableSet
import it.unibo.alchemist.model.implementations.times.DoubleTime
import it.unibo.alchemist.model.interfaces.Action
import it.unibo.alchemist.model.interfaces.Context
import it.unibo.alchemist.model.interfaces.Incarnation
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import org.jgrapht.Graph
import org.jgrapht.graph.SimpleGraph
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import it.unibo.alchemist.model.HarmonicCentrality.harmonicCentralityOf


private class NodeRef(node: Node<Double>) {

    private val reference = WeakReference(node)
    val node = reference.get()

    override fun equals(other: Any?) = other is NodeRef && node?.equals(other.node) ?: false

    override fun hashCode() =  node?.hashCode() ?: 0

    override fun toString() = "~>${node}"

    fun setConcentration(molecule: Molecule, value: Double) = node?.setConcentration(molecule, value)
}

private typealias NodeGraph = Graph<NodeRef, Pair<Node<Double>, Node<Double>>>
private typealias Environment = it.unibo.alchemist.model.interfaces.Environment<Double, *>

/**
 * Computes [Harmonic Centrality](https://en.wikipedia.org/wiki/Centrality#Harmonic_centrality) and associates each node
 * with its value.
 *
 * @property environment
 * @constructor
 * TODO
 *
 * @param node
 */
class ComputeHarmonicCentrality(
    val environment: Environment,
    node: Node<Double>,
    val targetMolecule: Molecule =
) : AbstractAction<Double>(node) {

    constructor(
        incarnation: Incarnation<Double, *>,
        environment: Environment,
        node: Node<Double>
    ) : this(environment, node, incarnation.createMolecule("harmonicCentrality"))

    private val nodeRef = node.toRef

    override fun execute() {
        node.setConcentration(targetMolecule, environment.harmonicCentralityOf(node))
    }

    /**
     * @return The context for this action.
     */
    override fun getContext() = Context.LOCAL

    /**
     * This method allows to clone this action on a new node. It may result
     * useful to support runtime creation of nodes with the same reaction
     * programming, e.g. for morphogenesis.
     *
     * @param n
     * The node where to clone this [Action]
     * @param r
     * The reaction to which the CURRENT action is assigned
     * @return the cloned action
     */
    override fun cloneAction(n: Node<Double>, r: Reaction<Double>?) = ComputeHarmonicCentrality(environment, n, targetMolecule)

    companion object {

        private val envToGraph = WeakHashMap<Environment, Pair<NodeGraph, Time>>()

        private val Node<Double>.toRef: NodeRef
            get() = NodeRef(this)

        private fun NodeGraph.repopulate(node: NodeRef, neighborhood: Sequence<NodeRef>) {
            removeVertex(node)
            addVertex(node)
            neighborhood.forEach {
                addEdge(node, it, Pair(node.node!!, it.node!!))
            }
        }

        private fun NodeGraph.neighborsOf(node: NodeRef) = edgesOf(node)
            .map { (n1, n2) -> if (n1 == node.node) n2 else n1 }
            .toSet()

        private val Environment.asGraph: Pair<NodeGraph, Time>
            get() = envToGraph.getOrPut(this) {
                val graph: NodeGraph = SimpleGraph(null)
                getNodes().asSequence()
                    .sorted()
                    .map{ getNeighborhood(it) }
                    .forEach { neighborhood ->
                        graph.repopulate(
                            neighborhood.center.toRef,
                            neighborhood.asSequence()
                                .filter { it.id < neighborhood.center.id }
                                .map { it.toRef }
                        )
                    }
                graph to DoubleTime(-1.0)
            }
    }
}
