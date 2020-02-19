package it.unibo.hll

import it.unibo.alchemist.model.HarmonicCentrality.harmonicCentralityOf
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import net.agkn.hll.HLL
import net.agkn.hll.HLLType
import org.protelis.lang.datatype.Field

@ExperimentalUnsignedTypes
class HyperLogLog private constructor(val hll: HLL) {

    val cardinality by lazy { hll.cardinality() }
    private val bytes: ByteArray by lazy {
        hll.toBytes()
    }
    private val hashCode by lazy { bytes.contentHashCode() }

    fun union(other: HyperLogLog): HyperLogLog = when {
        hll.type == HLLType.EMPTY -> other
        other.hll.type == HLLType.EMPTY -> this
        else -> HyperLogLog(hll.clone().apply { union(other.hll) })
    }

    override fun equals(other: Any?): Boolean {
        return other is HyperLogLog && bytes.contentEquals(other.bytes)
    }

    override fun hashCode() = hashCode

    override fun toString() = "HLL|$cardinality|"

    companion object {

        @JvmField val EMPTY_HLL = HyperLogLog(emptyHLL())

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

@ExperimentalUnsignedTypes
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
    }
}

object FieldUtil {
    @JvmStatic fun <T> foldToMap(f: Field<T>) = f.toMap()
}

@ExperimentalUnsignedTypes
private val M: ULong = 0x4fb21c651e98df25L.toULong() + 0x5000000000000000L.toULong()
private const val S = 28
private const val R1 = 49
private const val R2 = 24
@ExperimentalUnsignedTypes
fun rrmxmx(v: ULong): ULong {
    var a: ULong = v xor ((v ror R1) xor (v ror R2))
    a *= M
    a = a xor (a shr S)
    a *= M
    return a xor (a shr S)
}
@ExperimentalUnsignedTypes
infix fun ULong.ror(shift: Int): ULong = (this shr shift) or (this shl (64 - shift))
