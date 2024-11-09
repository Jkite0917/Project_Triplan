package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChecklistAdapter(
    private val items: MutableList<Checklist>,
    private val onDeleteClick: (Checklist) -> Unit,
    private val onCheckedChange: (Checklist) -> Unit // 체크박스 변경 리스너 추가
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_checklistItem_checkbox)
        val titleTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_title)
        val periodTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_period)
        val deleteButton: Button = itemView.findViewById(R.id.button_checklistItem_delete)

        fun bind(item: Checklist) {
            titleTextView.text = item.cTitle
            checkBox.isChecked = item.isChecked

            // period, weekDay, monthDay가 null이 아닌 경우에만 표시
            val period = item.period
            val weekDay = item.weekDay?.let { " $it" + "요일" } ?: ""
            val monthDay = item.monthDay?.let { " $it" + "일" } ?: ""

            // 모든 값이 null인 경우 빈 문자열이 되도록 조합
            periodTextView.text = "$period$weekDay$monthDay".trim() // trim()으로 공백 제거

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                onCheckedChange(item) // 체크박스 상태 변경 시 콜백 호출
            }

            deleteButton.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val currentItem = items[position]
        holder.bind(currentItem) // bind 메서드를 통해 데이터 바인딩
    }

    override fun getItemCount(): Int = items.size
}
