package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChecklistItem(
    val title: String,
    val cycle: String,
    var isChecked: Boolean
)

class CheckListAdapter(
    private val items: MutableList<ChecklistItem>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<CheckListAdapter.CheckListViewHolder>() {

    inner class CheckListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val titleTextView: TextView = itemView.findViewById(R.id.checklist_item_title)
        val cycleTextView: TextView = itemView.findViewById(R.id.checklist_item_cycle)
        val deleteButton: Button = itemView.findViewById(R.id.checklist_item_delete)

        fun bind(item: ChecklistItem, position: Int) {
            titleTextView.text = item.title
            cycleTextView.text = item.cycle
            checkBox.isChecked = item.isChecked

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
            }

            deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return CheckListViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckListViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}
