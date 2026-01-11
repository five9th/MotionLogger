package com.five9th.motionlogger.domain.entities

/** Represents a window of 128 samples. Shape: (128, 9) */
data class SampleWindow(val samples: List<SensorSample>)

/** Output shape: (6,) - probability of a window belonging to each of 6 activity classes
 * ACT_LABELS = ["dws","ups", "wlk", "jog", "std", "sit"] */
data class ModelOutput(val scores: List<Float>)