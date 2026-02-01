
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}


var mixinsPath = "src${File.separator}mixin${File.separator}java${File.separator}com${File.separator}gamma${File.separator}spool${File.separator}mixin"
var mixinsDir = file(mixinsPath)
var outputFilePath = "META-INF${File.separator}mixins.txt"
var generatedResourcesDir: File? = project.layout.buildDirectory.dir("generated-resources").get().asFile

sourceSets {
    main {
        resources.srcDirs(generatedResourcesDir)
    }
}

tasks.register("generateFileList") {

    outputs.upToDateWhen { false }

    inputs.dir(mixinsDir)
    outputs.file(file("$generatedResourcesDir${File.separator}$outputFilePath"))

    doLast {
        println("Generating mixin file list from $mixinsDir...")
        generatedResourcesDir?.mkdirs()

        var fileList = fileTree(mixinsDir).map {
            relativePath(it)
                .substringAfter(mixinsPath + File.separator)
                .replace(File.separatorChar, '.')
                .replace(".java", "")
        }.toList()

        var early = fileList.filter { it.startsWith("minecraft.") }.map { it.substringAfter("minecraft.") }.sorted()
        var late = fileList.filter { it.startsWith("compat.") }.map { it.substringAfter("compat.") }.sorted()

        file("$generatedResourcesDir/$outputFilePath").printWriter().use { writer ->
            early.forEach { filePath ->
                writer.println(filePath)
            }
            writer.println("|") // special delimiter
            late.forEach { filePath ->
                writer.println(filePath)
            }
        }

        println("Generated mixin file list (containing ${early.size} early and ${late.size} late entries).")
    }
}

tasks.processResources {
    dependsOn("generateFileList")
}

tasks.sourcesJar {
    mustRunAfter("generateFileList")
}

tasks.jar {
    manifest {
        attributes["FMLAT"] = "spool_at.cfg"
    }
}
