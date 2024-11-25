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
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_checklistItem_checkbox)
        val titleTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_title)
        val periodTextView: TextView = itemView.findViewById(R.id.textview_checklistItem_period)
        val deleteButton: Button = itemView.findViewById(R.id.button_checklistItem_delete)

        // 기간, 요일, 날짜 텍스트 구성
        fun buildPeriodText(item: ChecklistItem): String {
            val period = item.period
            val weekDay = item.weekDay?.let { " $it" + "요일"  } ?: "" // 숫자 -> 요일 변환
            val monthDay = item.monthDay?.let { " $it" + "일"  } ?: ""
            return "$period $weekDay $monthDay".trim()
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
        val item = items[position]

        // 기존 리스너 제거 (중복 방지)
        holder.checkBox.setOnCheckedChangeListener(null)

        // View 상태와 데이터 동기화
        holder.checkBox.isChecked = item.isChecked
        holder.titleTextView.text = item.cTitle
        holder.periodTextView.text = holder.buildPeriodText(item)

        // 새로운 리스너 설정
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            onCheckedChange(item) // 변경된 상태 콜백
        }

        // 삭제 버튼 클릭 리스너 설정
        holder.deleteButton.setOnClickListener {
            onDeleteClick(item.cNo)
        }
    }

    // 아이템 수 반환
    override fun getItemCount(): Int = items.size
}