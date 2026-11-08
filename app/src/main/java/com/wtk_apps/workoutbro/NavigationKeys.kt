package com.wtk_apps.workoutbro

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data object Settings : NavKey

@Serializable data object Trainings : NavKey

@Serializable data class EditTraining(
  val index: Int,
) : NavKey
