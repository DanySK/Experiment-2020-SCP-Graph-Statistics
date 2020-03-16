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
        return other is HyperLogLog && cardinality == other.cardinality
    }

    override fun hashCode() = hashCode

    override fun toString() = "HLL|$cardinality|"

    companion object {

        var EMPTY_HLL: HyperLogLog? = null

         @JvmStatic  fun emptyHLL(context: AlchemistExecutionContext<*>) =
            EMPTY_HLL ?: with(context) { HyperLogLog(HLL(log2m, regwidth)) }.also { EMPTY_HLL = it }

        @JvmStatic fun hyperLogLogFor(context: AlchemistExecutionContext<*>, id: Iterable<Long>) = HyperLogLog(
            HLL(context.log2m, context.regwidth).apply { id.forEach { addRaw(rrmxmx(it.toULong()).toLong()) } }
        )

        @JvmStatic fun myself(context: AlchemistExecutionContext<*>) = with(context) {
            HyperLogLog(
                HLL(log2m, regwidth).apply {
                    val node = context.getDeviceUID() as Node<*>
                    addRaw(rrmxmx(node.id.toULong()).toLong())
                }
            )
        }

        fun AlchemistExecutionContext<*>.intFromEnviroment(id: String, orElse: Int): Int =
            (getExecutionEnvironment().get(id, orElse) as Number).toInt()

        val AlchemistExecutionContext<*>.log2m get() = intFromEnviroment("log2m", 11)

        val AlchemistExecutionContext<*>.regwidth get() = intFromEnviroment("regwidth", 5)

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
