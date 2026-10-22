package com.example.crconnect.student

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.crconnect.R
import com.example.crconnect.core.utils.HapticUtils
import com.example.crconnect.databinding.ItemRoutineClassCardBinding

class RoutineAdapter(
    private val onAttendanceChanged: ((ClassEntry, String) -> Unit)? = null
) : ListAdapter<ClassEntry, RoutineAdapter.ClassViewHolder>(DiffCallback) {

    private val attendanceStates = mutableMapOf<String, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val binding = ItemRoutineClassCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ClassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ClassViewHolder(private val binding: ItemRoutineClassCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClassEntry) {
            val key = "${item.day}_${item.course}_${item.startTime}"
            var state = attendanceStates[key] ?: "NONE"

            // Left Time Slot Badge Box
            binding.tvTimeSlot.text = "${item.startTime}\n${item.endTime}"

            // Course Name & Faculty Code
            binding.tvCourseName.text = item.course
            binding.tvFacultyCode.text = if (item.faculty.startsWith("FC:")) item.faculty else "FC: ${item.faculty}"

            // Room Pill
            binding.tvRoomPill.text = if (item.room.startsWith("Room")) item.room else "Room ${item.room}"

            // Attendance Buttons UI Update
            updateAttendanceUi(state)

            binding.btnAttended.setOnClickListener { v ->
                HapticUtils.lightClick(v)
                state = if (state == "ATTENDED") "NONE" else "ATTENDED"
                attendanceStates[key] = state
                updateAttendanceUi(state)
                onAttendanceChanged?.invoke(item, state)
            }

            binding.btnMissed.setOnClickListener { v ->
                HapticUtils.lightClick(v)
                state = if (state == "MISSED") "NONE" else "MISSED"
                attendanceStates[key] = state
                updateAttendanceUi(state)
                onAttendanceChanged?.invoke(item, state)
            }
        }

        private fun updateAttendanceUi(state: String) {
            when (state) {
                "ATTENDED" -> {
                    binding.btnAttended.setBackgroundResource(R.drawable.bg_chip_ok)
                    binding.btnAttended.setTextColor(Color.parseColor("#166534"))

                    binding.btnMissed.setBackgroundResource(R.drawable.bg_chip_warn)
                    binding.btnMissed.setTextColor(Color.parseColor("#DC2626"))
                }
                "MISSED" -> {
                    binding.btnAttended.setBackgroundResource(R.drawable.bg_chip_green)
                    binding.btnAttended.setTextColor(Color.parseColor("#16A34A"))

                    binding.btnMissed.setBackgroundResource(R.drawable.bg_chip_warn)
                    binding.btnMissed.setTextColor(Color.parseColor("#991B1B"))
                }
                else -> {
                    binding.btnAttended.setBackgroundResource(R.drawable.bg_chip_green)
                    binding.btnAttended.setTextColor(Color.parseColor("#16A34A"))

                    binding.btnMissed.setBackgroundResource(R.drawable.bg_chip_warn)
                    binding.btnMissed.setTextColor(Color.parseColor("#DC2626"))
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ClassEntry>() {
        override fun areItemsTheSame(oldItem: ClassEntry, newItem: ClassEntry): Boolean {
            return oldItem.day == newItem.day &&
                    oldItem.course == newItem.course &&
                    oldItem.startTime == newItem.startTime
        }

        override fun areContentsTheSame(oldItem: ClassEntry, newItem: ClassEntry): Boolean {
            return oldItem == newItem
        }
    }
}
