package com.reposteria.presupuesto

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ---------- Formato de números ----------

fun money(v: Double): String {
    val s = String.format(Locale.US, "%,.2f", abs(v)).removeSuffix(".00")
    return (if (v < -0.004) "-$" else "$") + s
}

// 0.25 -> "0.25", 3.0 -> "3"
fun numText(v: Double): String =
    if (v == Math.floor(v) && abs(v) < 1e9) v.toLong().toString()
    else String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')

fun num(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

fun nowText(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())

// ---------- Modelo ----------

data class Insumo(val nombre: String, val precio: Double, val cantidad: Double)

data class Presupuesto(
    val id: Long,
    val fecha: String,
    val nombre: String,
    val insumos: List<Insumo>,
    val piezas: Int,
    val precioVenta: Double
) {
    val costoTotal: Double get() = insumos.sumOf { it.precio * it.cantidad }
    val costoPieza: Double get() = if (piezas > 0) costoTotal / piezas else 0.0
    val ingreso: Double get() = precioVenta * piezas
    val ganancia: Double get() = ingreso - costoTotal

    fun resumen(): String = buildString {
        appendLine(nombre)
        appendLine()
        appendLine("Insumos:")
        insumos.forEach { appendLine("- ${it.nombre}: ${money(it.precio * it.cantidad)}") }
        appendLine()
        appendLine("Costo total: ${money(costoTotal)}")
        if (piezas > 0) {
            appendLine("Piezas: $piezas")
            appendLine("Costo por pieza: ${money(costoPieza)}")
        }
        if (piezas > 0 && precioVenta > 0) {
            appendLine("Precio de venta: ${money(precioVenta)} c/u")
            appendLine("Ingreso: ${money(ingreso)}")
            append((if (ganancia < 0) "Pérdida: " else "Ganancia: ") + money(abs(ganancia)))
        }
    }.trimEnd()
}

// ---------- Guardado: un solo archivo con un arreglo JSON ----------

object PresupuestoStore {
    private const val FILE = "presupuestos.json"

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    fun loadAll(ctx: Context): List<Presupuesto> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            // Si el archivo se dañó, lo apartamos para no perderlo ni sobrescribirlo
            f.renameTo(File(ctx.filesDir, "presupuestos.dañado.${System.currentTimeMillis()}.json"))
            emptyList()
        }
    }

    private fun writeAll(ctx: Context, list: List<Presupuesto>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        val tmp = File(ctx.filesDir, "$FILE.tmp")
        tmp.writeText(arr.toString(2))
        tmp.renameTo(file(ctx))
    }

    // Si ya existe un presupuesto con ese id lo reemplaza; si no, lo agrega
    fun save(ctx: Context, p: Presupuesto) {
        val list = loadAll(ctx).toMutableList()
        val i = list.indexOfFirst { it.id == p.id }
        if (i >= 0) list[i] = p else list.add(p)
        writeAll(ctx, list)
    }

    fun delete(ctx: Context, id: Long) {
        writeAll(ctx, loadAll(ctx).filter { it.id != id })
    }

    private fun toJson(p: Presupuesto): JSONObject {
        val o = JSONObject()
        o.put("id", p.id)
        o.put("fecha", p.fecha)
        o.put("nombre", p.nombre)
        val its = JSONArray()
        p.insumos.forEach {
            its.put(JSONObject().put("n", it.nombre).put("p", it.precio).put("c", it.cantidad))
        }
        o.put("insumos", its)
        o.put("piezas", p.piezas)
        o.put("venta", p.precioVenta)
        return o
    }

    private fun fromJson(o: JSONObject): Presupuesto {
        val ja = o.getJSONArray("insumos")
        val its = (0 until ja.length()).map {
            val io = ja.getJSONObject(it)
            Insumo(io.optString("n", ""), io.optDouble("p", 0.0), io.optDouble("c", 0.0))
        }
        return Presupuesto(
            id = o.getLong("id"),
            fecha = o.optString("fecha", ""),
            nombre = o.optString("nombre", "Presupuesto"),
            insumos = its,
            piezas = o.optInt("piezas", 0),
            precioVenta = o.optDouble("venta", 0.0)
        )
    }
}
