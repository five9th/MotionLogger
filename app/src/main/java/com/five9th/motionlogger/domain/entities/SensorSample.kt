package com.five9th.motionlogger.domain.entities

data class SensorSample(
    val timestampMs: Long,
    val values: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorSample

        if (timestampMs != other.timestampMs) return false
        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestampMs.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}

data class SensorField(
    val id: String,        // "acc_x"
    // mb add later:
//    val label: String,     // "Acceleration X"
//    val unit: String,      // "m/s^2"
)

data class SensorSchema(
//    val version: Int,     // mb later
    val fields: List<SensorField>
) {
    val indexById: Map<String, Int> = fields.mapIndexed { index, field ->
        field.id to index
    }.toMap()
}