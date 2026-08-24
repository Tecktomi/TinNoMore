package com.tinnomore.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patient_specialist_assignments",
    indices = [
        Index(value = ["patientId", "specialistId"], unique = true),
        Index(value = ["specialistId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["specialistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PatientSpecialistAssignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val specialistId: Long
)
