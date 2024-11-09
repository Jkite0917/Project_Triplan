package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChecklistItem(
    val cNo: Long = 0, // Primary Key
    val cTitle: String, // 제목
    var isChecked: Boolean = false, // 체크 상태
    val period: String, // 주기
    val weekDay: String? = null, // 요일 (선택 사항)
    val monthDay: String? = null // 날짜 (선택 사항)
)

class ChecklistAdapter(
    private val items: MutableList<ChecklistItem>,
    private val onDeleteClick: (Long) -> Unit,
    private val onCheckedChange: (ChecklistItem) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {
    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_checklistItem_checkbox)
        private val titleTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_title)
        private val periodTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_period)
        private val deleteButton: Button = itemView.findViewById(R.id.button_checklistItem_delete)

        fun bind(item: ChecklistItem) {
            titleTextView.text = item.cTitle
            checkBox.isChecked = item.isChecked
            periodTextView.text = buildPeriodText(item)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                onCheckedChange(item)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(item.cNo)
            }
        }

        private fun buildPeriodText(item: ChecklistItem): String {
            val period = item.period
            val weekDay = item.weekDay?.let { " $it" + "요일" } ?: ""
            val monthDay = item.monthDay?.let { " $it" + "일" } ?: ""
            return "$period$weekDay$monthDay".trim()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
