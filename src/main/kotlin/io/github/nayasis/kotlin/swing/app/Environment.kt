package io.github.nayasis.kotlin.swing.app

import io.github.nayasis.kotlin.basica.core.collection.flattenKeys
import io.github.nayasis.kotlin.basica.core.collection.getByMvel
import io.github.nayasis.kotlin.basica.core.collection.merge
import io.github.nayasis.kotlin.basica.core.collection.setByMvel
import io.github.nayasis.kotlin.basica.core.collection.toJson
import io.github.nayasis.kotlin.basica.core.string.toResource
import io.github.nayasis.kotlin.basica.core.url.toInputStream
import org.yaml.snakeyaml.Yaml

class Environment(
    args: Array<String>? = null,
    configYamlPath: String = "application.yml",
) {

    val map = linkedMapOf<String,Any>()

    init {
        loadYml(configYamlPath)
        args?.let { merge(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadYml(path: String) {
        path.toResource()?.toInputStream()?.let {
            Yaml().loadAll(it)
        }?.map {
            it as Map<String,Any>
        }?.map {
            it.forEach { key, value ->
                map[key] = value
            }
        }
    }

    fun merge(args: Array<String>): Environment {
        args.filter { it.contains("=") }
            .map { it.split("=", limit = 2) }
            .associate { (key, value) -> key to value }
            .forEach { (key, value) -> map[key] = value }
        return this
    }

    fun merge(other: Map<String, Any?>): Environment {
        map.merge(other)
        return this
    }

    inline operator fun <reified T: Any> get(key: String): T? {
        return map.getByMvel(key)
    }

    operator fun set(key: String, value: Any) {
        map.setByMvel(key, value)
    }

    operator fun contains(key: String): Boolean {
        return map.getByMvel<Any>(key) != null
    }

    fun startsWith(prefix: String): Map<String, Any?> {
        return map.flattenKeys().filter { it.key.startsWith(prefix) }
    }

    override fun toString(): String {
        return map.toString()
    }

    fun toJson(pretty: Boolean = true): String {
        return map.toJson(pretty)
    }

}