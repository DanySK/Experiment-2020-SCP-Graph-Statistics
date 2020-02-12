package it.unibo.hll

import net.agkn.hll.HLL
import net.agkn.hll.HLLType

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
