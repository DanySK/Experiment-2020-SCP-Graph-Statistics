package it.unibo.alchemist.model.implementations.terminators

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Time
import java.util.function.Predicate

class AfterTime(val endTime: Time) : Predicate<Environment<*, *>> {
    override fun test(environment: Environment<*, *>) =
        environment.getSimulation().getTime() >= endTime
}