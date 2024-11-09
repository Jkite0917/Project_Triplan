package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 데이터 클래스 ChecklistItem 정의
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
    private val onDeleteClick: (Long) -> Unit, // 삭제 버튼 클릭 리스너
    private val onCheckedChange: (ChecklistItem) -> Unit // 체크박스 변경 리스너
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    // ViewHolder 정의
    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_checklistItem_checkbox)
        private val titleTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_title)
        private val periodTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_period)
        private val deleteButton: Button = itemView.findViewById(R.id.button_checklistItem_delete)

        // ViewHolder에 데이터 바인딩
        fun bind(item: ChecklistItem) {
            // 제목 및 체크 상태 초기화
            titleTextView.text = item.cTitle
            checkBox.isChecked = item.isChecked
            periodTextView.text = buildPeriodText(item)

            // 체크박스 클릭 리스너 설정 - 변경 시 콜백 호출
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                onCheckedChange(item)
            }

            // 삭제 버튼 클릭 리스너 설정 - 삭제 시 콜백 호출
            deleteButton.setOnClickListener {
                onDeleteClick(item.cNo)
            }
        }

        // 기간, 요일, 날짜 텍스트 구성
        private fun buildPeriodText(item: ChecklistItem): String {
            val period = item.period
            val weekDay = item.weekDay?.let { " $it" + "요일" } ?: ""
            val monthDay = item.monthDay?.let { " $it" + "일" } ?: ""
            return "$period$weekDay$monthDay".trim()
        }
    }

    // ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    // ViewHolder에 데이터 바인딩
    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // 아이템 수 반환
    override fun getItemCount(): Int = items.size
}
