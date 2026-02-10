package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

object TerritoryResolver {
    private val LOGGER = LoggerFactory.getLogger("WWC")

    fun getCurrentTerritoryName(): String? {
        return try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return null

            val modelsClass = Class.forName("com.wynntils.core.components.Models")
            val territoryField = modelsClass.getField("Territory")
            val territoryModel = territoryField.get(null)

            val method = territoryModel.javaClass.methods.firstOrNull {
                it.name == "getTerritoryProfileForPosition" && it.parameterCount == 1
            } ?: return null

            val positionArg = createPositionArgument(method.parameterTypes[0], player.pos) ?: return null
            val result = method.invoke(territoryModel, positionArg) ?: return null

            extractNameFromResult(result)
        } catch (e: Exception) {
            LOGGER.warn("Failed to extract territory: {} - {}", e.javaClass.simpleName, e.message)
            null
        }
    }

    private fun createPositionArgument(paramType: Class<*>, playerPos: Any): Any? {
        if (paramType.isInstance(playerPos) || paramType.isAssignableFrom(playerPos.javaClass)) {
            return playerPos
        }

        if (paramType.isInterface) {
            val proxy = createPositionProxy(paramType, playerPos)
            if (proxy != null) {
                return proxy
            }
        }

        val coords = getPositionCoordinates(playerPos) ?: return null
        val (x, y, z) = coords

        try {
            val ctor = paramType.getConstructor(Double::class.java, Double::class.java, Double::class.java)
            return ctor.newInstance(x, y, z)
        } catch (e: Exception) {
            // Ignore
        }

        try {
            val ctor = paramType.getConstructor(Int::class.java, Int::class.java, Int::class.java)
            return ctor.newInstance(x.toInt(), y.toInt(), z.toInt())
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    private fun createPositionProxy(paramType: Class<*>, playerPos: Any): Any? {
        return try {
            java.lang.reflect.Proxy.newProxyInstance(
                paramType.classLoader,
                arrayOf(paramType)
            ) { _, method, _ ->
                val coords = getPositionCoordinates(playerPos)
                if (coords == null) {
                    return@newProxyInstance defaultValueFor(method.returnType)
                }
                val (x, y, z) = coords
                when (method.name) {
                    "x", "getX", "method_10216" -> x
                    "y", "getY", "method_10214" -> y
                    "z", "getZ", "method_10215" -> z
                    else -> defaultValueFor(method.returnType)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getPositionCoordinates(playerPos: Any): Triple<Double, Double, Double>? {
        val x = getPositionCoordinate(playerPos, listOf("getX", "x", "method_10216"))
        val y = getPositionCoordinate(playerPos, listOf("getY", "y", "method_10214"))
        val z = getPositionCoordinate(playerPos, listOf("getZ", "z", "method_10215"))
        if (x == null || y == null || z == null) {
            return null
        }
        return Triple(x, y, z)
    }

    private fun getPositionCoordinate(playerPos: Any, names: List<String>): Double? {
        for (name in names) {
            try {
                val method = playerPos.javaClass.methods.firstOrNull {
                    it.name == name && it.parameterCount == 0
                }
                if (method != null) {
                    val value = method.invoke(playerPos)
                    if (value is Number) {
                        return value.toDouble()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }

            try {
                val field = playerPos.javaClass.getDeclaredField(name)
                field.isAccessible = true
                val value = field.get(playerPos)
                if (value is Number) {
                    return value.toDouble()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return null
    }

    private fun defaultValueFor(returnType: Class<*>): Any? {
        return when (returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            else -> null
        }
    }

    private fun extractNameFromResult(result: Any): String? {
        return try {
            val getNameMethod = result.javaClass.methods.firstOrNull {
                it.name == "getName" && it.parameterCount == 0
            }
            if (getNameMethod != null) {
                val name = getNameMethod.invoke(result) as? String
                if (!name.isNullOrEmpty()) {
                    return name
                }
            }

            if (result is Collection<*>) {
                val first = result.firstOrNull()
                if (first != null) {
                    return extractNameFromResult(first)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
