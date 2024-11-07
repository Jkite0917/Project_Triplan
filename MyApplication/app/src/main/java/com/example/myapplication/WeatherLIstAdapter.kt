package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WeatherListItem(
    val wNo: Long,
    val contents: String,
    val weather: Int,
    val time: String,
)

class WeatherListAdapter(
    private val items: MutableList<WeatherListItem>,
    private val onDeleteClick: (Long) -> Unit
) : RecyclerView.Adapter<WeatherListAdapter.WeatherListViewHolder>() {

    inner class WeatherListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contents: TextView = itemView.findViewById(R.id.WeatherItemTextView)
        private val weather: ImageView = itemView.findViewById(R.id.WeatherItemIcon)
        private val time: TextView = itemView.findViewById(R.id.WeatherItemTimeTextView)
        private val deleteButton: Button = itemView.findViewById(R.id.WeatherItemDeleteButton)

        fun bind(item: WeatherListItem) {
            contents.text = item.contents
            weather.setImageResource(item.weather) // 저장된 drawable ID로 이미지 설정
            time.text = item.time

            deleteButton.setOnClickListener {
                onDeleteClick(item.wNo) // 삭제 시 데이터베이스의 기본 키 전달
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weatherlist, parent, false)
        return WeatherListViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherListViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

