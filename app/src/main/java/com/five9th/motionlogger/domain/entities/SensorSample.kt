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
    var version: Int,
    val fields: List<SensorField>
) {
    val indexById: Map<String, Int> = fields.mapIndexed { index, field ->
        field.id to index
    }.toMap()

    override fun toString(): String =
        fields.joinToString(",") { it.id }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorSchema

        return fields == other.fields
    }

    override fun hashCode(): Int {
        return fields.hashCode()
    }

    val isVersionNotSet = version == VERSION_NOT_SET

    companion object {
        const val VERSION_NOT_SET = -1

        // example: string = "acc_x,acc_y,acc_z,yaw"
        fun fromString(value: String): SensorSchema =
            SensorSchema(
                version = VERSION_NOT_SET,
                fields = value.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map(::SensorField)
            )
    }
}