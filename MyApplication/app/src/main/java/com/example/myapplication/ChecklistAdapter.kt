package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChecklistAdapter(private val items: MutableList<Checklist>,
                       private val onDeleteClick: (Checklist) -> Unit)
    : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checklist_item_checkbox)
        val titleTextView: TextView = itemView.findViewById(R.id.checklist_item_title)
        val periodTextView: TextView = itemView.findViewById(R.id.checklist_item_period)
        val deleteButton: Button = itemView.findViewById(R.id.checklist_item_delete)

        fun bind(item: Checklist) {
            titleTextView.text = item.cTitle
            periodTextView.text = item.period
            checkBox.isChecked = item.isChecked

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
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
