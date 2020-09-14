import org.protelis.lang.ProtelisLoader
import org.protelis.lang.datatype.DeviceUID
import org.protelis.lang.datatype.impl.IntegerUID
import org.protelis.vm.ProtelisVM
import org.protelis.vm.impl.AbstractExecutionContext
import org.protelis.vm.impl.SimpleExecutionEnvironment
import org.protelis.vm.impl.SimpleNetworkManager

object Context : AbstractExecutionContext<Context>(SimpleExecutionEnvironment(), SimpleNetworkManager()){
    override fun nextRandomDouble(): Double = TODO()
    override fun getDeviceUID()= IntegerUID(0)
    override fun getCurrentTime() = System.currentTimeMillis()
    override fun instance() = TODO()
}

fun main() {
    ProtelisVM(ProtelisLoader.parse("""
[0].map {
    rep(local <- it) { local + 1 }
}
""".trimIndent()), Context).apply {
        (0..100).forEach {
            runCycle()
            println("Cycle $it -> $currentValue")
        }
    }
}