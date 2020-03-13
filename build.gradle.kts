import java.io.ByteArrayOutputStream

plugins {
    application
    kotlin("jvm") version "1.3.61"
}

repositories {
    mavenCentral()
    /* 
     * The following repositories contain beta features and should be added for experimental mode only
     * 
     * maven("https://dl.bintray.com/alchemist-simulator/Alchemist/")
     * maven("https://dl.bintray.com/protelis/Protelis/")
     */
}
/*
 * Only required if you plan to use Protelis, remove otherwise
 */
sourceSets {
    main {
        resources {
            srcDir("src/main/protelis")
        }
    }
}
dependencies {
    implementation("com.codepoetics:protonpack:1.13")
    implementation("it.unibo.alchemist:alchemist:9.3.0")
    implementation("net.agkn:hll:1.6.0")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("org.jgrapht:jgrapht-core:1.3.1")
    implementation("org.protelis:protelis-lang:13.1.0")
    implementation(kotlin("stdlib-jdk8"))
}

// Heap size estimation for batches
val maxHeap: Long? by project
val heap: Long = maxHeap ?:
if (System.getProperty("os.name").toLowerCase().contains("linux")) {
    ByteArrayOutputStream().use { output ->
        exec {
            executable = "bash"
            args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
            standardOutput = output
        }
        output.toString().trim().toLong() / 1024
    }
        .also { println("Detected ${it}MB RAM available.") }  * 9 / 10
} else {
    // Guess 16GB RAM of which 2 used by the OS
    14 * 1024L
}
val taskSizeFromProject: Int? by project
val taskSize = taskSizeFromProject ?: 20 * 1024 // 5K nodes with 10 neighbors require a lot of memory
val threadCount = maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize ))

val alchemistGroup = "Run Alchemist"
/*
 * This task is used to run all experiments in sequence
 */
val runAllGraphic by tasks.register<DefaultTask>("runAllGraphic") {
    group = alchemistGroup
    description = "Launches all simulations with the graphic subsystem enabled"
}
val runAllBatch by tasks.register<DefaultTask>("runAllBatch") {
    group = alchemistGroup
    description = "Launches all experiments"
}
/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
val variables = mapOf(
    "simulation" to arrayOf("seed", "speed", "meanNeighbors", "nodeCount"),
    "converge" to arrayOf("diameter")
)
File(rootProject.rootDir.path + "/src/main/yaml").listFiles()
    ?.filter { it.extension == "yml" }
    ?.sortedBy { it.nameWithoutExtension }
    ?.forEach {
        fun basetask(name: String, additionalConfiguration: JavaExec.() -> Unit = {}) = tasks.register<JavaExec>(name) {
            group = alchemistGroup
            description = "Launches graphic simulation ${it.nameWithoutExtension}"
            main = "it.unibo.alchemist.Alchemist"
            classpath = sourceSets["main"].runtimeClasspath
            args(
                "-y", it.absolutePath,
                "-g", "effects/${it.nameWithoutExtension}.aes"
            )
            if (System.getenv("CI") == "true") {
                args("-hl", "-t", "2")
            }
            this.additionalConfiguration()
        }
        val capitalizedName = it.nameWithoutExtension.capitalize()
        val graphic by basetask("run${capitalizedName}Graphic")
        runAllGraphic.dependsOn(graphic)
        val batch by basetask("run${capitalizedName}Batch") {
            description = "Launches batch experiments for $capitalizedName"
            jvmArgs("-XX:+AggressiveHeap")
            maxHeapSize = "${minOf(heap.toInt(), Runtime.getRuntime().availableProcessors() * taskSize)}m"
            File("data").mkdirs()
            args(
                "-e", "data/${it.nameWithoutExtension}",
                "-b",
                "-var", "seed", *variables[it.nameWithoutExtension]!!,
                "-p", threadCount,
                "-i", 0.5
            )
        }
        runAllBatch.dependsOn(batch)
    }

