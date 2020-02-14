package it.unibo.hll

import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import net.agkn.hll.HLL
import net.agkn.hll.HLLType
import org.jgrapht.Graph
import org.jgrapht.graph.SimpleGraph

data class HyperLogLog private constructor(val hll: HLL) {

    val cardinality get() = hll.cardinality()

    fun union(other: HyperLogLog): HyperLogLog = when {
        hll.type == HLLType.EMPTY -> other
        other.hll.type == HLLType.EMPTY -> this
        else -> HyperLogLog(hll.clone().apply { union(other.hll) })
    }

    override fun toString() = "HLL|$cardinality|"

    companion object {

        @JvmField val EMPTY_HLL = HyperLogLog(emptyHLL())

        fun emptyHLL(log2m: Int = 11, regwidth: Int = 5) = HLL(log2m, regwidth)

        @JvmOverloads @JvmStatic fun hyperLogLogFor(id: Iterable<Long>, log2m: Int = 11, regwidth: Int = 5) = HyperLogLog(
            emptyHLL(log2m, regwidth).apply { id.forEach { addRaw(rrmxmx(it.toULong()).toLong()) } }
        )

    }
}

object HarmonicCentrality {

    @JvmStatic fun harmonicCentralityFromHLL(cardinalities: Iterable<HyperLogLog>): Double = cardinalities.asSequence()
        .map { it.cardinality }
        .zipWithNext { a, b -> b - a }
        .withIndex()
        .map { it.value.toDouble() / (it.index + 1) } // / (it.index + 2) }
        .sum()

    @JvmStatic fun recomputeHarmonicCentrality(context: AlchemistExecutionContext<*>) {
        val environment = context.getEnvironmentAccess()
        val graph: Graph<Node<Any>, *> = SimpleGraph(Any::class.java)
        environment.getNodes().asSequence()
            .onEach { graph.addVertex(it) }
            .map{ environment.getNeighborhood(it) }
            .map { neighborhood -> Pair(
                neighborhood.center,
                neighborhood.neighbors.asSequence()
                    // Only keep links from higher ids to lower ones.
                    // Also ensures that the vertex is added before each edge
                    .filter { it.id < neighborhood.center.id })
            }
            .flatMap { (center, neighbors) -> neighbors.map { center to it } }
//            .onEach { (_, node) -> graph.addVertex(node) }
            .forEach { (n1, n2) -> graph.addEdge(n1, n2) }
        // Compute scores and inject in every node
        org.jgrapht.alg.scoring.HarmonicCentrality(graph).scores.forEach { (node, value) ->
            node.setConcentration(SimpleMolecule("actualCentrality"), value)
        }
    }
}

val M: ULong = 0x4fb21c651e98df25L.toULong() + 0x5000000000000000L.toULong()
const val S = 28
const val R1 = 49
const val R2 = 24
fun rrmxmx(v: ULong): ULong {
    var a: ULong = v xor ((v ror R1) xor (v ror R2))
    a *= M
    a = a xor (a shr S)
    a *= M
    return a xor (a shr S)
}
infix fun ULong.ror(shift: Int): ULong = (this shr shift) or (this shl (64 - shift))
