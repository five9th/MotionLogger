package com.five9th.motionlogger.domain.entities

// value must correspond to the expected activity order in `ModelOutput` class:
// "dws", "ups", "wlk", "jog", "std", "sit"
enum class ActivityClass(val value: Int) {
    DOWN_STAIRS(0),
    UP_STAIRS(1),
    WALKING(2),
    JOGGING(3),
    STANDING(4),
    SITTING(5);

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }
}