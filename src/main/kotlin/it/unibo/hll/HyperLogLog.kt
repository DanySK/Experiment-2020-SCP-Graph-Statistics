package it.unibo.hll

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.collect.ImmutableSet
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import net.agkn.hll.HLL
import net.agkn.hll.HLLType
import org.jgrapht.Graph
import org.jgrapht.graph.SimpleGraph
import org.protelis.lang.datatype.Field
import org.protelis.vm.ExecutionContext
import it.unibo.alchemist.model.HarmonicCentrality.harmonicCentralityOf

class HyperLogLog private constructor(val hll: HLL) {

    val cardinality by lazy { hll.cardinality() }
    private val bytes by lazy {
        hll.toBytes()
    }
    private val hashCode by lazy { bytes.contentHashCode() }

    fun union(other: HyperLogLog): HyperLogLog = when {
        hll.type == HLLType.EMPTY -> other
        other.hll.type == HLLType.EMPTY -> this
        else -> HyperLogLog(hll.clone().apply { union(other.hll) })
//        else -> cache.get(ImmutableSet.of(hll, other.hll))
    }

    override fun equals(other: Any?): Boolean {
        return other is HyperLogLog && bytes.contentEquals(other.bytes)
    }

    override fun hashCode() = hashCode

    override fun toString() = "HLL|$cardinality|"

    companion object {

        @JvmField val EMPTY_HLL = HyperLogLog(emptyHLL())
//        val cache = CacheBuilder.newBuilder()
//            .maximumSize(100_000)
//            .recordStats()
//            .build(CacheLoader.from { origins: Set<HLL>? ->
//                require(origins!!.size == 2)
//                val ordered = origins.toList()
//                HyperLogLog(ordered[0].clone().apply { union(ordered[1]) })
//            })

        fun emptyHLL(log2m: Int = 11, regwidth: Int = 5) = HLL(log2m, regwidth)
        fun AlchemistExecutionContext<*>.intFromEnviroment(id: String, orElse: Int): Int =
            (getExecutionEnvironment().get(id, orElse) as Number).toInt()

        @JvmOverloads @JvmStatic fun hyperLogLogFor(id: Iterable<Long>, log2m: Int = 11, regwidth: Int = 5) = HyperLogLog(
            emptyHLL(log2m, regwidth).apply { id.forEach { addRaw(rrmxmx(it.toULong()).toLong()) } }
        )

        @JvmOverloads @JvmStatic fun myself(
            context: AlchemistExecutionContext<*>,
            log2m: Int = context.intFromEnviroment("log2m", 11),
            regwidth: Int = context.intFromEnviroment("regwidth", 5)
        ) = HyperLogLog(
            HLL(log2m, regwidth).apply {
                val node = context.getDeviceUID() as Node<*>
                addRaw(rrmxmx(node.id.toULong()).toLong())
            }
        )

//        @JvmOverloads @JvmStatic fun printStats() = println(cache.stats())

    }
}

object HarmonicCentrality {

    @JvmStatic fun harmonicCentralityFromHLL(cardinalities: Iterable<HyperLogLog>): Double = cardinalities.asSequence()
        .map { it.cardinality }
        .zipWithNext { a, b -> b - a }
        .withIndex()
        .map { it.value.toDouble() / (it.index + 1) } // / (it.index + 2) }
        .sum()

    private val hCMol = SimpleMolecule("harmonicCentrality")
    @JvmStatic fun recomputeHarmonicCentrality(context: AlchemistExecutionContext<*>): Unit {
        with (context.getEnvironmentAccess()) {
            getNodes().forEach {
                it.setConcentration(hCMol, harmonicCentralityOf(it))
            }
        }
//        val environment = context.getEnvironmentAccess()
//        val graph: Graph<Node<Any>, *> = SimpleGraph(Any::class.java)
//        environment.getNodes().asSequence()
//            .onEach { graph.addVertex(it) }
//            .map{ environment.getNeighborhood(it) }
//            .map { neighborhood -> Pair(
//                neighborhood.center,
//                neighborhood.neighbors.asSequence()
//                    // Only keep links from higher ids to lower ones.
//                    // Also ensures that the vertex is added before each edge
//                    .filter { it.id < neighborhood.center.id })
//            }
//            .flatMap { (center, neighbors) -> neighbors.map { center to it } }
////            .onEach { (_, node) -> graph.addVertex(node) }
//            .forEach { (n1, n2) -> graph.addEdge(n1, n2) }
//        // Compute scores and inject in every node
//        org.jgrapht.alg.scoring.HarmonicCentrality(graph, false, false)
//            .scores
//            .forEach { (node, value) ->
//                node.setConcentration(SimpleMolecule("actualCentrality"), value)
//            }
    }
}

object FieldUtil {
    @JvmStatic fun <T> foldToMap(f: Field<T>) = f.toMap()
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
@ExperimentalUnsignedTypes
infix fun ULong.ror(shift: Int): ULong = (this shr shift) or (this shl (64 - shift))
