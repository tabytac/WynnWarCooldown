package wynnwarcooldown.tabytac

import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

object TerritoryResolver {
    private val LOGGER = LoggerFactory.getLogger("WWC")

    private fun getTerritoryModel(): Any? {
        return try {
            val modelsClass = Class.forName("com.wynntils.core.components.Models")
            val territoryField = modelsClass.getField("Territory")
            territoryField.get(null)
        } catch (e: Exception) {
            LOGGER.warn("Failed to get Territory model: {} - {}", e.javaClass.simpleName, e.message)
            null
        }
    }

    fun getCurrentTerritoryName(): String? {
        return try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return null

            val territoryModel = getTerritoryModel() ?: return null

            val method = territoryModel.javaClass.methods.firstOrNull {
                it.name == "getTerritoryProfileForPosition" && it.parameterCount == 1
            } ?: return null

            val playerPos = net.minecraft.util.math.Vec3d(player.x, player.y, player.z)
            val positionArg = createPositionArgument(method.parameterTypes[0], playerPos) ?: return null
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

    fun getAllTerritoryNames(): List<String> {
        return try {
            val territoryModel = getTerritoryModel()
            if (territoryModel == null) {
                LOGGER.warn("Failed to get territory model for getAllTerritoryNames")
                return emptyList()
            }

            val method = territoryModel.javaClass.methods.firstOrNull {
                it.name == "getTerritoryNames" && it.parameterCount == 0
            }
            if (method == null) {
                LOGGER.warn("getTerritoryNames method not found")
                return emptyList()
            }

            val result = method.invoke(territoryModel)

            // Try to handle Stream<String>
            when (result) {
                is java.util.stream.Stream<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (result as java.util.stream.Stream<String>).toList().sorted()
                }
                is Collection<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (result as Collection<String>).sorted()
                }
                else -> {
                    LOGGER.warn("getTerritoryNames returned unexpected type: ${result?.javaClass?.simpleName}")
                    return emptyList()
                }
            }
        } catch (e: Exception) {
            LOGGER.warn("Failed to get territory names: {} - {}", e.javaClass.simpleName, e.message)
            emptyList()
        }
    }

    fun getTerritoryProfile(territoryName: String): TerritoryProfileData? {
        return try {
            val territoryModel = getTerritoryModel()
            if (territoryModel == null) {
                LOGGER.warn("Failed to get territory model for $territoryName")
                return null
            }

            val method = territoryModel.javaClass.methods.firstOrNull {
                it.name == "getTerritoryProfile" && it.parameterCount == 1
            }
            if (method == null) {
                LOGGER.warn("getTerritoryProfile method not found")
                return null
            }

            val profile = method.invoke(territoryModel, territoryName)
            if (profile == null) {
                LOGGER.warn("getTerritoryProfile returned null for: $territoryName")
                return null
            }

            // Extract fields using reflection
            val getAcquiredMethod = profile.javaClass.methods.firstOrNull {
                it.name == "getAcquired" && it.parameterCount == 0
            }
            val isOnCooldownMethod = profile.javaClass.methods.firstOrNull {
                it.name == "isOnCooldown" && it.parameterCount == 0
            }

            if (getAcquiredMethod == null || isOnCooldownMethod == null) {
                LOGGER.warn("Missing methods on profile: acquired=$getAcquiredMethod, isOnCooldown=$isOnCooldownMethod")
                return null
            }

            val acquired = getAcquiredMethod.invoke(profile)
            val isOnCooldown = isOnCooldownMethod.invoke(profile) as? Boolean ?: false

            // Calculate time held from Instant
            val timeHeldMillis = if (acquired != null) {
                // acquired is java.time.Instant
                val acquiredInstant = acquired as? java.time.Instant
                if (acquiredInstant != null) {
                    System.currentTimeMillis() - acquiredInstant.toEpochMilli()
                } else {
                    0L
                }
            } else {
                0L
            }

            TerritoryProfileData(
                name = territoryName,
                acquired = acquired,
                isOnCooldown = isOnCooldown,
                timeHeldMillis = timeHeldMillis
            )
        } catch (e: Exception) {
            LOGGER.warn("Failed to get territory profile for $territoryName: {} - {}", e.javaClass.simpleName, e.message)
            null
        }
    }

    data class TerritoryProfileData(
        val name: String,
        val acquired: Any?,
        val isOnCooldown: Boolean,
        val timeHeldMillis: Long
    )
}
