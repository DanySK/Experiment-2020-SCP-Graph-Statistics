package it.unibo.hll

import it.unibo.alchemist.model.HarmonicCentrality.harmonicCentralityOf
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.protelis.AlchemistExecutionContext

@ExperimentalUnsignedTypes
object HarmonicCentrality {

    @JvmStatic fun harmonicCentralityFromHLL(cardinalities: Iterable<HyperLogLog>): Double = cardinalities.asSequence()
        .indexed { it.value.toDouble() / (it.index + 1) } // / (it.index + 2) }
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


@ExperimentalUnsignedTypes
object ClosenessCentrality {
    @JvmStatic fun closenessCentralityFromHLL(cardinalities: Iterable<HyperLogLog>): Double = 1 / cardinalities.asSequence()
        .indexed { it.value.toDouble() * (it.index + 1) }
        .sum()
}

@ExperimentalUnsignedTypes
private fun <R> Sequence<HyperLogLog>.indexed(operation: (IndexedValue<Long>)->R): Sequence<R> =
    map { it.cardinality }.zipWithNext { a, b -> b - a }.withIndex().map(operation)
