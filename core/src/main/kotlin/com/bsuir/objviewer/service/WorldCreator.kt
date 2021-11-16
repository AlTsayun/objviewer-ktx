package com.bsuir.objviewer.service

import com.bsuir.objviewer.core.model.*
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class WorldCreator {

    val CUBE_OBJ_CONTENT = listOf(
        "v 0 0 0", // 1
        "v 0 0 100", // 2
        "v 0 100 0", // 3
        "v 0 100 100", // 4
        "v 100 0 0", // 5
        "v 100 0 100", // 6
        "v 100 100 0", // 7
        "v 100 100 100", // 8

        "f 1 2 4 3",
        "f 3 4 8 7",
        "f 7 8 6 5",
        "f 5 6 2 1",
        "f 3 7 5 1",
        "f 2 6 8 4",
    )

    val PYRAMID_OBJ_CONTENT = listOf(
        "v 0 0 0", // 1
        "v 0 0 1", // 2
        "v 0 3 0", // 3
        "v 0 1 1", // 4
        "v 1 0 0", // 5
        "v 1 0 1", // 6
        "v 1 1 0", // 7
        "v 1 1 1", // 8

        "f 1 3 5",
        "f 1 3 2",
        "f 1 2 5",
    )

    val world: World

    init {
        val parser = ObjParser()
//        val parsed = parser.parse(Files.readAllLines(Paths.get("cat.obj")))
        val parsed = parser.parse(Files.readAllLines(Paths.get("rigged_hand/Rigged Hand.obj")))
//        val parsed = parser.parse(Files.readAllLines(Paths.get("nissan_laurel_c31_turbo/laurel.obj")))
//        val parsed = parser.parse(Files.readAllLines(Paths.get("Batmobile.obj")))
//        val parsed = parser.parse(CUBE_OBJ_CONTENT)

        world = World(
                objects = mutableListOf(
                    parsed.let {
                        WorldObject(
                            UUID.randomUUID(),
                            it.vertexes,
                            it.textures,
                            it.normals,
                            it.faces,
                        )
                    }
                ),
                cam = Camera(
                    speed = 0.01,
                    front = mk.ndarray(listOf(1.0, 0.0, 0.0)),
//                    position = mk.ndarray(listOf(-900.0, 50.0, 0.5)),
                    position = mk.ndarray(listOf(-1.0, 1.0, 0.5)),
//                    windowSize = IntSize(1850, 1018)
                    windowSize = IntSize(640, 480)
                ),
                lightSources = mutableListOf(LightSource(0.0, 100.0, 100.0))
            )

    }
}